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
import com.sdl.dxa.tridion.linking.api.BatchLinkResolver;
import com.sdl.dxa.tridion.linking.api.descriptors.SingleLinkDescriptor;
import com.sdl.dxa.tridion.linking.descriptors.ComponentLinkDescriptor;
import com.sdl.dxa.tridion.linking.descriptors.DynamicComponentLinkDescriptor;
import com.sdl.dxa.tridion.linking.descriptors.RichTextLinkDescriptor;
import com.sdl.dxa.tridion.linking.processors.EntityLinkProcessor;
import com.sdl.dxa.tridion.linking.processors.FragmentListProcessor;
import com.sdl.webapp.common.api.content.LinkResolver;
import com.sdl.webapp.common.util.TcmUtils;
import com.tridion.meta.NameValuePair;
import com.tridion.taxonomies.Keyword;
import com.tridion.taxonomies.TaxonomyFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.sdl.dxa.utils.FragmentUtils.assignUUIDsToRichTextFragments;

/**
 * Expands {@link PageModelData} using an instance of {@link PageRequestDto}.
 */
@Slf4j
public class EntityModelExpander extends DataModelDeepFirstSearcher {

    private EntityRequestDto entityRequest;

    private RichTextLinkResolver richTextLinkResolver;

    private LinkResolver linkResolver;

    private BatchLinkResolver batchLinkResolver;

    private ConfigService configService;

    private boolean _resolveLinks = true;

    public EntityModelExpander(EntityRequestDto request,
                               RichTextLinkResolver richTextLinkResolver,
                               LinkResolver linkResolver,
                               ConfigService configService,
                               boolean resolveLinks,
                               BatchLinkResolver batchLinkResolver) {
        this.entityRequest = request;
        this.richTextLinkResolver = richTextLinkResolver;
        this.linkResolver = linkResolver;
        this.configService = configService;

        this._resolveLinks = resolveLinks;
        this.batchLinkResolver = batchLinkResolver;

    }

    /**
     * Expands an entity data model.
     *
     * @param entity model to expand
     */
    public void expandEntity(@Nullable EntityModelData entity) {
        traverseObject(entity);
        if (shouldResolveLinks()) {
            this.batchLinkResolver.resolveAndFlush();
        }
    }

    private boolean shouldResolveLinks() {
        return _resolveLinks;
    }

    @Override
    protected void processEntityModel(EntityModelData entityModelData) {
        if (shouldResolveLinks()) {
            SingleLinkDescriptor ld;
            if(entityModelData.getId().matches("\\d+-\\d+")) {
                ld = new DynamicComponentLinkDescriptor(entityRequest.getPublicationId(),
                        this.entityRequest.getContextId(),
                        new EntityLinkProcessor(entityModelData));
            } else {
                ld = new ComponentLinkDescriptor(entityRequest.getPublicationId(),
                 this.entityRequest.getContextId(), new EntityLinkProcessor(entityModelData));
            }

            this.batchLinkResolver.dispatchLinkResolution(ld);
        }
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

        long start = System.currentTimeMillis();

        if (shouldResolveLinks()) {
            final List<Object> fragments = assignUUIDsToRichTextFragments(richTextData);

            log.info("Processing {} fragments.", fragments.size());
            richTextData.setFragments(fragments);

            for (Object fragment : fragments) {
                if (fragment instanceof ImmutablePair) {

                    this.batchLinkResolver.dispatchMultipleLinksResolution(
                            new RichTextLinkDescriptor(
                                    entityRequest.getPublicationId(), this.entityRequest.getContextId(),
                                    richTextLinkResolver.retrieveAllLinksFromFragment((String) ((ImmutablePair) fragment).getRight()),
                                    new FragmentListProcessor(
                                            richTextData, (ImmutablePair<String, String>) fragment,
                                            this.richTextLinkResolver)
                            )
                    );

                }
            }
        } else {
            log.debug(">>> Did not resolve links.");
        }

        log.info("Entity Model RTF resolving took: {} ms.", ((System.currentTimeMillis() - start)));
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
