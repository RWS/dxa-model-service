package com.sdl.dxa.modelservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdl.dxa.common.dto.DataModelType;
import com.sdl.dxa.common.dto.EntityRequestDto;
import com.sdl.dxa.common.dto.PageRequestDto;
import com.sdl.dxa.common.util.PathUtils;
import com.sdl.dxa.tridion.compatibility.TridionQueryLoader;
import com.sdl.webapp.common.api.content.ContentProviderException;
import com.sdl.webapp.common.api.content.PageNotFoundException;
import com.sdl.webapp.common.exceptions.DxaItemNotFoundException;
import com.sdl.webapp.common.util.TcmUtils;
import com.tridion.broker.querying.criteria.content.PageURLCriteria;
import com.tridion.broker.querying.criteria.content.PublicationCriteria;
import com.tridion.broker.querying.criteria.operators.AndCriteria;
import com.tridion.broker.querying.criteria.operators.OrCriteria;
import com.tridion.content.PageContentFactory;
import com.tridion.data.CharacterData;
import com.tridion.dcp.ComponentPresentation;
import com.tridion.dcp.ComponentPresentationFactory;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import static com.sdl.dxa.common.util.PathUtils.normalizePathToDefaults;

/**
 * The service to load raw content stored in Broker database completely without or with light processing.
 * See details in Javadoc of a concrete method.
 * <p>
 * Will work both in-process and over OData.
 */
@Slf4j
@Service
public class ContentService {

    private final ConfigService configService;

    private final ObjectMapper objectMapper;

    private ApplicationContext applicationContext;

    @Autowired
    public ContentService(ConfigService configService,
                          ObjectMapper objectMapper,
                          ApplicationContext appContext) {
        this.configService = configService;
        this.objectMapper = objectMapper;
        this.applicationContext = appContext;
    }

    /**
     * Detects model type from json content string.
     *
     * @param jsonContent json content of a page
     * @return type of the model
     */
    public static DataModelType getModelType(String jsonContent) {
        boolean isR2Page = jsonContent.contains("UrlPath") && !jsonContent.contains("ComponentPresentations");
        boolean isR2Entity = jsonContent.contains("Content") && jsonContent.contains("SchemaId") && !jsonContent.contains("ComponentType");

        return isR2Page || isR2Entity ? DataModelType.R2 : DataModelType.DD4T;
    }

    @NotNull
    @Cacheable(value = "pageContents", key = "{ #root.methodName, #pageRequest }")
    public String loadPageContent(PageRequestDto pageRequest) throws ContentProviderException {
        return loadPageContentNotCached(pageRequest);
    }

    @NotNull
    public String loadPageContentNotCached(PageRequestDto pageRequest) throws ContentProviderException {
        log.info("Page: {}, request: {}", pageRequest.getPublicationId(), pageRequest.getPath());
        int publicationId = pageRequest.getPublicationId();
        String path = pageRequest.getPath();

        log.debug("Trying to request a page with localization id = '{}' and path = '{}'", publicationId, path);

        // cannot call OrCriteria#addCriteria(Criteria) due to SOException, https://jira.sdl.com/browse/CRQ-3850
        OrCriteria urlCriteria = PathUtils.hasExtension(path) ?
                new OrCriteria(new PageURLCriteria(normalizePathToDefaults(path))) :
                new OrCriteria(new PageURLCriteria(normalizePathToDefaults(path)), new PageURLCriteria(normalizePathToDefaults(path + "/")));

        // Use a wrapper to make it work on CIL and In Process

        final TridionQueryLoader queryLoader = applicationContext.getBean(TridionQueryLoader.class);

        try {
            String[] result =
                    queryLoader.constructQueryAndSetResultFilter(
                            new AndCriteria(urlCriteria,
                                    new PublicationCriteria(publicationId)), pageRequest);

            log.debug("Requested publication '{}', path '{}', result is '{}'", publicationId, path, result);
            if (result.length == 0) {
                log.debug("Page not found for {}", pageRequest);
                throw new PageNotFoundException(publicationId, path);
            }

            return loadPageContent(publicationId, TcmUtils.getItemId(result[0]));
        } catch (DxaItemNotFoundException ex) {
            throw ex;
        } catch (Exception e) {
            ContentProviderException exception = new ContentProviderException("Couldn't communicate to CD broker DB while loading a page " +
                    "with localization ID '" + publicationId + "' and page URL '" + path + "'", e);
            log.warn("Issues communicating with CD", exception);
            throw exception;
        }
    }

    /**
     * Loads component presentation for an entity without any processing.
     *
     * @param entityRequest current entity request
     * @return raw component presentation of a entity based on a request
     * @throws DxaItemNotFoundException in case there nothing was found for this request
     */
    @NotNull
    @Cacheable(value = "entityModels", key = "{ #root.methodName, #entityRequest}")
    public ComponentPresentation loadComponentPresentation(EntityRequestDto entityRequest) throws DxaItemNotFoundException {
        int publicationId = entityRequest.getPublicationId();

        String componentUri = TcmUtils.buildTcmUri(publicationId, entityRequest.getComponentId());
        ComponentPresentationFactory componentPresentationFactory = new ComponentPresentationFactory(componentUri);

        ComponentPresentation componentPresentation;

        if (entityRequest.getDcpType() == EntityRequestDto.DcpType.HIGHEST_PRIORITY && entityRequest.getTemplateId() <= 0) {
            log.debug("Load Component Presentation with component id = {} with highest priority", componentUri);
            componentPresentation = componentPresentationFactory.getComponentPresentationWithHighestPriority(componentUri);
        } else {
            String templateUri;
            if (entityRequest.getTemplateId() > 0) {
                templateUri = TcmUtils.buildTemplateTcmUri(publicationId, entityRequest.getTemplateId());
            } else {
                templateUri = TcmUtils.buildTemplateTcmUri(publicationId, configService.getDefaults().getDynamicTemplateId(publicationId));
            }

            log.debug("Load Component Presentation with component uri = {} and template uri = {}", componentUri, templateUri);
            componentPresentation = componentPresentationFactory.getComponentPresentation(componentUri, templateUri);
        }

        if (componentPresentation == null) {
            throw new DxaItemNotFoundException("Cannot find a CP for componentUri = " + componentUri + ", template id = " + entityRequest.getTemplateId());
        }
        return componentPresentation;
    }

    String loadPageContent(int publicationId, int pageId) throws ContentProviderException {
        try {
            log.trace("requesting page content for publication {} page id and {}", publicationId, pageId);
            CharacterData data = new PageContentFactory().getPageContent(publicationId, pageId);
            if (data == null) {
                throw new PageNotFoundException("Content Service returned null for request pubId = " + publicationId + "pageId = " + pageId);
            }

            return data.getString();
        } catch (DxaItemNotFoundException ex) {
            throw ex;
        } catch (Exception e) {
            ContentProviderException exception = new ContentProviderException("Couldn't load a page with localization ID '" + publicationId + "' and page ID '" + pageId + "'", e);
            log.warn("Failed to load page content", exception);
            throw exception;
        }
    }
}
