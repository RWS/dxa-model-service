package com.sdl.dxa.modelservice.service.processing.conversion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdl.dxa.api.datamodel.model.BinaryContentData;
import com.sdl.dxa.api.datamodel.model.ComponentTemplateData;
import com.sdl.dxa.api.datamodel.model.ContentModelData;
import com.sdl.dxa.api.datamodel.model.EntityModelData;
import com.sdl.dxa.api.datamodel.model.KeywordModelData;
import com.sdl.dxa.api.datamodel.model.MvcModelData;
import com.sdl.dxa.api.datamodel.model.PageModelData;
import com.sdl.dxa.api.datamodel.model.PageTemplateData;
import com.sdl.dxa.api.datamodel.model.RegionModelData;
import com.sdl.dxa.api.datamodel.model.RichTextData;
import com.sdl.dxa.api.datamodel.model.util.ListWrapper;
import com.sdl.dxa.common.dto.EntityRequestDto;
import com.sdl.dxa.common.dto.PageRequestDto;
import com.sdl.dxa.common.util.MvcUtils;
import com.sdl.dxa.common.util.PathUtils;
import com.sdl.dxa.modelservice.service.ConfigService;
import com.sdl.dxa.modelservice.service.ContentService;
import com.sdl.dxa.modelservice.service.LegacyEntityModelService;
import com.sdl.dxa.modelservice.service.processing.conversion.models.LightSchema;
import com.sdl.webapp.common.api.content.ContentProviderException;
import com.sdl.webapp.common.util.TcmUtils;
import com.sdl.webapp.common.util.XpmUtils;
import com.tridion.meta.PageMeta;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.dd4t.contentmodel.Component;
import org.dd4t.contentmodel.ComponentPresentation;
import org.dd4t.contentmodel.ComponentTemplate;
import org.dd4t.contentmodel.Field;
import org.dd4t.contentmodel.FieldSet;
import org.dd4t.contentmodel.Keyword;
import org.dd4t.contentmodel.Multimedia;
import org.dd4t.contentmodel.Page;
import org.dd4t.contentmodel.PageTemplate;
import org.dd4t.contentmodel.impl.ComponentLinkField;
import org.dd4t.contentmodel.impl.ComponentTemplateImpl;
import org.dd4t.contentmodel.impl.EmbeddedField;
import org.dd4t.contentmodel.impl.KeywordField;
import org.dd4t.contentmodel.impl.TextField;
import org.dd4t.contentmodel.impl.XhtmlField;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@org.springframework.stereotype.Component
public class ToR2ConverterImpl implements ToR2Converter {

    private final ConfigService configService;

    private final ContentService contentService;

    private final ObjectMapper objectMapper;

    private final MetadataService metadataService;

    private LegacyEntityModelService entityModelService;

    @Autowired
    public ToR2ConverterImpl(ConfigService configService,
                             ContentService contentService,
                             @Qualifier("dxaR2ObjectMapper") ObjectMapper objectMapper,
                             MetadataService metadataService) {
        this.configService = configService;
        this.contentService = contentService;
        this.objectMapper = objectMapper;
        this.metadataService = metadataService;
    }

    @Autowired
    public void setEntityModelService(LegacyEntityModelService entityModelService) {
        this.entityModelService = entityModelService;
    }

    @Override
    @Contract("!null, _ -> !null; null, _ -> null")
    public PageModelData convertToR2(@Nullable Page toConvert, @NotNull PageRequestDto pageRequest) throws ContentProviderException {
        if (toConvert == null) {
            log.warn("Model to convert is null, returning null");
            return null;
        }

        PageModelData page = new PageModelData();

        page.setId(String.valueOf(TcmUtils.getItemId(toConvert.getId())));
        page.setTitle(toConvert.getTitle());
        page.setPageTemplate(_buildPageTemplate(toConvert.getPageTemplate(), pageRequest));
        page.setUrlPath(PathUtils.stripDefaultExtension(metadataService.getPageMeta(pageRequest.getPublicationId(), toConvert.getId()).getURLPath()));
        page.setStructureGroupId(String.valueOf(TcmUtils.getItemId(toConvert.getStructureGroup().getId())));

        // todo Meta

        Map<String, RegionModelData> regions = new LinkedHashMap<>();
        for (ComponentPresentation componentPresentation : toConvert.getComponentPresentations()) {

            ComponentTemplate componentTemplate = componentPresentation.getComponentTemplate();
            Component component = componentPresentation.getComponent();

            String regionName = componentTemplate.getMetadata().containsKey("regionView") ?
                    componentTemplate.getMetadata().get("regionView").getValues().get(0).toString() : "Main";
            if (!regions.containsKey(regionName)) {
                regions.put(regionName, new RegionModelData(regionName, null, null, null));
            }
            RegionModelData currentRegion = regions.get(regionName);

            if (currentRegion.getEntities() == null) {
                currentRegion.setEntities(new ArrayList<>());
            }
            currentRegion.setMvcData(MvcUtils.parseMvcQualifiedViewName(regionName));
            currentRegion.getEntities().add(_buildEntity(component, componentPresentation, pageRequest));
        }
        for (RegionModelData regionModelData : _loadIncludes(page.getPageTemplate(), pageRequest)) {
            regions.put(regionModelData.getName(), regionModelData);
        }
        page.setRegions(new ArrayList<>(regions.values()));

        page.setMvcData(_getMvcModelData(toConvert.getPageTemplate().getMetadata()));

        page.setXpmMetadata(new XpmUtils.PageXpmBuilder()
                .setPageID(toConvert.getId())
                .setPageModified(toConvert.getRevisionDate())
                .setPageTemplateID(toConvert.getPageTemplate().getId())
                .setPageTemplateModified(toConvert.getPageTemplate().getRevisionDate())
                .buildXpm());

        page.setMetadata(_convertContent(toConvert.getMetadata(), pageRequest));

        return page;
    }

    @Override
    public EntityModelData convertToR2(@Nullable ComponentPresentation toConvert, @NotNull EntityRequestDto entityRequestDto) throws ContentProviderException {
        return _buildEntity(toConvert.getComponent(), toConvert, PageRequestDto.builder().publicationId(entityRequestDto.getPublicationId()).build());
    }

    private MvcModelData _getMvcModelData(Map<String, Field> metadata) {
        MvcModelData.MvcModelDataBuilder mvcBuilder = MvcModelData.builder();

        Pair<String, String> view = _getMvcValue(metadata, "view");
        mvcBuilder.viewName(view.getLeft()).areaName(view.getRight());

        Pair<String, String> controller = _getMvcValue(metadata, "controller");
        mvcBuilder.controllerName(controller.getLeft()).controllerAreaName(controller.getRight());

        Pair<String, String> action = _getMvcValue(metadata, "action");
        mvcBuilder.actionName(action.getLeft());

        mvcBuilder.parameters(!metadata.containsKey("routeValues") ? null :
                ((TextField) metadata.get("routeValues")).getTextValues().stream()
                        .map(this::_splitMvcValue)
                        .collect(Collectors.toMap(Pair::getRight, Pair::getLeft)));

        return mvcBuilder.build();
    }

    @NotNull
    private Pair<String, String> _getMvcValue(@NotNull Map<String, Field> metadata, String name) {
        if (!metadata.containsKey(name)) {
            return new ImmutablePair<>(null, null);
        }

        return _splitMvcValue(String.valueOf(metadata.get(name).getValues().get(0)));
    }

    @NotNull
    private Pair<String, String> _splitMvcValue(String value) {
        String[] split = value.split(":");
        // left is always something's name, right (if exists) is always area name
        if (split.length == 2) {
            return new ImmutablePair<>(split[1], split[0]);
        } else if (split.length == 1) {
            return new ImmutablePair<>(split[0], null);
        } else {
            throw new IllegalArgumentException("The given mvc value is not supported, should be 'Name' or 'Area:Name', but is: " + value);
        }
    }

    private List<RegionModelData> _loadIncludes(PageTemplateData pageTemplate, PageRequestDto pageRequest) throws ContentProviderException {
        String includesKey = "includes";
        if (!pageTemplate.getMetadata().containsKey(includesKey)) {
            return Collections.emptyList();
        }

        List<RegionModelData> list = new ArrayList<>();
        //type safe because list of includes is only expected to be a list wrapper of strings
        //noinspection unchecked
        for (String include : ((ListWrapper<String>) pageTemplate.getMetadata().get(includesKey)).getValues()) {

            String includeUrl = PathUtils.combinePath(metadataService.getPublicationMeta(pageRequest.getPublicationId()).getPublicationUrl(), include);
            try {
                JsonNode tree = objectMapper.readTree(contentService.loadPageContent(pageRequest.toBuilder().path(includeUrl).build()));
                String id = tree.has("Id") ? String.valueOf(TcmUtils.getItemId(tree.get("Id").asText())) : tree.get("IncludePageId").asText();
                String name = (tree.has("Title") ? tree.get("Title") : tree.get("Name")).asText();
                RegionModelData includeRegion = new RegionModelData(name, id, null, null);

                PageMeta pageMeta = metadataService.getPageMeta(pageRequest.getPublicationId(), TcmUtils.buildPageTcmUri(pageRequest.getPublicationId(), id));
                includeRegion.setXpmMetadata(new XpmUtils.RegionXpmBuilder()
                        .setIncludedFromPageID(TcmUtils.buildPageTcmUri(pageRequest.getPublicationId(), id))
                        .setIncludedFromPageTitle(name)
                        .setIncludedFromPageFileName(PathUtils.getFileName(pageMeta.getPath()))
                        .buildXpm());

                includeRegion.setMvcData(MvcUtils.parseMvcQualifiedViewName(name));

                list.add(includeRegion);
            } catch (IOException e) {
                throw new ContentProviderException("Error parsing include page content, request = " + pageRequest, e);
            }
        }
        return list;
    }

    private PageTemplateData _buildPageTemplate(PageTemplate pageTemplate, PageRequestDto pageRequest) throws ContentProviderException {
        PageTemplateData templateData = new PageTemplateData();
        templateData.setId(String.valueOf(TcmUtils.getItemId(pageTemplate.getId())));
        templateData.setTitle(pageTemplate.getTitle());
        templateData.setFileExtension(pageTemplate.getFileExtension());
        templateData.setRevisionDate(pageTemplate.getRevisionDate());
        templateData.setMetadata(_convertContent(pageTemplate.getMetadata(), pageRequest));
        return templateData;
    }

    @Nullable
    private ContentModelData _convertContent(Map<String, Field> content, PageRequestDto pageRequest) throws ContentProviderException {
        if (content == null || content.isEmpty()) {
            return null;
        }

        ContentModelData data = new ContentModelData();
        for (Map.Entry<String, Field> entry : content.entrySet()) {
            String key = entry.getKey();
            Field value = entry.getValue();

            data.put(key, _convertField(value, pageRequest));
        }
        return data;
    }

    @Nullable
    private BinaryContentData _convertMultimediaContent(Multimedia content, PageRequestDto pageRequest) {
        if (content == null) {
            return null;
        }
        return new BinaryContentData(content.getFileName(), content.getSize(), content.getMimeType(), content.getUrl());
    }

    private Object _convertField(Field field, PageRequestDto pageRequest) throws ContentProviderException {
        switch (field.getFieldType()) {
            case EMBEDDED:
                return _convertEmbeddedField((EmbeddedField) field, pageRequest);
            case COMPONENTLINK:
            case MULTIMEDIALINK:
                return _convertComponentLink((ComponentLinkField) field, pageRequest);
            case KEYWORD:
                return _convertKeyword((KeywordField) field);
            case XHTML:
                return _convertRichTextData((XhtmlField) field);
            default:
                return _convertNotSpecificField(field);
        }
    }

    private Object _convertRichTextData(XhtmlField field) throws ContentProviderException {
        return _convertField(field, new SingleOrMultipleFork() {
            @Override
            public Object onSingleValue() throws ContentProviderException {
                return new RichTextData(Collections.singletonList(String.valueOf(field.getTextValues().get(0))));
            }

            @Override
            public Object onMultipleValues() throws ContentProviderException {
                List<RichTextData> list = new ArrayList<>();
                for (String string : field.getTextValues()) {
                    RichTextData richTextData = new RichTextData(Collections.singletonList(string));
                    list.add(richTextData);
                }
                return new ListWrapper.RichTextDataListWrapper(list);
            }
        });
    }

    private Object _convertKeyword(KeywordField field) throws ContentProviderException {
        return _convertField(field, new SingleOrMultipleFork() {
            @Override
            public Object onSingleValue() throws ContentProviderException {
                return _convertKeyword(field.getKeywordValues().get(0));
            }

            @Override
            public Object onMultipleValues() throws ContentProviderException {
                List<KeywordModelData> list = new ArrayList<>();
                for (Keyword keyword : field.getKeywordValues()) {
                    KeywordModelData keywordModelData = _convertKeyword(keyword);
                    list.add(keywordModelData);
                }
                return new ListWrapper.KeywordModelDataListWrapper(list);
            }
        });

    }

    private KeywordModelData _convertKeyword(Keyword keyword) {
        return new KeywordModelData(
                String.valueOf(TcmUtils.getItemId(keyword.getId())),
                keyword.getDescription(),
                keyword.getKey(),
                String.valueOf(TcmUtils.getItemId(keyword.getTaxonomyId())),
                keyword.getTitle());
    }

    private Object _convertEmbeddedField(EmbeddedField field, PageRequestDto pageRequest) throws ContentProviderException {
        return _convertField(field,
                new SingleOrMultipleFork() {
                    @Override
                    public Object onSingleValue() throws ContentProviderException {
                        return _convertContent(field.getEmbeddedValues().get(0).getContent(), pageRequest);
                    }

                    @Override
                    public Object onMultipleValues() throws ContentProviderException {
                        List<ContentModelData> list = new ArrayList<>();
                        for (FieldSet fieldSet : field.getEmbeddedValues()) {
                            list.add(_convertContent(fieldSet.getContent(), pageRequest));
                        }
                        return new ListWrapper.ContentModelDataListWrapper(list);
                    }
                });
    }

    private Object _convertComponentLink(ComponentLinkField linkField, PageRequestDto pageRequest) throws ContentProviderException {
        return _convertField(linkField,
                new SingleOrMultipleFork() {
                    @Override
                    public Object onSingleValue() throws ContentProviderException {
                        return _buildEntity(linkField.getLinkedComponentValues().get(0), null, pageRequest);
                    }

                    @Override
                    public Object onMultipleValues() throws ContentProviderException {
                        List<EntityModelData> list = new ArrayList<>();
                        for (Component component : linkField.getLinkedComponentValues()) {
                            list.add(_buildEntity(component, null, pageRequest));
                        }
                        return new ListWrapper.EntityModelDataListWrapper(list);
                    }
                });
    }

    private EntityModelData _buildEntity(Component component, @Nullable ComponentPresentation componentPresentation, PageRequestDto pageRequest) throws ContentProviderException {
        EntityModelData entity = new EntityModelData();
        int componentId = TcmUtils.getItemId(component.getId());
        entity.setId(String.valueOf(componentId));

        if(component != null && component.getComponentType() == Component.ComponentType.MULTIMEDIA) {
            entity.setBinaryContent(_convertMultimediaContent(component.getMultimedia(), pageRequest));
        }

        if (componentPresentation != null) {
            ComponentTemplate componentTemplate = componentPresentation.getComponentTemplate();
            if(componentPresentation.isDynamic()) {
                String dcpId = String.valueOf(componentId).concat("-").concat(String.valueOf(TcmUtils.getItemId(componentTemplate.getId())));
                EntityRequestDto req = EntityRequestDto.builder()
                        .publicationId(pageRequest.getPublicationId())
                        .entityId(dcpId)
                        .build();
                componentPresentation = this.entityModelService.loadLegacyEntityModel(req);
                component = componentPresentation.getComponent();
                componentTemplate = componentPresentation.getComponentTemplate();
                entity.setId(dcpId);
            }

            if (componentTemplate != null) {
                // if CT is null, then we have a DCP and thus no component template
                entity.setMvcData(_getMvcModelData(componentTemplate.getMetadata()));

                ComponentTemplateData templateData = new ComponentTemplateData();
                templateData.setId(String.valueOf(TcmUtils.getItemId(componentTemplate.getId())));
                templateData.setTitle(componentTemplate.getTitle());
                templateData.setRevisionDate(componentTemplate.getRevisionDate());
                templateData.setMetadata(_convertContent(componentTemplate.getMetadata(), pageRequest));
                if (componentTemplate instanceof ComponentTemplateImpl) {
                    templateData.setOutputFormat(((ComponentTemplateImpl) componentTemplate).getOutputFormat());
                }
                entity.setComponentTemplate(templateData);

                entity.setXpmMetadata(new XpmUtils.EntityXpmBuilder()
                        .setComponentId(component.getId())
                        .setComponentModified(component.getRevisionDate())
                        .setComponentTemplateID(componentTemplate.getId())
                        .setComponentTemplateModified(componentTemplate.getRevisionDate())
                        .setRepositoryPublished(componentPresentation.isDynamic())
                        .buildXpm());
            }
        }

        entity.setMetadata(_convertContent(component.getMetadata(), pageRequest));
        entity.setContent(_convertContent(component.getContent(), pageRequest));
        LightSchema lightSchema = configService.getDefaults().getSchemasJson(pageRequest.getPublicationId())
                .get(String.valueOf(TcmUtils.getItemId(component.getSchema().getId())));
        entity.setSchemaId(lightSchema.getId());

        return entity;
    }

    private Object _convertNotSpecificField(Field field) throws ContentProviderException {
        return _convertField(field, new SingleOrMultipleFork() {
            @Override
            public Object onSingleValue() {
                return String.valueOf(field.getValues().get(0));
            }

            @Override
            public Object onMultipleValues() {
                return new ListWrapper<>(field.getValues().stream()
                        .map(String::valueOf)
                        .collect(Collectors.toList()));
            }
        });
    }

    private Object _convertField(Field field, @NotNull SingleOrMultipleFork fork) throws ContentProviderException {
        return field.getValues().size() == 1 ? fork.onSingleValue() : fork.onMultipleValues();
    }

    private interface SingleOrMultipleFork {

        Object onSingleValue() throws ContentProviderException;

        Object onMultipleValues() throws ContentProviderException;
    }

}
