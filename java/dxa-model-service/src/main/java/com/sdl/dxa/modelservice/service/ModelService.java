package com.sdl.dxa.modelservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdl.dxa.api.datamodel.model.EntityModelData;
import com.sdl.dxa.api.datamodel.model.PageModelData;
import com.sdl.dxa.api.datamodel.model.RegionModelData;
import com.sdl.dxa.api.datamodel.model.ViewModelData;
import com.sdl.dxa.common.dto.EntityRequestDto;
import com.sdl.dxa.common.dto.PageRequestDto;
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
import com.tridion.dcp.ComponentPresentation;
import com.tridion.dcp.ComponentPresentationFactory;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

import static com.sdl.dxa.common.util.PathUtils.normalizePathToDefaults;


@Slf4j
@Service
@Cacheable(value = "model-service")
public class ModelService implements PageModelService, EntityModelService {

    private final ObjectMapper objectMapper;

    @Autowired
    public ModelService(@Qualifier("dxaR2ObjectMapper") ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    @NotNull
    public PageModelData loadPageModel(PageRequestDto pageRequest) throws ContentProviderException {
        PageModelData pageModel = _parseResponse(loadPageContent(pageRequest), PageModelData.class);
        return _processPageModel(pageModel, pageRequest);
    }

    @Override
    @NotNull
    public String loadPageContent(PageRequestDto pageRequest) throws ContentProviderException {
        int publicationId = pageRequest.getPublicationId();
        String path = pageRequest.getPath();

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

            return _getPageContent(publicationId, TcmUtils.getItemId(result[0]));
        } catch (StorageException e) {
            throw new ContentProviderException("Couldn't communicate to CD broker DB while loading a page " +
                    "with localization ID '" + publicationId + "' and page URL '" + path + "'", e);
        }
    }

    private PageModelData _processPageModel(PageModelData pageModel, PageRequestDto pageRequest) throws ContentProviderException {
        return _expandIncludePages(pageModel, pageRequest);
    }

    private PageModelData _expandIncludePages(PageModelData pageModel, PageRequestDto pageRequest) throws ContentProviderException {
        if (pageRequest.getIncludePages() == PageRequestDto.PageInclusion.EXCLUDE) {
            log.debug("Page {} requested excluding included regions {}", pageModel, pageRequest);
            return pageModel;
        }

        List<RegionModelData> regions = pageModel.getRegions();
        for (RegionModelData region : regions) {
            if (region.getIncludePageId() == null) {
                continue;
            }
            log.trace("Found include region include id = {}", region.getIncludePageId());

            String includePageContent = _getPageContent(pageRequest.getPublicationId(), Integer.parseInt(region.getIncludePageId()));
            PageModelData includePage = _processPageModel(_parseResponse(includePageContent, PageModelData.class), pageRequest);

            includePage.getRegions().forEach(region::addRegion);
        }
        return pageModel;
    }

    private String _getPageContent(int publicationId, int pageId) throws ContentProviderException {
        try {
            return new PageContentFactory().getPageContent(publicationId, pageId).getString();
        } catch (IOException e) {
            throw new ContentProviderException("Couldn't load a page with localization ID '" + publicationId + "' and page ID '" + pageId + "'", e);
        }
    }

    @Override
    @NotNull
    public EntityModelData loadEntity(EntityRequestDto entityRequest) throws ContentProviderException {
        int publicationId = entityRequest.getPublicationId();
        int componentId = entityRequest.getComponentId();
        int templateId = entityRequest.getTemplateId();

        String componentUri = TcmUtils.buildTcmUri(publicationId, componentId);
        String templateUri = TcmUtils.buildTemplateTcmUri(publicationId, templateId);

        ComponentPresentationFactory componentPresentationFactory = new ComponentPresentationFactory(componentUri);
        ComponentPresentation componentPresentation = componentPresentationFactory.getComponentPresentation(componentUri, templateUri);

        if (componentPresentation == null) {
            throw new DxaItemNotFoundException("Cannot find a CP for componentUri" + componentUri + ", templateUri" + templateUri);
        }
        return _parseResponse(componentPresentation.getContent(), EntityModelData.class);
    }

    private <T extends ViewModelData> T _parseResponse(String content, Class<T> expectedClass) throws ContentProviderException {
        try {
            return objectMapper.readValue(content, expectedClass);
        } catch (IOException e) {
            throw new ContentProviderException("Couldn't deserialize content '" + content + "' for " + expectedClass, e);
        }
    }

}
