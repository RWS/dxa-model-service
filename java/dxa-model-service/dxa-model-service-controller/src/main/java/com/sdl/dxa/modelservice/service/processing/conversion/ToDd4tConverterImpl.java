package com.sdl.dxa.modelservice.service.processing.conversion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdl.dxa.api.datamodel.model.ContentModelData;
import com.sdl.dxa.api.datamodel.model.EntityModelData;
import com.sdl.dxa.api.datamodel.model.PageModelData;
import com.sdl.dxa.api.datamodel.model.PageTemplateData;
import com.sdl.dxa.api.datamodel.model.RegionModelData;
import com.sdl.dxa.api.datamodel.model.util.ListWrapper;
import com.sdl.dxa.common.dto.PageRequestDto;
import com.sdl.dxa.common.util.PathUtils;
import com.sdl.dxa.modelservice.service.ConfigService;
import com.sdl.dxa.modelservice.service.ContentService;
import com.sdl.dxa.modelservice.service.processing.conversion.models.LightSchema;
import com.sdl.dxa.modelservice.service.processing.conversion.models.LightSitemapItem;
import com.sdl.webapp.common.api.content.ContentProviderException;
import com.sdl.webapp.common.util.TcmUtils;
import com.tridion.dcp.ComponentPresentationFactory;
import com.tridion.meta.ComponentMeta;
import com.tridion.meta.ComponentMetaFactory;
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
import org.dd4t.contentmodel.impl.KeywordImpl;
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

    private final ConfigService configService;

    private final ObjectMapper objectMapper;

    private final MetadataService metadataService;

    @Autowired
    public ToDd4tConverterImpl(ContentService contentService,
                               ConfigService configService,
                               @Qualifier("dxaR2ObjectMapper") ObjectMapper objectMapper,
                               MetadataService metadataService) {
        this.contentService = contentService;
        this.configService = configService;
        this.objectMapper = objectMapper;
        this.metadataService = metadataService;
    }

    @Override
    public Page convertToDd4t(@Nullable PageModelData toConvert, @NotNull PageRequestDto pageRequest) throws ContentProviderException {
        if (toConvert == null) {
            log.warn("Model to convert is null, returning null");
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

        page.setMetadata(_convertContent(toConvert.getMetadata(), pageRequest));

        page.setPageTemplate(_buildPageTemplate(toConvert.getPageTemplate(), pageRequest));

        // component presentations, one CP per one top-level (not embedded) EMD
        if (toConvert.getRegions() != null) {
            List<ComponentPresentation> presentations = new ArrayList<>();
            ComponentPresentationFactory componentPresentationFactory = new ComponentPresentationFactory(publicationId);
            for (RegionModelData region : toConvert.getRegions()) {
                presentations.addAll(_loadComponentPresentations(region, pageRequest, componentPresentationFactory));
            }
            page.setComponentPresentations(presentations);
        }

        return page;
    }

    @Nullable
    private StructureGroupImpl _loadStructureGroup(@NotNull PageModelData toConvert, @NotNull PageRequestDto pageRequest, PageImpl page) throws ContentProviderException {
        if (toConvert.getStructureGroupId() == null) {
            return null;
        }

        String content = contentService.loadPageContent(pageRequest.toBuilder().path(pageRequest.getPath() + "/navigation.json").build());
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
    private PageTemplate _buildPageTemplate(@NotNull PageTemplateData pageTemplateData, @NotNull PageRequestDto pageRequest) throws ContentProviderException {
        PageTemplate pageTemplate = new PageTemplateImpl();
        pageTemplate.setId(TcmUtils.buildTcmUri(pageTemplateData.getId(), pageRequest.getPublicationId(), PAGE_TEMPLATE_ITEM_TYPE));
        pageTemplate.setTitle(pageTemplateData.getTitle());
        pageTemplate.setFileExtension(pageTemplateData.getFileExtension());
        pageTemplate.setRevisionDate(pageTemplateData.getRevisionDate());
        pageTemplate.setMetadata(_convertContent(pageTemplateData.getMetadata(), pageRequest));
        return pageTemplate;
    }

    private Publication _loadPublication(int publicationId) throws ContentProviderException {
        PublicationMeta publicationMeta = metadataService.getPublicationMeta(publicationId);
        Publication publication = new PublicationImpl(TcmUtils.buildPublicationTcmUri(publicationMeta.getId()));
        publication.setTitle(publicationMeta.getTitle());
        return publication;
    }

    private List<ComponentPresentation> _loadComponentPresentations(@NotNull RegionModelData region,
                                                                    @NotNull PageRequestDto pageRequestDto, ComponentPresentationFactory factory) throws ContentProviderException {

        List<ComponentPresentation> presentations = new ArrayList<>();
        if (region.getRegions() != null) {
            for (RegionModelData nested : region.getRegions()) {
                presentations.addAll(_loadComponentPresentations(nested, pageRequestDto, factory));
            }
        }

        if (region.getEntities() != null) {
            for (EntityModelData entity : region.getEntities()) {
                presentations.add(_buildEntityModel(entity, pageRequestDto, factory));
            }

        }
        return presentations;
    }


    public ComponentPresentation _buildEntityModel(EntityModelData entity, @NotNull PageRequestDto pageRequestDto, ComponentPresentationFactory factory) throws ContentProviderException {
//        String componentUri = TcmUtils.buildTcmUri(String.valueOf(pageRequestDto.getPublicationId()), entity.getId());
//        com.tridion.dcp.ComponentPresentation cp = factory.getComponentPresentation(componentUri, entity.getComponentTemplateId());


        ComponentPresentation presentation = new ComponentPresentationImpl();
        presentation.setComponent(_convertEntity(entity, pageRequestDto));
        presentation.setComponentTemplate(_buildComponentTemplate());
        return presentation;
    }

    @NotNull
    private ComponentTemplate _buildComponentTemplate() {
        //        template.setId(TcmUtils.buildTemplateTcmUri(pageRequestDto.getPublicationId(), cp.getComponentTemplateId()));
        // todo load template and fill the rest


        // todo OrderOnPage ?
        // todo dynamic = only DCPs?
        return new ComponentTemplateImpl();
    }

    private Component _convertEntity(EntityModelData entity, @NotNull PageRequestDto pageRequestDto) throws ContentProviderException {
        ComponentMeta meta = new ComponentMetaFactory(pageRequestDto.getPublicationId())
                .getMeta(TcmUtils.buildTcmUri(pageRequestDto.getPublicationId(), entity.getId()));

        ComponentImpl component = new ComponentImpl();
        component.setId(TcmUtils.buildTcmUri(String.valueOf(pageRequestDto.getPublicationId()), entity.getId()));
        component.setTitle(meta.getTitle());
        component.setContent(_convertContent(entity.getContent(), pageRequestDto));
        component.setLastPublishedDate(new DateTime(meta.getLastPublicationDate()));
        component.setRevisionDate(new DateTime(meta.getModificationDate()));
        component.setMetadata(_convertContent(entity.getMetadata(), pageRequestDto));

        LightSchema lightSchema = configService.getDefaults().getSchemasJson(pageRequestDto.getPublicationId()).get(entity.getSchemaId());
        SchemaImpl schema = new SchemaImpl();
        schema.setRootElement(lightSchema.getRootElement());
        schema.setId(lightSchema.getId());
        schema.setTitle(lightSchema.getTitle());
        component.setSchema(schema);

        // todo ComponentType
        // todo Folder
        component.setCategories(Arrays.stream(meta.getCategories())
                .map(category -> {
                    Category result = new CategoryImpl();
                    // todo id
                    result.setTitle(category.getName());

                    result.setKeywords(Arrays.stream(category.getKeywordList())
                            .map(keyword -> {
                                Keyword kwd = new KeywordImpl();
                                // todo the rest of keyword
                                kwd.setTitle(keyword.getKeywordName());
                                return kwd;
                            }).collect(Collectors.toList()));
                    // todo rest of category
                    return result;
                }).collect(Collectors.toList()));
        component.setVersion(meta.getMajorVersion());
        component.setPublication(_loadPublication(meta.getPublicationId()));
        component.setPublication(_loadPublication(meta.getOwningPublicationId()));
        return component;
    }

    @Contract("null, _ -> null; !null, _ -> !null")
    private Map<String, Field> _convertContent(ContentModelData contentModelData, @NotNull PageRequestDto pageRequestDto) throws ContentProviderException {
        if (contentModelData == null) {
            return null;
        }

        Map<String, Field> content = new HashMap<>();
        for (Map.Entry<String, Object> entry : contentModelData.entrySet()) {
            Field convertedField = _convertToField(entry, pageRequestDto);
            if (convertedField != null) {
                content.put(entry.getKey(), convertedField);
            } else {
                log.warn("Couldn't convert {}", contentModelData);
            }
        }
        return content;
    }

    @Nullable
    private Field _convertToField(@NotNull Map.Entry<String, Object> entry, @NotNull PageRequestDto pageRequest) throws ContentProviderException {
        Object value = entry.getValue();
        Field field = null;

        if (value instanceof ContentModelData) {
            field = _convertEmbeddedToField((ContentModelData) value, pageRequest);
        } else if (value instanceof ListWrapper.ContentModelDataListWrapper) {
            field = _convertEmbeddedToField((ListWrapper.ContentModelDataListWrapper) value, pageRequest);
        } else if (value instanceof String) {
            // todo here we need to derive type from schemas.json because not everything is String in DD4T like it is in R2
            field = _convertToTextField(Collections.singletonList((String) value));
        } else if (value instanceof EntityModelData) {
            field = _convertToCompLinkField((EntityModelData) value, pageRequest);
        } else if (value instanceof ListWrapper) {
            // some non-specific ListWrapper
            field = _convertListWrapperToField((ListWrapper) value);
        } else {
            // todo add KMD, EMD[]
            log.warn("Field of type {} is not supported", value.getClass());
        }

        if (field != null) {
            field.setName(entry.getKey());
        }

        return field;
    }


    private Field _convertListWrapperToField(ListWrapper<?> wrapper) {
        if (!wrapper.empty()) {
            Object o = wrapper.get(0);
            if (o instanceof String) {
                // typesafe because explicitly checked first element and assume other to be of the same type
                //noinspection unchecked
                return _convertToTextField((List<String>) wrapper.getValues());
            } else {
                log.warn("Unspecific ListWrappers of type {} are not supported", o.getClass());
            }
        }
        return null;
    }

    private EmbeddedField _convertEmbeddedToField(ListWrapper.ContentModelDataListWrapper cmdWrapper, PageRequestDto pageRequestDto) throws ContentProviderException {
        return _convertEmbeddedToField(cmdWrapper.getValues(), pageRequestDto);
    }

    private EmbeddedField _convertEmbeddedToField(ContentModelData contentModelData, PageRequestDto pageRequestDto) throws ContentProviderException {
        return _convertEmbeddedToField(Collections.singletonList(contentModelData), pageRequestDto);
    }

    private EmbeddedField _convertEmbeddedToField(List<ContentModelData> cmdList, PageRequestDto pageRequestDto) throws ContentProviderException {
        EmbeddedField embeddedField = new EmbeddedField();

        List<FieldSet> fieldSets = new ArrayList<>();
        for (ContentModelData contentModelData : cmdList) {
            FieldSet fieldSet = new FieldSetImpl();
            fieldSet.setContent(_convertContent(contentModelData, pageRequestDto));
            fieldSets.add(fieldSet);
        }

        embeddedField.setEmbeddedValues(fieldSets);
        return embeddedField;
    }

    private ComponentLinkField _convertToCompLinkField(EntityModelData entityModelData, PageRequestDto pageRequestDto) throws ContentProviderException {
        ComponentLinkField linkField = new ComponentLinkField();
        Component component = _convertEntity(entityModelData, pageRequestDto);
        linkField.setLinkedComponentValues(Collections.singletonList(component));
        return linkField;
    }

    private TextField _convertToTextField(List<String> strings) {
        TextField textField = new TextField();
        textField.setTextValues(strings);
        return textField;
    }
}
