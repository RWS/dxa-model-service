package com.sdl.dxa.modelservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdl.dxa.api.datamodel.model.EntityModelData;
import com.sdl.dxa.api.datamodel.model.PageModelData;
import com.sdl.dxa.api.datamodel.model.ViewModelData;
import com.sdl.dxa.common.util.PathUtils;
import com.sdl.webapp.common.api.content.ContentProviderException;
import com.sdl.webapp.common.api.content.PageNotFoundException;
import com.sdl.webapp.common.exceptions.DxaItemNotFoundException;
import com.sdl.webapp.common.util.TcmUtils;
import com.tridion.broker.StorageException;
import com.tridion.broker.querying.Query;
import com.tridion.broker.querying.criteria.content.PageURLCriteria;
import com.tridion.broker.querying.criteria.content.PublicationCriteria;
import com.tridion.broker.querying.criteria.operators.AndCriteria;
import com.tridion.broker.querying.criteria.operators.OrCriteria;
import com.tridion.broker.querying.filter.LimitFilter;
import com.tridion.broker.querying.sorting.SortParameter;
import com.tridion.content.PageContentFactory;
import com.tridion.data.CharacterData;
import com.tridion.dcp.ComponentPresentation;
import com.tridion.dcp.ComponentPresentationFactory;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;

import static com.sdl.dxa.common.util.PathUtils.normalizePathToDefaults;


@Slf4j
@Service
public class ContentService {

    private final ObjectMapper objectMapper;

    @Autowired
    public ContentService(@Qualifier("dxaR2ObjectMapper") ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Loads a page from CD and converts it to the {@link PageModelData}.
     * See {@link #loadPageContent(int, String)} for details.
     */
    @NotNull
    @Cacheable(value = "pmd", key = "#root.methodName + #publicationId + #path")
    public PageModelData loadPageModel(int publicationId, @NotNull String path) throws ContentProviderException {
        return _parseResponse(loadPageContent(publicationId, path), PageModelData.class);
    }

    private <T extends ViewModelData> T _parseResponse(String content, Class<T> expectedClass) throws ContentProviderException {
        try {
            return objectMapper.readValue(content, expectedClass);
        } catch (IOException e) {
            throw new ContentProviderException("Couldn't deserialize content '" + content + "' for " + expectedClass, e);
        }
    }

    /**
     * Loads a page from CD.
     *
     * @param publicationId current publication id
     * @param path          page URL to look up
     * @return a page model data, never null
     * @throws PageNotFoundException    if the page doesn't exist
     * @throws ContentProviderException if couldn't load or parse the page content
     */
    @Cacheable(value = "default", key = "#root.methodName + #publicationId + #path")
    public String loadPageContent(int publicationId, @NotNull String path) throws ContentProviderException {
        log.debug("Trying to request a page with localization id = '{}' and path = '{}'", publicationId, path);

        // cannot call OrCriteria#addCriteria(Criteria) due to SOException, https://jira.sdl.com/browse/CRQ-3850
        OrCriteria urlCriteria = PathUtils.hasExtension(path) ?
                new OrCriteria(new PageURLCriteria(normalizePathToDefaults(path))) :
                new OrCriteria(new PageURLCriteria(normalizePathToDefaults(path)), new PageURLCriteria(normalizePathToDefaults(path + "/")));

        Query query = new Query(new AndCriteria(urlCriteria, new PublicationCriteria(publicationId)));
        query.setResultFilter(new LimitFilter(1));
        query.addSorting(new SortParameter(SortParameter.ITEMS_URL, SortParameter.ASCENDING));

        try {
            String[] result = query.executeQuery();

            log.debug("Requested publication '{}', path '{}', result is '{}'", publicationId, path, result);

            if (result.length == 0) {
                throw new PageNotFoundException(publicationId, path);
            }

            CharacterData pageContent = new PageContentFactory().getPageContent(publicationId, TcmUtils.getItemId(result[0]));
            return pageContent.getString();
        } catch (StorageException | IOException e) {
            throw new ContentProviderException("Couldn't load a page with localization ID '" + publicationId + "' and page URL '" + path + "'", e);
        }
    }

    /**
     * Loads an Entity model.
     *
     * @param publicationId current publication ID
     * @param componentId   component to load
     * @param templateId    template to load
     * @return an entity model data, never null
     * @throws DxaItemNotFoundException if the component wasn't found
     * @throws ContentProviderException if couldn't load or parse the page content
     */
    @NotNull
    @Cacheable(value = "emd", key = "#root.methodName + #publicationId + #componentId + #templateId")
    public EntityModelData loadEntity(int publicationId, int componentId, int templateId) throws ContentProviderException {
        String componentUri = TcmUtils.buildTcmUri(publicationId, componentId);
        String templateUri = TcmUtils.buildTemplateTcmUri(publicationId, templateId);

        ComponentPresentationFactory componentPresentationFactory = new ComponentPresentationFactory(componentUri);
        ComponentPresentation componentPresentation = componentPresentationFactory.getComponentPresentation(componentUri, templateUri);

        if (componentPresentation == null) {
            throw new DxaItemNotFoundException("Cannot find a CP for componentUri" + componentUri + ", templateUri" + templateUri);
        }
        return _parseResponse(componentPresentation.getContent(), EntityModelData.class);
    }

}
