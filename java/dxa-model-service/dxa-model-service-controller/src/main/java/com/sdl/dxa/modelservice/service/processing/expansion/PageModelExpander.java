package com.sdl.dxa.modelservice.service.processing.expansion;

import com.sdl.dxa.api.datamodel.model.ContentModelData;
import com.sdl.dxa.api.datamodel.model.EntityModelData;
import com.sdl.dxa.api.datamodel.model.KeywordModelData;
import com.sdl.dxa.api.datamodel.model.PageModelData;
import com.sdl.dxa.api.datamodel.model.RichTextData;
import com.sdl.dxa.api.datamodel.model.util.ListWrapper;
import com.sdl.dxa.api.datamodel.processing.DataModelDeepFirstSearcher;
import com.sdl.dxa.common.dto.EntityRequestDto;
import com.sdl.dxa.common.dto.PageRequestDto;
import com.sdl.dxa.modelservice.service.ConfigService;
import com.sdl.dxa.modelservice.service.EntityModelService;
import com.sdl.dxa.tridion.linking.RichTextLinkResolver;
import com.sdl.webapp.common.api.content.ContentProviderException;
import com.sdl.webapp.common.api.content.LinkResolver;
import com.sdl.webapp.common.util.TcmUtils;
import com.tridion.meta.NameValuePair;
import com.tridion.taxonomies.Keyword;
import com.tridion.taxonomies.TaxonomyFactory;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Expands {@link PageModelData} using an instance of {@link PageRequestDto}.
 */
@Slf4j
public class PageModelExpander extends DataModelDeepFirstSearcher {

    private PageRequestDto pageRequest;

    private EntityModelService entityModelService;

    private RichTextLinkResolver richTextLinkResolver;

    private LinkResolver linkResolver;

    private ConfigService configService;

    private int pageId;

    public PageModelExpander(PageRequestDto pageRequest,
                             EntityModelService entityModelService,
                             RichTextLinkResolver richTextLinkResolver,
                             LinkResolver linkResolver,
                             ConfigService configService,
                             int pageId) {
        this.pageRequest = pageRequest;
        this.entityModelService = entityModelService;
        this.richTextLinkResolver = richTextLinkResolver;
        this.linkResolver = linkResolver;
        this.configService = configService;
        this.pageId = pageId;
    }

    /**
     * Expands a data model.
     *
     * @param page model to expand
     */
    public void expandPage(@Nullable PageModelData page) {
        traverseObject(page);
    }

    @Override
    protected boolean goingDeepIsAllowed() {
        return pageRequest.getDepthCounter().depthIncreaseAndCheckIfSafe();
    }

    @Override
    protected void goLevelUp() {
        pageRequest.getDepthCounter().depthDecrease();
    }

    @Override
    protected void processPageModel(PageModelData pageModelData) {
        // pages may have meta (sic!: not metadata which is part of content wrapper), process it
        pageModelData.setMeta(Optional.ofNullable(pageModelData.getMeta())
                .orElse(Collections.emptyMap())
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey, meta ->
                                TcmUtils.isTcmUri(meta.getValue()) ?
                                        linkResolver.resolveLink(meta.getValue(), String.valueOf(pageRequest.getPublicationId()), true, String.valueOf(pageId)) :
                                        richTextLinkResolver.processFragment(meta.getValue(), pageRequest.getPublicationId(),pageId))));
    }

    @Override
    protected void processEntityModel(EntityModelData entityModelData) {
        if (_isEntityToExpand(entityModelData)) {
            _expandEntity(entityModelData, pageRequest);
        }
        String componentUri = TcmUtils.buildTcmUri(String.valueOf(pageRequest.getPublicationId()), entityModelData.getId());
        entityModelData.setLinkUrl(linkResolver.resolveLink(componentUri, String.valueOf(pageRequest.getPublicationId()),String.valueOf(pageId)));
        entityModelData.setContextId(String.valueOf(pageId));
    }

    @Override
    protected void processKeywordModel(KeywordModelData keywordModel) {
        if (!_isKeywordToExpand(keywordModel)) {
            return;
        }

        String keywordURI = TcmUtils.buildKeywordTcmUri(String.valueOf(pageRequest.getPublicationId()), keywordModel.getId());
        log.trace("Found keyword to expand, uri = '{}'", keywordURI);
        Keyword keyword = new TaxonomyFactory().getTaxonomyKeyword(keywordURI);

        if (keyword != null) {
            keywordModel.setDescription(keyword.getKeywordDescription())
                    .setKey(keyword.getKeywordKey())
                    .setTitle(keyword.getKeywordName())
                    .setTaxonomyId(String.valueOf(TcmUtils.getItemId(keyword.getTaxonomyURI())))
                    .setMetadata(_getMetadata(keyword, pageRequest));
        } else {
            _suppressIfNeeded("Keyword " + keywordModel.getId() + " in publication " +
                            pageRequest.getPublicationId() + " cannot be found, is it published?",
                    configService.getErrors().isMissingKeywordSuppress());
        }
    }

    @Override
    protected void processRichTextData(RichTextData richTextData) {
        Set<String> notResolvedLinks = new HashSet<>();
        List<Object> fragments = richTextData.getValues().stream()
                .map(fragment ->
                        fragment instanceof String ?
                                richTextLinkResolver.processFragment((String) fragment, pageRequest.getPublicationId(), notResolvedLinks, pageId) :
                                fragment)
                .collect(Collectors.toList());

        richTextData.setFragments(fragments);
    }

    @NotNull
    private ContentModelData _getMetadata(Keyword keyword, PageRequestDto pageRequest) {
        ContentModelData metadata = new ContentModelData();
        for (Map.Entry<String, NameValuePair> entry : keyword.getKeywordMeta().getNameValues().entrySet()) {
            String key = entry.getKey();
            NameValuePair value = entry.getValue();

            metadata.put(key, _getMetadataValues(pageRequest, value));
        }
        return metadata;
    }


    @NotNull
    private ListWrapper<?> _getMetadataValues(PageRequestDto pageRequest, NameValuePair value) {
        ListWrapper<?> values = null;

        String firstValue = String.valueOf(value.getFirstValue());
        if (TcmUtils.isTcmUri(firstValue)) {
            int itemType = TcmUtils.getItemType(firstValue);
            if (itemType == TcmUtils.KEYWORD_ITEM_TYPE) {
                List<KeywordModelData> keywords = new ArrayList<>();
                for (Object uri : value.getMultipleValues()) {
                    KeywordModelData keywordModelData = new KeywordModelData().setId(String.valueOf(TcmUtils.getItemId(String.valueOf(uri))));
                    traverseObject(keywordModelData);
                    keywords.add(keywordModelData);
                }
                values = new ListWrapper.KeywordModelDataListWrapper(keywords);
            } else if (itemType == TcmUtils.COMPONENT_ITEM_TYPE) {
                List<EntityModelData> entities = new ArrayList<>();
                for (Object uri : value.getMultipleValues()) {
                    String id = String.valueOf(TcmUtils.getItemId(String.valueOf(uri))) + "-" +
                            configService.getDefaults().getDynamicTemplateId(pageRequest.getPublicationId());
                    EntityModelData entityModelData = EntityModelData.builder().id(id).build();
                    traverseObject(entityModelData);
                    entities.add(entityModelData);
                }
                values = new ListWrapper.EntityModelDataListWrapper(entities);
            }
        }

        values = values == null ? new ListWrapper<>(value.getMultipleValues()) : values;
        return values;
    }

    private boolean _isKeywordToExpand(Object value) {
        return value instanceof KeywordModelData && ((KeywordModelData) value).getTitle() == null;
    }

    private boolean _isEntityToExpand(Object value) {
        return value instanceof EntityModelData
                && ((EntityModelData) value).getSchemaId() == null
                && ((EntityModelData) value).getId().matches("\\d+-\\d+");
    }

    private void _expandEntity(EntityModelData toExpand, PageRequestDto pageRequest) {
        EntityRequestDto entityRequest = EntityRequestDto.builder(pageRequest.getPublicationId(), toExpand.getId(), pageId).build();

        log.trace("Found entity to expand {}, request {}", toExpand.getId(), entityRequest);
        try {
            toExpand.copyFrom(entityModelService.loadEntity(entityRequest));
        } catch (ContentProviderException e) {
            _suppressIfNeeded("Cannot expand entity " + toExpand + " for page " + pageRequest, configService.getErrors().isMissingEntitySuppress(), e);
        }
    }

    private void _suppressIfNeeded(String message, boolean suppressingFlag) {
        log.warn(message);
        if (!suppressingFlag) {
            throw new DataModelExpansionException(message);
        }
    }

    private void _suppressIfNeeded(String message, boolean suppressingFlag, ContentProviderException e) {
        log.warn(message, e);
        if (!suppressingFlag) {
            throw new DataModelExpansionException(message, e);
        }
    }
}
