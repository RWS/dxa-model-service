package com.sdl.dxa.modelservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdl.dxa.api.datamodel.model.ContentModelData;
import com.sdl.dxa.api.datamodel.model.EntityModelData;
import com.sdl.dxa.api.datamodel.model.KeywordModelData;
import com.sdl.dxa.api.datamodel.model.PageModelData;
import com.sdl.dxa.api.datamodel.model.RegionModelData;
import com.sdl.dxa.api.datamodel.model.ViewModelData;
import com.sdl.dxa.api.datamodel.model.util.CanWrapContentAndMetadata;
import com.sdl.dxa.api.datamodel.model.util.ListWrapper;
import com.sdl.dxa.api.datamodel.model.util.ModelDataWrapper;
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
import com.tridion.meta.NameValuePair;
import com.tridion.taxonomies.Keyword;
import com.tridion.taxonomies.TaxonomyFactory;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

    @Contract("!null, _ -> !null")
    private PageModelData _processPageModel(PageModelData pageModel, PageRequestDto pageRequest) throws ContentProviderException {
        PageModelData pageModelData = _expandIncludePages(pageModel, pageRequest);

        // let's check every leaf here if we need to expand it
        _expandObject(pageModelData, pageRequest);

        return pageModelData;
    }

    private void _expandObject(Object value, PageRequestDto pageRequest) throws ContentProviderException {
        log.trace("Expanding '{}' under request '{}'", value, pageRequest);
        if (value instanceof PageModelData) { // got something, maybe it's a Page?
            _expandPageModel((PageModelData) value, pageRequest);
        } else if (value instanceof RegionModelData) { // this is not a page, so maybe region?
            _expandRegionModel((RegionModelData) value, pageRequest);
        } else if (_isCollectionType(value)) { // this is not a page nor a region, maybe it's one one collections?
            _expandCollection(value, pageRequest);
        } else {
            // it's one of concrete models

            if (value instanceof CanWrapContentAndMetadata) { // if it may have own content or metadata, let's process it also, maybe we can find models there
                ModelDataWrapper wrapper = ((CanWrapContentAndMetadata) value).getDataWrapper();
                if (wrapper.getContent() != null) {
                    _expandObject(wrapper.getContent(), pageRequest);
                }
                if (wrapper.getMetadata() != null) {
                    _expandObject(wrapper.getMetadata(), pageRequest);
                }
            }

            if (_isModelToExpand(value)) { // ok, we have one of concrete models, do we want to expand it?
                // we want to expand it, let's finally decide what is it and expand
                if (value instanceof KeywordModelData) {
                    _expandKeyword((KeywordModelData) value, pageRequest);
                } else if (value instanceof EntityModelData) {
                    _expandEntity((EntityModelData) value, pageRequest);
                }
            }
        }
    }

    private void _expandPageModel(PageModelData page, PageRequestDto pageRequest) throws ContentProviderException {
        // let's expand all regions, one by one
        for (RegionModelData region : page.getRegions()) {
            _expandObject(region, pageRequest);
        }
        // pages may have metadata, process it
        _expandObject(page.getMetadata(), pageRequest);
    }

    private void _expandRegionModel(RegionModelData region, PageRequestDto pageRequest) throws ContentProviderException {
        if (region.getRegions() != null) { // then it may have nested regions
            for (RegionModelData nestedRegion : region.getRegions()) {
                _expandObject(nestedRegion, pageRequest);
            }
        }

        if (region.getEntities() != null) { // or maybe it has entities?
            for (EntityModelData entity : region.getEntities()) {
                _expandObject(entity, pageRequest);
            }
        }

        // regions may have metadata, process it
        _expandObject(region.getMetadata(), pageRequest);
    }

    private void _expandCollection(Object value, PageRequestDto pageRequest) throws ContentProviderException {
        Collection<?> values;

        if (value instanceof Map) { // ok, found a Map (CMD?)
            values = ((Map) value).values();
        } else if (value instanceof ListWrapper) { // if it's not a map, then it's probable a ListWrapper
            values = ((ListWrapper) value).getValues();
        } else { // should have been handled previously, but maybe we lost a type and it's just a collection?
            values = (Collection) value;
        }

        for (Object element : values) { // let's expand our collection element by element
            _expandObject(element, pageRequest);
        }
    }

    private boolean _isCollectionType(Object value) {
        return value instanceof ListWrapper || value instanceof Collection || value instanceof Map;
    }

    private boolean _isModelToExpand(Object value) {
        return (value instanceof KeywordModelData && ((KeywordModelData) value).getTitle() == null)
                || (value instanceof EntityModelData && ((EntityModelData) value).getSchemaId() == null);
    }

    private void _expandEntity(EntityModelData toExpand, PageRequestDto pageRequest) throws ContentProviderException {
        EntityRequestDto entityRequest = EntityRequestDto.builder()
                .entityId(toExpand.getId())
                .publicationId(pageRequest.getPublicationId())
                .build();

        log.trace("Found entity to expand {}, request {}", toExpand.getId(), entityRequest);
        toExpand.copyFrom(loadEntity(entityRequest));
    }

    private void _expandKeyword(KeywordModelData keywordModel, PageRequestDto pageRequest) throws ContentProviderException {
        String keywordURI = TcmUtils.buildKeywordTcmUri(String.valueOf(pageRequest.getPublicationId()), keywordModel.getId());
        log.trace("Found keyword to expand, uri = '{}'", keywordURI);
        Keyword keyword = new TaxonomyFactory().getTaxonomyKeyword(keywordURI);

        if (keyword == null) {
            throw new ContentProviderException("Keyword " + keywordModel.getId() + " in publication " + pageRequest.getPublicationId() + " cannot be found, is it published?");
        }

        keywordModel.setDescription(keyword.getKeywordDescription())
                .setKey(keyword.getKeywordKey())
                .setTitle(keyword.getKeywordName())
                .setTaxonomyId(String.valueOf(TcmUtils.getItemId(keyword.getTaxonomyURI())))
                .setMetadata(_getMetadata(keyword, pageRequest));
    }

    @NotNull
    private ContentModelData _getMetadata(Keyword keyword, PageRequestDto pageRequest) throws ContentProviderException {
        ContentModelData metadata = new ContentModelData();
        for (Map.Entry<String, NameValuePair> entry : keyword.getKeywordMeta().getNameValues().entrySet()) {
            String key = entry.getKey();
            NameValuePair value = entry.getValue();

            ListWrapper<?> values = null;

            String firstValue = String.valueOf(value.getFirstValue());
            if (TcmUtils.isTcmUri(firstValue) && TcmUtils.getItemType(firstValue) == TcmUtils.KEYWORD_ITEM_TYPE) {

                List<KeywordModelData> data = new ArrayList<>();

                for (Object uri : value.getMultipleValues()) {
                    KeywordModelData keywordModelData = new KeywordModelData().setId(String.valueOf(TcmUtils.getItemId(String.valueOf(uri))));
                    _expandObject(keywordModelData, pageRequest);
                    data.add(keywordModelData);
                }

                values = new ListWrapper.KeywordModelDataListWrapper(data);
            }

            values = values == null ? new ListWrapper<>(value.getMultipleValues()) : values;

            metadata.put(key, values);
        }
        return metadata;
    }

    @Contract("!null, _ -> !null")
    private PageModelData _expandIncludePages(PageModelData pageModel, PageRequestDto pageRequest) throws ContentProviderException {
        Iterator<RegionModelData> iterator = pageModel.getRegions().iterator();
        while (iterator.hasNext()) {
            RegionModelData region = iterator.next();
            if (region.getIncludePageId() == null) {
                continue;
            }

            log.trace("Found include region include id = {}, we {} this page", region.getIncludePageId(), pageRequest.getIncludePages());

            switch (pageRequest.getIncludePages()) {
                case EXCLUDE:
                    iterator.remove();
                    break;
                case INCLUDE:
                default:
                    String includePageContent = _getPageContent(pageRequest.getPublicationId(), Integer.parseInt(region.getIncludePageId()));
                    // maybe it has inner regions which we need to include?
                    PageModelData includePage = _expandIncludePages(_parseResponse(includePageContent, PageModelData.class), pageRequest);

                    if (includePage.getRegions() != null) {
                        includePage.getRegions().forEach(region::addRegion);
                    }
            }
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
