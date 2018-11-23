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
import com.sdl.dxa.tridion.linking.RichTextLinkResolver;
import com.sdl.webapp.common.api.content.LinkResolver;
import com.sdl.webapp.common.util.TcmUtils;
import com.tridion.meta.NameValuePair;
import com.tridion.taxonomies.Keyword;
import com.tridion.taxonomies.TaxonomyFactory;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Expands {@link PageModelData} using an instance of {@link PageRequestDto}.
 */
@Slf4j
public class EntityModelExpander extends DataModelDeepFirstSearcher {

    private EntityRequestDto entityRequest;

    private RichTextLinkResolver richTextLinkResolver;

    private LinkResolver linkResolver;

    private ConfigService configService;

    private boolean _resolveLinks = true;

    public EntityModelExpander(EntityRequestDto request,
                               RichTextLinkResolver richTextLinkResolver,
                               LinkResolver linkResolver,
                               ConfigService configService,
                               boolean resolveLinks) {
        this.entityRequest = request;
        this.richTextLinkResolver = richTextLinkResolver;
        this.linkResolver = linkResolver;
        this.configService = configService;

        this._resolveLinks = resolveLinks;
    }

    /**
     * Expands an entity data model.
     *
     * @param entity model to expand
     */
    public void expandEntity(@Nullable EntityModelData entity) {
        traverseObject(entity);
    }

    @Override
    protected void processEntityModel(EntityModelData entityModelData) {
        if(shouldResolveLinks()) {
            String componentUri = TcmUtils.buildTcmUri(String.valueOf(entityRequest.getPublicationId()), entityModelData.getId());
            entityModelData.setLinkUrl(linkResolver.resolveLink(componentUri, String.valueOf(entityRequest.getPublicationId())));
        }
    }

    private boolean shouldResolveLinks() {
        return _resolveLinks;
    }

    @Override
    protected void processKeywordModel(KeywordModelData keywordModel) {
        if (!_isKeywordToExpand(keywordModel)) {
            return;
        }

        int publicationId = entityRequest.getPublicationId();
        String keywordURI = TcmUtils.buildKeywordTcmUri(String.valueOf(publicationId), keywordModel.getId());
        log.trace("Found keyword to expand, uri = '{}'", keywordURI);
        Keyword keyword = new TaxonomyFactory().getTaxonomyKeyword(keywordURI);

        if (keyword != null) {
            keywordModel.setDescription(keyword.getKeywordDescription())
                    .setKey(keyword.getKeywordKey())
                    .setTitle(keyword.getKeywordName())
                    .setTaxonomyId(String.valueOf(TcmUtils.getItemId(keyword.getTaxonomyURI())))
                    .setMetadata(_getKeywordMetadata(keyword, publicationId));
        } else {
            _suppressIfNeeded("Keyword " + keywordModel.getId() + " in publication " +
                            publicationId + " cannot be found, is it published?",
                    configService.getErrors().isMissingKeywordSuppress());
        }
    }

    @Override
    protected void processRichTextData(RichTextData richTextData) {
        Set<String> notResolvedLinks = new HashSet<>();
        List<Object> fragments = richTextData.getValues().stream()
                .map(fragment ->
                        fragment instanceof String ?
                                richTextLinkResolver.processFragment((String) fragment, entityRequest.getPublicationId(), notResolvedLinks) :
                                fragment)
                .collect(Collectors.toList());

        richTextData.setFragments(fragments);
    }

    @NotNull
    private ContentModelData _getKeywordMetadata(Keyword keyword, int publicationId) {
        ContentModelData metadata = new ContentModelData();
        for (Map.Entry<String, NameValuePair> entry : keyword.getKeywordMeta().getNameValues().entrySet()) {
            String key = entry.getKey();
            NameValuePair value = entry.getValue();

            metadata.put(key, _getMetadataValues(publicationId, value));
        }
        return metadata;
    }

    @NotNull
    private ListWrapper<?> _getMetadataValues(int publicationId, NameValuePair value) {
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
                            configService.getDefaults().getDynamicTemplateId(publicationId);
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

    private void _suppressIfNeeded(String message, boolean suppressingFlag) {
        log.warn(message);
        if (!suppressingFlag) {
            throw new DataModelExpansionException(message);
        }
    }
}
