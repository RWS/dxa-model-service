package com.sdl.dxa.modelservice.service.processing.conversion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdl.dxa.api.datamodel.model.ComponentTemplateData;
import com.sdl.dxa.api.datamodel.model.ContentModelData;
import com.sdl.dxa.api.datamodel.model.EntityModelData;
import com.sdl.dxa.api.datamodel.model.KeywordModelData;
import com.sdl.dxa.api.datamodel.model.PageModelData;
import com.sdl.dxa.api.datamodel.model.PageTemplateData;
import com.sdl.dxa.api.datamodel.model.RegionModelData;
import com.sdl.dxa.api.datamodel.model.RichTextData;
import com.sdl.dxa.api.datamodel.model.util.ListWrapper;
import com.sdl.dxa.common.dto.EntityRequestDto;
import com.sdl.dxa.common.dto.PageRequestDto;
import com.sdl.dxa.common.util.PathUtils;
import com.sdl.dxa.modelservice.service.ConfigService;
import com.sdl.dxa.modelservice.service.ContentService;
import com.sdl.dxa.modelservice.service.EntityModelService;
import com.sdl.dxa.modelservice.service.processing.conversion.models.AdoptedRichTextField;
import com.sdl.dxa.modelservice.service.processing.conversion.models.LightSitemapItem;
import com.sdl.webapp.common.api.content.ContentProviderException;
import com.sdl.webapp.common.api.mapping.semantic.config.FieldPath;
import com.sdl.webapp.common.impl.localization.semantics.JsonSchema;
import com.sdl.webapp.common.impl.localization.semantics.JsonSchemaField;
import com.sdl.webapp.common.util.TcmUtils;
import com.tridion.dcp.ComponentPresentationFactory;
import com.tridion.meta.ComponentMeta;
import com.tridion.meta.PageMeta;
import com.tridion.meta.PublicationMeta;
import lombok.extern.slf4j.Slf4j;
import org.dd4t.contentmodel.Category;
import org.dd4t.contentmodel.Component;
import org.dd4t.contentmodel.ComponentPresentation;
import org.dd4t.contentmodel.ComponentTemplate;
import org.dd4t.contentmodel.Field;
import org.dd4t.contentmodel.FieldSet;
import org.dd4t.contentmodel.Keyword;
import org.dd4t.contentmodel.Page;
import org.dd4t.contentmodel.PageTemplate;
import org.dd4t.contentmodel.Publication;
import org.dd4t.contentmodel.impl.CategoryImpl;
import org.dd4t.contentmodel.impl.ComponentImpl;
import org.dd4t.contentmodel.impl.ComponentLinkField;
import org.dd4t.contentmodel.impl.ComponentPresentationImpl;
import org.dd4t.contentmodel.impl.ComponentTemplateImpl;
import org.dd4t.contentmodel.impl.EmbeddedField;
import org.dd4t.contentmodel.impl.FieldSetImpl;
import org.dd4t.contentmodel.impl.KeywordField;
import org.dd4t.contentmodel.impl.KeywordImpl;
import org.dd4t.contentmodel.impl.OrganizationalItemImpl;
import org.dd4t.contentmodel.impl.PageImpl;
import org.dd4t.contentmodel.impl.PageTemplateImpl;
import org.dd4t.contentmodel.impl.PublicationImpl;
import org.dd4t.contentmodel.impl.SchemaImpl;
import org.dd4t.contentmodel.impl.StructureGroupImpl;
import org.dd4t.contentmodel.impl.TextField;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.sdl.webapp.common.util.TcmUtils.PAGE_TEMPLATE_ITEM_TYPE;
import static com.sdl.webapp.common.util.TcmUtils.buildPageTcmUri;

@Slf4j
@org.springframework.stereotype.Component
public class ToDd4tConverterImpl implements ToDd4tConverter {

    private final ContentService contentService;

    private final EntityModelService entityModelService;

    private final ConfigService configService;

    private final ObjectMapper objectMapper;

    private final MetadataService metadataService;

    @Autowired
    public ToDd4tConverterImpl(ContentService contentService,
                               EntityModelService entityModelService,
                               ConfigService configService,
                               @Qualifier("dxaR2ObjectMapper") ObjectMapper objectMapper,
                               MetadataService metadataService) {
        this.contentService = contentService;
        this.entityModelService = entityModelService;
        this.configService = configService;
        this.objectMapper = objectMapper;
        this.metadataService = metadataService;
    }

    @Override
    public Page convertToDd4t(@Nullable PageModelData toConvert, @NotNull PageRequestDto pageRequest) throws ContentProviderException {
        if (toConvert == null) {
            log.warn("PageModelData to convert is null, returning null");
            return null;
        }

        PageImpl page = new PageImpl();

        int publicationId = pageRequest.getPublicationId();
        String pageTcmUri = buildPageTcmUri(publicationId, toConvert.getId());

        // first set everything obvious at the top-level
        page.setId(pageTcmUri);
        page.setTitle(toConvert.getTitle());

        // then load page metadata and fill the rest at the top-level
        PageMeta pageMeta = metadataService.getPageMeta(publicationId, pageTcmUri);
        page.setVersion(pageMeta.getMajorVersion());
        page.setLastPublishedDate(new DateTime(pageMeta.getLastPublicationDate()));
        page.setRevisionDate(new DateTime(pageMeta.getModificationDate()));
        page.setFileName(PathUtils.getFileName(pageMeta.getPath()));
        page.setFileExtension(PathUtils.getExtension(pageMeta.getPath()));

        // top-level /Publication and /OwningPublication
        page.setPublication(_loadPublication(publicationId));
        page.setOwningPublication(_loadPublication(pageMeta.getOwningPublicationId()));

        page.setStructureGroup(_loadStructureGroup(toConvert, pageRequest, page));

        page.setMetadata(_convertContent(toConvert.getMetadata(), publicationId));

        page.setPageTemplate(_buildPageTemplate(toConvert.getPageTemplate(), publicationId));

        // component presentations, one CP per one top-level (not embedded, not from include page) EMD
        if (toConvert.getRegions() != null) {
            List<ComponentPresentation> presentations = new ArrayList<>();
            ComponentPresentationFactory componentPresentationFactory = new ComponentPresentationFactory(publicationId);
            for (RegionModelData region : toConvert.getRegions()) {
                presentations.addAll(_loadComponentPresentations(region, publicationId, componentPresentationFactory));
            }
            page.setComponentPresentations(presentations);
        }

        return page;
    }

    @Override
    public ComponentPresentation convertToDd4t(@Nullable EntityModelData toConvert, @NotNull EntityRequestDto entityRequest) throws ContentProviderException {
        if (toConvert == null) {
            log.warn("EntityModelData to convert is null, returning null");
            return null;
        }

        return _buildEntityModel(toConvert, entityRequest.getPublicationId(), new ComponentPresentationFactory(entityRequest.getPublicationId()));
    }

    @Nullable
    private StructureGroupImpl _loadStructureGroup(@NotNull PageModelData toConvert, @NotNull PageRequestDto pageRequest, PageImpl page) throws ContentProviderException {
        if (toConvert.getStructureGroupId() == null) {
            return null;
        }

        String publicationUrl = metadataService.getPublicationMeta(pageRequest.getPublicationId()).getPublicationUrl();
        if (publicationUrl == null) {
            return null;
        }
        PageRequestDto navigationJsonRequest = pageRequest.toBuilder()
                .path(PathUtils.combinePath(publicationUrl, "navigation.json"))
                .build();
        String content = contentService.loadPageContent(navigationJsonRequest);
        Optional<LightSitemapItem> sitemapItem;
        try {
            sitemapItem = objectMapper.readValue(content, LightSitemapItem.class).findWithId(TcmUtils.buildTcmUri(
                    pageRequest.getPublicationId(), toConvert.getStructureGroupId(), TcmUtils.STRUCTURE_GROUP_ITEM_TYPE));
        } catch (IOException e) {
            throw new ContentProviderException("Error parsing navigation.json", e);
        }

        if (sitemapItem.isPresent()) {
            StructureGroupImpl structureGroup = new StructureGroupImpl();
            structureGroup.setPublicationId(page.getPublication().getId());
            structureGroup.setId(sitemapItem.get().getId());
            structureGroup.setTitle(sitemapItem.get().getTitle());
            return structureGroup;
        } else {
            return null;
        }
    }

    @NotNull
    private PageTemplate _buildPageTemplate(@NotNull PageTemplateData pageTemplateData, int publicationId) throws ContentProviderException {
        PageTemplate pageTemplate = new PageTemplateImpl();
        pageTemplate.setId(TcmUtils.buildTcmUri(publicationId, pageTemplateData.getId(), PAGE_TEMPLATE_ITEM_TYPE));
        pageTemplate.setTitle(pageTemplateData.getTitle());
        pageTemplate.setFileExtension(pageTemplateData.getFileExtension());
        pageTemplate.setRevisionDate(pageTemplateData.getRevisionDate());
        pageTemplate.setMetadata(_convertContent(pageTemplateData.getMetadata(), publicationId));
        return pageTemplate;
    }

    private Publication _loadPublication(int publicationId) throws ContentProviderException {
        PublicationMeta publicationMeta = metadataService.getPublicationMeta(publicationId);

        if (publicationMeta == null || publicationMeta.getId() == 0) {
            log.info("Publication with id {} is not published. Can not load publication data", publicationId);
            return null;
        }

        Publication publication = new PublicationImpl(TcmUtils.buildPublicationTcmUri(publicationMeta.getId()));
        publication.setTitle(publicationMeta.getTitle());
        publication.setId(TcmUtils.buildPublicationTcmUri(publicationMeta.getId()));
//        publication.setCustomProperties();
//        publication.setExtensionData();
        return publication;
    }

    private List<ComponentPresentation> _loadComponentPresentations(@NotNull RegionModelData region,
                                                                    int publicationId, ComponentPresentationFactory factory) throws ContentProviderException {
        if (region.getIncludePageId() != null) { // include pages are not needed in DD4T representation
            log.debug("Skipping region {} because it's an include page", region);
            return Collections.emptyList();
        }

        List<ComponentPresentation> presentations = new ArrayList<>();
        if (region.getRegions() != null) {
            for (RegionModelData nested : region.getRegions()) {
                presentations.addAll(_loadComponentPresentations(nested, publicationId, factory));
            }
        }

        if (region.getEntities() != null) {
            for (EntityModelData entity : region.getEntities()) {
                presentations.add(_buildEntityModel(entity, publicationId, factory));
            }

        }
        return presentations;
    }


    private ComponentPresentation _buildEntityModel(EntityModelData entity, int publicationId, ComponentPresentationFactory factory) throws ContentProviderException {
        ComponentPresentation presentation = new ComponentPresentationImpl();
        presentation.setIsDynamic(entity.getId().matches("\\d+-\\d+"));

        EntityModelData entityModelData = presentation.isDynamic() ?
                entityModelService.loadEntity(EntityRequestDto.builder(publicationId, entity.getId()).build()) : entity;

        presentation.setComponent(_convertEntity(entityModelData, publicationId));
        presentation.setComponentTemplate(_buildComponentTemplate(entityModelData.getComponentTemplate(), publicationId));
        // todo OrderOnPage ?
        return presentation;
    }

    @NotNull
    private ComponentTemplate _buildComponentTemplate(ComponentTemplateData componentTemplateData, int publicationId) throws ContentProviderException {
        ComponentTemplateImpl componentTemplate = new ComponentTemplateImpl();
        componentTemplate.setId(TcmUtils.buildTcmUri(publicationId, componentTemplateData.getId(), TcmUtils.COMPONENT_TEMPLATE_ITEM_TYPE));
        componentTemplate.setTitle(componentTemplateData.getTitle());
        componentTemplate.setRevisionDate(componentTemplateData.getRevisionDate());
        componentTemplate.setOutputFormat(componentTemplateData.getOutputFormat());
        componentTemplate.setMetadata(_convertContent(componentTemplateData.getMetadata(), publicationId));
        return componentTemplate;
    }

    private Component _convertEntity(EntityModelData entity, int publicationId) throws ContentProviderException {
        String entityId = entity.getId();
        if (entityId.matches("\\d+-\\d+")) {
            entityId = entityId.split("-")[0];
        }
        ComponentMeta meta = metadataService.getComponentMeta(publicationId, Integer.parseInt(entityId));
        ComponentImpl component = new ComponentImpl();
        component.setId(TcmUtils.buildTcmUri(String.valueOf(publicationId), entityId));
        component.setTitle(meta.getTitle());
        component.setContent(_convertContent(entity.getContent(), publicationId,
                configService.getDefaults().getSchemasJson(publicationId).get(entity.getSchemaId()), null, 0));
        component.setLastPublishedDate(new DateTime(meta.getLastPublicationDate()));
        component.setRevisionDate(new DateTime(meta.getModificationDate()));
        component.setMetadata(_convertContent(entity.getMetadata(), publicationId));
        component.setPublication(_loadPublication(meta.getPublicationId()));
        component.setOwningPublication(_loadPublication(meta.getOwningPublicationId()));

        if (entity.getSchemaId() != null) {
            JsonSchema jsonSchema = configService.getDefaults().getSchemasJson(publicationId).get(entity.getSchemaId());
            SchemaImpl schema = new SchemaImpl();
            schema.setRootElement(jsonSchema.getRootElement());
            schema.setId(entity.getSchemaId());
            schema.setTitle(jsonSchema.getRootElement());
            // todo RevisionDate&LastPublishedDate are not available in CIL/CM
            component.setSchema(schema);
        }

        component.setComponentType(meta.isMultimedia() ? Component.ComponentType.MULTIMEDIA : Component.ComponentType.NORMAL);

        // todo load /Folder:
        component.setOrganizationalItem(new OrganizationalItemImpl());

        component.setCategories(Arrays.stream(meta.getCategories())
                .map(category -> {
                    Category cat = new CategoryImpl();
//                   cat.setId();
                    cat.setTitle(category.getName());
                    cat.setKeywords(Arrays.stream(category.getKeywordList())
                            .map(keyword -> {
                                Keyword kwd = new KeywordImpl();
                                kwd.setIsAbstract(keyword.isKeywordAbstract());
                                kwd.setTaxonomyId(keyword.getTaxonomyURI());
                                kwd.setId(keyword.getKeywordURI());
                                kwd.setTitle(keyword.getKeywordName());
//                                keyword.getKeywordMeta().
//                                kwd.setIsRoot(keyword.);
//                                kwd.setPath();
//                                kwd.setCustomProperties();
//                                kwd.setExtensionData();
//                                kwd.setMetadata();
                                return kwd;
                            })
                            .collect(Collectors.toList()));
//                    cat.setCustomProperties();
//                    cat.setExtensionData();
                    return cat;
                }).collect(Collectors.toList()));
        component.setVersion(meta.getMajorVersion());
        component.setPublication(_loadPublication(meta.getPublicationId()));
        component.setPublication(_loadPublication(meta.getOwningPublicationId()));
        return component;
    }

    @Contract("null, _ -> null; !null, _ -> !null")
    private Map<String, Field> _convertContent(ContentModelData contentModelData, int publicationId) throws ContentProviderException {
        return _convertContent(contentModelData, publicationId, null, null, 0);
    }

    @Contract("null, _, _, _, _ -> null; !null, _, _, _, _ -> !null")
    private Map<String, Field> _convertContent(ContentModelData contentModelData, int publicationId,
                                               @Nullable JsonSchema schema, @Nullable JsonSchemaField schemaField, int multivalueCounter) throws ContentProviderException {
        if (contentModelData == null) {
            return null;
        }

        Map<String, Field> content = new HashMap<>();
        for (Map.Entry<String, Object> entry : contentModelData.entrySet()) {
            Field convertedField = _convertToField(entry, publicationId, schema, schemaField, multivalueCounter);
            if (convertedField != null) {
                content.put(entry.getKey(), convertedField);
            } else {
                log.warn("Couldn't convert {}", contentModelData);
            }
        }
        return content;
    }

    @Contract("null, _ -> null; _, null -> null")
    private JsonSchemaField _findCurrentField(@Nullable List<JsonSchemaField> jsonSchemaFields, @Nullable String name) {
        if (jsonSchemaFields == null || name == null) {
            return null;
        }

        return jsonSchemaFields.parallelStream().filter(jsonSchemaField -> name.equals(jsonSchemaField.getName()))
                .findFirst()
                .orElse(null);
    }

    @Nullable
    private Field _convertToField(@NotNull Map.Entry<String, Object> entry, int publicationId,
                                  @Nullable JsonSchema contextSchema, @Nullable JsonSchemaField contextSchemaField, int multivalueCounter) throws ContentProviderException {
        Object value = entry.getValue();
        Field field = null;

        List<JsonSchemaField> currentFields = _getNestedSchemaFields(contextSchema, contextSchemaField);
        JsonSchemaField currentField = _findCurrentField(currentFields, entry.getKey());

        if (value instanceof ListWrapper) {
            field = _convertListWrapperToField((ListWrapper) value, publicationId, contextSchema);
        } else if (value instanceof ContentModelData) {
            field = _convertEmbeddedToField(Collections.singletonList((ContentModelData) value), publicationId, contextSchema, currentField);
        } else if (value instanceof EntityModelData) {
            field = _convertToCompLinkField(Collections.singletonList((EntityModelData) value), publicationId);
        } else if (value instanceof KeywordModelData) {
            field = _convertToKeywordField(Collections.singletonList((KeywordModelData) value), publicationId);
        } else if (value instanceof RichTextData) {
            field = _convertToRichTextField(Collections.singletonList((RichTextData) value), publicationId);
        } else if (value instanceof String) {
            // todo here we need to derive type from schemas.json because not everything is String in DD4T like it is in R2
            field = _convertToTextField(Collections.singletonList((String) value));
        } else {
            log.warn("Field of type {} is not supported", value.getClass());
        }

        if (field != null) {
            field.setName(entry.getKey());
            if (currentField != null) {
                field.setXPath(_getXPathFromContext(currentField, contextSchemaField, multivalueCounter));
            }
        }

        return field;
    }

    @NotNull
    private String _getXPathFromContext(JsonSchemaField currentField, @Nullable JsonSchemaField contextSchemaField, int multivalueCounter) {
        String currentFieldPath = currentField.getPath();
        if (contextSchemaField != null && multivalueCounter != 0) {
            log.debug("Field {} exists in a context of an embedded field {}, it's {} value (counting)", currentField, contextSchemaField, multivalueCounter);
            currentFieldPath = currentFieldPath.replaceFirst(contextSchemaField.getPath(),
                    contextSchemaField.getPath() + String.format("[%s]", multivalueCounter));
        }
        return new FieldPath(currentFieldPath).getXPath(null);
    }

    @Nullable
    private List<JsonSchemaField> _getNestedSchemaFields(@Nullable JsonSchema schema, @Nullable JsonSchemaField schemaField) {
        return schemaField != null ? schemaField.getFields() : (schema != null ? schema.getFields() : null);
    }

    private Keyword _convertKeyword(KeywordModelData kmd, int publicationId) {
        KeywordImpl keyword = new KeywordImpl();
        //todo finish
        keyword.setTitle(kmd.getTitle());
        keyword.setId(TcmUtils.buildKeywordTcmUri(String.valueOf(publicationId), kmd.getId()));
        keyword.setTaxonomyId(TcmUtils.buildKeywordTcmUri(String.valueOf(publicationId), kmd.getTaxonomyId()));
        keyword.setKey(kmd.getKey());
        keyword.setDescription(kmd.getDescription());
        return keyword;
    }

    private Field _convertListWrapperToField(ListWrapper<?> wrapper, int publicationId, @Nullable JsonSchema schema) throws ContentProviderException {
        if (!wrapper.empty()) {
            if (wrapper instanceof ListWrapper.ContentModelDataListWrapper) {
                return _convertEmbeddedToField(((ListWrapper.ContentModelDataListWrapper) wrapper).getValues(), publicationId, schema, null);
            } else if (wrapper instanceof ListWrapper.KeywordModelDataListWrapper) {
                return _convertToKeywordField(((ListWrapper.KeywordModelDataListWrapper) wrapper).getValues(), publicationId);
            } else if (wrapper instanceof ListWrapper.EntityModelDataListWrapper) {
                return _convertToCompLinkField(((ListWrapper.EntityModelDataListWrapper) wrapper).getValues(), publicationId);
            } else if (wrapper instanceof ListWrapper.RichTextDataListWrapper) {
                return _convertToRichTextField(((ListWrapper.RichTextDataListWrapper) wrapper).getValues(), publicationId);
            } else {
                Object o = wrapper.get(0);
                if (o instanceof String) {
                    // typesafe because explicitly checked first element and assume other to be of the same type
                    //noinspection unchecked
                    return _convertToTextField((List<String>) wrapper.getValues());
                } else {
                    log.warn("Unspecific ListWrappers of type {} are not supported", o.getClass());
                }
            }
        }
        return null;
    }

    private EmbeddedField _convertEmbeddedToField(List<ContentModelData> cmds, int publicationId, @Nullable JsonSchema schema, JsonSchemaField schemaField) throws ContentProviderException {
        EmbeddedField embeddedField = new EmbeddedField();

        List<FieldSet> fieldSets = new ArrayList<>();
        for (int i = 0, cmdsSize = cmds.size(); i < cmdsSize; i++) {
            ContentModelData contentModelData = cmds.get(i);
            FieldSet fieldSet = new FieldSetImpl();
            fieldSet.setContent(_convertContent(contentModelData, publicationId, schema, schemaField, i + 1));
            fieldSets.add(fieldSet);
        }

        embeddedField.setEmbeddedValues(fieldSets);
        return embeddedField;
    }

    private ComponentLinkField _convertToCompLinkField(List<EntityModelData> emds, int publicationId) throws ContentProviderException {
        ComponentLinkField linkField = new ComponentLinkField();

        List<Component> components = new ArrayList<>(emds.size());
        for (EntityModelData emd : emds) {
            components.add(_convertEntity(emd, publicationId));
        }

        linkField.setLinkedComponentValues(components);
        return linkField;
    }

    private TextField _convertToTextField(List<String> strings) {
        TextField textField = new TextField();
        textField.setTextValues(strings);
        return textField;
    }

    private KeywordField _convertToKeywordField(List<KeywordModelData> kmds, int publicationId) {
        KeywordField keywordField = new KeywordField();

        List<Keyword> keywords = new ArrayList<>(kmds.size());
        for (KeywordModelData kmd : kmds) {
            keywords.add(_convertKeyword(kmd, publicationId));
        }

        keywordField.setKeywords(keywords);
        return keywordField;
    }

    private Field _convertToRichTextField(List<RichTextData> richTextData, int publicationId) throws ContentProviderException {
        // todo make this work TSI-2698
        List<Object> list = new ArrayList<>();
        AdoptedRichTextField richTextField = new AdoptedRichTextField();
        for (RichTextData data : richTextData) {
            for (Object o : data.getFragments()) {
                if (o instanceof String) {
                    list.add(o);
                } else if (o instanceof EntityModelData) {
                    list.add(_convertEntity((EntityModelData) o, publicationId));
                }
            }
        }
        richTextField.setRichTextValues(list);
        return richTextField;
    }
}
