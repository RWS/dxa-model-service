package com.sdl.dxa.modelservice.service;

import com.sdl.dxa.common.dto.DataModelType;
import com.sdl.dxa.common.dto.EntityRequestDto;
import com.sdl.dxa.common.dto.PageRequestDto;
import com.sdl.dxa.common.util.PathUtils;
import com.sdl.dxa.tridion.compatibility.TridionQueryLoader;
import com.sdl.webapp.common.api.content.ContentProviderException;
import com.sdl.webapp.common.api.content.PageNotFoundException;
import com.sdl.webapp.common.exceptions.DxaItemNotFoundException;
import com.sdl.webapp.common.util.TcmUtils;
import com.tridion.broker.StorageException;
import com.tridion.broker.querying.criteria.content.PageURLCriteria;
import com.tridion.broker.querying.criteria.content.PublicationCriteria;
import com.tridion.broker.querying.criteria.operators.AndCriteria;
import com.tridion.broker.querying.criteria.operators.OrCriteria;
import com.tridion.content.PageContentFactory;
import com.tridion.data.CharacterData;
import com.tridion.dcp.ComponentPresentation;
import com.tridion.dcp.ComponentPresentationFactory;
import com.tridion.dynamiccontent.ComponentPresentationAssembler;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;

import static com.sdl.dxa.common.util.PathUtils.normalizePathToDefaults;

/**
 * The service to load raw content stored in Broker database completely without or with light processing.
 * See details in Javadoc of a concrete method.
 *
 * Will work both in-process and over OData.
 */
@Slf4j
@Service
public class ContentService {

    private final ConfigService configService;
    private final TridionQueryLoader queryLoader;
    private final PageContentFactory pageContentFactory;

    @Autowired
    public ContentService(ConfigService configService,
                          TridionQueryLoader queryLoader, PageContentFactory pageContentFactory) {
        this.configService = configService;
        this.queryLoader = queryLoader;
        this.pageContentFactory = pageContentFactory;
    }

    /**
     * Detects model type from json content string.
     *
     * @param jsonContent json content of a page
     * @return type of the model
     */
    public static DataModelType getModelType(String jsonContent) {
        boolean isR2Page = jsonContent.contains("UrlPath") && !jsonContent.contains("ComponentPresentations");
        boolean isR2Entity =
                jsonContent.contains("Content") && jsonContent.contains("SchemaId") && !jsonContent.contains(
                        "\"ComponentType\"");

        return isR2Page || isR2Entity ? DataModelType.R2 : DataModelType.DD4T;
    }

    @NotNull
    @Cacheable(value = "pageModels", key = "{ #root.methodName, #pageRequest }", sync = true)
    public String loadPageContent(PageRequestDto pageRequest) throws ContentProviderException {
        log.info("Page: {}, request: {} - CACHE NOT USED", pageRequest.getPublicationId(), pageRequest.getPath());
        int publicationId = pageRequest.getPublicationId();
        if (pageRequest.getPageId() != 0) {
            log.info("Page ID is known, no need to search it, requesting pubId = {}, pageId = {}", publicationId, pageRequest.getPageId());
            return loadPageContent(publicationId, pageRequest.getPageId());
        }

        String path = pageRequest.getPath();

        log.debug("Trying to request a page with localization id = '{}' and path = '{}'", publicationId, path);
        // cannot call OrCriteria#addCriteria(Criteria) due to SOException, https://jira.sdl.com/browse/CRQ-3850
        OrCriteria urlCriteria = PathUtils.hasExtension(path) ?
                new OrCriteria(new PageURLCriteria(normalizePathToDefaults(path))) :
                new OrCriteria(new PageURLCriteria(normalizePathToDefaults(path)), new PageURLCriteria(normalizePathToDefaults(path + "/")));

        // Use a wrapper to make it work on CIL and In Process
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
        } catch (StorageException e) {
            ContentProviderException exception = new ContentProviderException("Couldn't communicate to CD broker DB while loading a page " +
                    "with localization ID '" + publicationId + "' and page URL '" + path + "'", e);
            log.warn("Issues communicating with CD", exception);
            throw exception;
        }
    }

    /**
     * Loads component presentation content for an entity and renders all tcdl links.
     *
     * @param publicationId Publication ID
     * @param componentId Component ID
     * @param templateId Template ID
     * @return rendered component presentation content of an entity based on a request
     */
    @NotNull
    @Cacheable(value = "entityModels", key = "{ #root.methodName, #publicationId, #componentId, #templateId}", sync = true)
    public String loadRenderedComponentPresentation(int publicationId, int componentId, int templateId) throws DxaItemNotFoundException {
        ComponentPresentationAssembler assembler = getAssembler(publicationId);

        if (templateId <= 0) {
            templateId = configService.getDefaults().getDynamicTemplateId(publicationId);
        }

        String content = assembler.getContent(componentId, templateId);
        if(content == null) {
            throw new DxaItemNotFoundException("Cannot find a CP for componentId = " + componentId + ", template id = " + templateId);
        }
        return content;
    }

    /**
     * Loads component presentation for an entity without any processing.
     *
     * @param entityRequest current entity request
     * @return raw component presentation of a entity based on a request
     * @throws DxaItemNotFoundException in case there nothing was found for this request
     */
    @NotNull
    @Cacheable(value = "entityModels", key = "{ #root.methodName, #entityRequest}", sync = true)
    public ComponentPresentation loadComponentPresentation(EntityRequestDto entityRequest) throws DxaItemNotFoundException {
        int publicationId = entityRequest.getPublicationId();
        int componentId = entityRequest.getComponentId();
        int templateId = entityRequest.getTemplateId();

        ComponentPresentationFactory componentPresentationFactory = getComponentPresentationFactory(publicationId);

        ComponentPresentation componentPresentation;

        if (entityRequest.getDcpType() == EntityRequestDto.DcpType.HIGHEST_PRIORITY && templateId <= 0) {
            log.debug("Load Component Presentation with component id = {} with highest priority", componentId);
            componentPresentation = componentPresentationFactory.getComponentPresentationWithHighestPriority(componentId);
        } else {
            if (templateId <= 0) {
                templateId = configService.getDefaults().getDynamicTemplateId(publicationId);
            }

            log.debug("Load Component Presentation with component ID = {} and template ID = {}", componentId, templateId);
            componentPresentation = componentPresentationFactory.getComponentPresentation(publicationId, componentId, templateId);
        }

        if (componentPresentation == null) {
            throw new DxaItemNotFoundException("Cannot find a CP for componentId = " + componentId + ", template id = " + templateId);
        }
        return componentPresentation;
    }


    String loadPageContent(int publicationId, int pageId) throws ContentProviderException {
        try {
            log.trace("requesting page content for publication {} page id and {}", publicationId, pageId);
            CharacterData data = pageContentFactory.getPageContent(publicationId, pageId);
            if (data == null) {
                throw new ContentProviderException("Content Service returned null for request pubId = " + publicationId + "pageId = " + pageId);
            }
            return data.getString();
        } catch (IOException e) {
            ContentProviderException exception = new ContentProviderException("Couldn't load a page with localization ID '" + publicationId + "' and page ID '" + pageId + "'", e);
            log.warn("Failed to load page content", exception);
            throw exception;
        }
    }

    ComponentPresentationAssembler getAssembler(int publicationId) {
        return new ComponentPresentationAssembler(publicationId);
    }

    ComponentPresentationFactory getComponentPresentationFactory(int publicationId) {
        return new ComponentPresentationFactory(publicationId);
    }
}
