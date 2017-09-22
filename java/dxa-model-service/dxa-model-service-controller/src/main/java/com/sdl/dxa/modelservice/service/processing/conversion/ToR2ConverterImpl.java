package com.sdl.dxa.modelservice.service.processing.conversion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdl.dxa.api.datamodel.model.BinaryContentData;
import com.sdl.dxa.api.datamodel.model.ComponentTemplateData;
import com.sdl.dxa.api.datamodel.model.ContentModelData;
import com.sdl.dxa.api.datamodel.model.EntityModelData;
import com.sdl.dxa.api.datamodel.model.ExternalContentData;
import com.sdl.dxa.api.datamodel.model.KeywordModelData;
import com.sdl.dxa.api.datamodel.model.MvcModelData;
import com.sdl.dxa.api.datamodel.model.PageModelData;
import com.sdl.dxa.api.datamodel.model.PageTemplateData;
import com.sdl.dxa.api.datamodel.model.RegionModelData;
import com.sdl.dxa.api.datamodel.model.RichTextData;
import com.sdl.dxa.api.datamodel.model.util.ListWrapper;
import com.sdl.dxa.common.dto.DataModelType;
import com.sdl.dxa.common.dto.EntityRequestDto;
import com.sdl.dxa.common.dto.PageRequestDto;
import com.sdl.dxa.common.util.MvcUtils;
import com.sdl.dxa.common.util.PathUtils;
import com.sdl.dxa.modelservice.service.ContentService;
import com.sdl.dxa.modelservice.service.LegacyEntityModelService;
import com.sdl.webapp.common.api.content.ContentProviderException;
import com.sdl.webapp.common.util.TcmUtils;
import com.sdl.webapp.common.util.XpmUtils;
import com.tridion.meta.PageMeta;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
import org.dd4t.contentmodel.Schema;
import org.dd4t.contentmodel.impl.BaseField;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.sdl.dxa.modelservice.service.ContentService.getModelType;

@Slf4j
@org.springframework.stereotype.Component
public class ToR2ConverterImpl implements ToR2Converter {

    private static final String IMAGE_FIELD_NAME = "image";

    private static final String REGION_FOR_PAGE_TITLE_COMPONENT = "Main";

    private static final String STANDARD_METADATA_FIELD_NAME = "standardMeta";

    private static final String STANDARD_METADATA_TITLE_FIELD_NAME = "name";

    private static final String STANDARD_METADATA_DESCRIPTION_FIELD_NAME = "description";

    private static final String COMPONENT_PAGE_TITLE_FIELD_NAME = "headline";

    private final ContentService contentService;

    private final MetadataService metadataService;

    private final ObjectMapper objectMapper;

    private LegacyEntityModelService legacyEntityModelService;

    @Autowired
    public ToR2ConverterImpl(ContentService contentService,
                             @Qualifier("dxaR2ObjectMapper") ObjectMapper objectMapper,
                             MetadataService metadataService) {
        this.contentService = contentService;
        this.objectMapper = objectMapper;
        this.metadataService = metadataService;
    }

    @Nullable
    private static String _getValueFromFieldSet(FieldSet fieldSet, String fieldName) {
        Map<String, Field> content = fieldSet != null ? fieldSet.getContent() : null;
        if (content != null) {
            Field field = content.get(fieldName);
            if (field != null) {
                return Objects.toString(field.getValues().get(0));
            }
        }
        return null;
    }

    private static String _extract(Map<String, Field> metaMap, String key) {
        return metaMap.containsKey(key) ? metaMap.get(key).getValues().get(0).toString() : null;
    }

    @Nullable
    private static String _getRegionName(@NotNull Map<String, Field> templateMeta) {
        String name = templateMeta.containsKey("regionName") ? templateMeta.get("regionName").getValues().get(0).toString() : "";
        if(name == null || StringUtils.isEmpty(name)) {
            name = _getRegionView(templateMeta);
        }

        return name;
    }

    @Nullable
    private static String _getRegionView(@NotNull Map<String, Field> templateMeta) {
        return templateMeta.containsKey("regionView") ? templateMeta.get("regionView").getValues().get(0).toString() : "Main";
    }

    @Autowired
    public void setEntityModelService(LegacyEntityModelService legacyEntityModelService) {
        this.legacyEntityModelService = legacyEntityModelService;
    }

    private String _createDcpId(String componentId, String componentTemplateId) {
        return String.format("%s-%s", String.valueOf(TcmUtils.getItemId(componentId)), String.valueOf(TcmUtils.getItemId(componentTemplateId)));
    }

    private String _extractPageTitle(Page page, Map<String, String> meta, int publicationId) {
        String title = meta.get("title");
        if (isNullOrEmpty(title)) {
            for (ComponentPresentation cp : page.getComponentPresentations()) {
                if (Objects.equals(REGION_FOR_PAGE_TITLE_COMPONENT, _getRegionView(cp.getComponentTemplate().getMetadata()))) {
                    Component component = cp.getComponent();
                    ComponentTemplate componentTemplate = cp.getComponentTemplate();

                    if (cp.isDynamic()) {
                        String dcpId = _createDcpId(component.getId(), componentTemplate.getId());
                        try {
                            cp = legacyEntityModelService.loadLegacyEntityModel(EntityRequestDto.builder(publicationId, dcpId).build());
                            component = cp.getComponent();
                        } catch (ContentProviderException e) {
                            log.warn("Could not load dynamic component presentation with id {}.", dcpId, e);
                            continue;
                        }
                    }

                    final Map<String, Field> metadata = component.getMetadata();
                    BaseField standardMetaField = (BaseField) metadata.get(STANDARD_METADATA_FIELD_NAME);
                    if (standardMetaField != null && !standardMetaField.getEmbeddedValues().isEmpty()) {
                        final Map<String, Field> standardMeta = standardMetaField.getEmbeddedValues().get(0).getContent();
                        if (isNullOrEmpty(title) && standardMeta.containsKey(STANDARD_METADATA_TITLE_FIELD_NAME)) {
                            title = _extract(standardMeta, STANDARD_METADATA_TITLE_FIELD_NAME);
                        }
                    }

                    final Map<String, Field> content = component.getContent();
                    if (isNullOrEmpty(title) && content.containsKey(COMPONENT_PAGE_TITLE_FIELD_NAME)) {
                        title = _extract(content, COMPONENT_PAGE_TITLE_FIELD_NAME);
                    }

                    break;
                }
            }
        }

        // Use page title if no title found
        if (isNullOrEmpty(title)) {
            title = page.getTitle();
            if (title.equalsIgnoreCase("index") || title.equalsIgnoreCase("default")) {
                // Use default page title from configuration if nothing better was found
                title = "defaultPageTitle";
            }
        }

        return PathUtils.removeSequenceFromPageTitle(title);
    }

    private Map<String, String> _processPageMeta(org.dd4t.contentmodel.Page page, int publicationId) {
        Map<String, String> meta = new HashMap<>();

        String description = meta.get("description");
        String image = meta.get(IMAGE_FIELD_NAME);

        if (isNullOrEmpty(image) || isNullOrEmpty(description)) {
            for (ComponentPresentation cp : page.getComponentPresentations()) {
                if (Objects.equals(REGION_FOR_PAGE_TITLE_COMPONENT, _getRegionName(cp.getComponentTemplate().getMetadata()))) {
                    final org.dd4t.contentmodel.Component component = cp.getComponent();

                    final Map<String, Field> metadata = component.getMetadata();
                    BaseField standardMetaField = (BaseField) metadata.get(STANDARD_METADATA_FIELD_NAME);
                    if (standardMetaField != null && !standardMetaField.getEmbeddedValues().isEmpty()) {
                        final Map<String, Field> standardMeta = standardMetaField.getEmbeddedValues().get(0).getContent();
                        if (isNullOrEmpty(description) && standardMeta.containsKey(STANDARD_METADATA_DESCRIPTION_FIELD_NAME)) {
                            description = _extract(standardMeta, STANDARD_METADATA_DESCRIPTION_FIELD_NAME);
                        }
                    }

                    final Map<String, Field> content = component.getContent();
                    if (isNullOrEmpty(image) && content.containsKey(IMAGE_FIELD_NAME)) {
                        image = ((BaseField) content.get(IMAGE_FIELD_NAME))
                                .getLinkedComponentValues().get(0).getMultimedia().getUrl();
                    }
                    break;
                }
            }
        }


        String title = _extractPageTitle(page, meta, publicationId);
        meta.put("twitter:card", "summary");
        meta.put("og:title", title);
        meta.put("og:type", "article");

        if (!isNullOrEmpty(description)) {
            meta.put("og:description", description);
        }

        if (!isNullOrEmpty(image)) {
            meta.put("og:image", image);
        }

        if (!meta.containsKey("description")) {
            meta.put("description", !isNullOrEmpty(description) ? description : title);
        }

        return meta;
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
        page.setPageTemplate(_buildPageTemplate(toConvert.getPageTemplate(), pageRequest.getPublicationId()));
        page.setUrlPath(PathUtils.stripDefaultExtension(metadataService.getPageMeta(pageRequest.getPublicationId(), toConvert.getId()).getURLPath()));
        page.setStructureGroupId(String.valueOf(TcmUtils.getItemId(toConvert.getStructureGroup().getId())));

        Schema rootSchema = toConvert.getSchema();
        if (rootSchema != null) {
            page.setSchemaId(String.valueOf(TcmUtils.getItemId(toConvert.getSchema().getId())));
        }

        final Map<String, String> pageMeta = _processPageMeta(toConvert, pageRequest.getPublicationId());
        page.setMeta(pageMeta);
        page.setTitle(_extractPageTitle(toConvert, pageMeta, pageRequest.getPublicationId()));


        PageTemplate  pageTemplate = toConvert.getPageTemplate();
        Map<String, RegionModelData> regions = _getOrderedRegions(pageTemplate.getMetadata(), pageRequest.getPublicationId());
        for (ComponentPresentation componentPresentation : toConvert.getComponentPresentations()) {
            Component component = componentPresentation.getComponent();

            Map<String, Field> metadata = componentPresentation.getComponentTemplate().getMetadata();
            String regionName = _getRegionName(metadata);
            if (!regions.containsKey(regionName)) {
                regions.put(regionName, new RegionModelData(regionName, null, null, null));
            }
            RegionModelData currentRegion = regions.get(regionName);

            if (currentRegion.getEntities() == null) {
                currentRegion.setEntities(new ArrayList<>());
            }
            currentRegion.setMvcData(MvcUtils.parseMvcQualifiedViewName(_getRegionView(metadata)));
            currentRegion.getEntities().add(_buildEntity(component, componentPresentation, pageRequest.getPublicationId()));
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

        page.setMetadata(_convertContent(toConvert.getMetadata(), pageRequest.getPublicationId()));

        return page;
    }

    private Map<String, RegionModelData> _getOrderedRegions(Map<String, Field> metadata, int publicationId) throws ContentProviderException {
        String key = "regions";
        Map<String, RegionModelData> list = new LinkedHashMap<>();
        if (!metadata.containsKey(key)) {
            return list;
        }

        ListWrapper<ContentModelData> field = (ListWrapper<ContentModelData>) _convertField(metadata.get(key), publicationId);
        for(ContentModelData r : field.getValues()) {
            String name = (String) r.get("name");
            if(name == null || StringUtils.isEmpty(name)) {
                name = (String) r.get("view");
            }

            RegionModelData rmd = new RegionModelData(name, null, null, null);

            rmd.setMetadata(r);
            rmd.setMvcData(MvcUtils.parseMvcQualifiedViewName(name));
            rmd.setEntities(new ArrayList<>());

            list.put(name, rmd);
        }
        return list;
    }

    @Override
    public EntityModelData convertToR2(@Nullable ComponentPresentation toConvert, @NotNull EntityRequestDto entityRequestDto) throws ContentProviderException {
        if (toConvert == null) {
            log.warn("Model to convert is null, return null for request {}", entityRequestDto);
            return null;
        }

        return _buildEntity(toConvert.getComponent(), toConvert, entityRequestDto.getPublicationId());
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
        Object includes = pageTemplate.getMetadata().get(includesKey);
        for (String include : _processIncludes(includes).getValues()) {
            list.add(_loadInclude(include, pageRequest));
        }
        return list;
    }

    private RegionModelData _loadInclude(String include, PageRequestDto pageRequest) throws ContentProviderException {
        String includeUrl = PathUtils.combinePath(metadataService.getPublicationMeta(pageRequest.getPublicationId()).getPublicationUrl(), include);
        try {
            String content = contentService.loadPageContent(pageRequest.toBuilder().path(includeUrl).build());
            DataModelType publishedModelType = getModelType(content);
            if (publishedModelType == DataModelType.DD4T) {
                return _convertDD4TPageToRegion(objectMapper.readTree(content), pageRequest);
            } else if (publishedModelType == DataModelType.R2) {
                return _convertR2PageToRegion(objectMapper.readTree(content), pageRequest);
            }

            throw new ContentProviderException("Content is published of an unexpected content type, " +
                    "we don't support it, page request = " + pageRequest + ", content = " + content);
        } catch (IOException e) {
            throw new ContentProviderException("Error parsing include page content, request = " + pageRequest, e);
        }
    }

    private RegionModelData _convertR2PageToRegion(JsonNode tree, PageRequestDto pageRequest) {
        String title = tree.get("Title").asText();

        return _createPageRegionData(
                tree.get("Id").asText(),
                title,
                tree.get("UrlPath").asText(""),
                pageRequest);
    }

    private RegionModelData _convertDD4TPageToRegion(JsonNode tree, PageRequestDto pageRequest) {
        String id = tree.get("Id").asText();
        String name = tree.get("Title").asText();

        PageMeta pageMeta = metadataService.getPageMeta(pageRequest.getPublicationId(),
                TcmUtils.buildPageTcmUri(pageRequest.getPublicationId(), String.valueOf(TcmUtils.getItemId(id))));

        return _createPageRegionData(
                String.valueOf(TcmUtils.getItemId(id)),
                name,
                PathUtils.getFileName(pageMeta.getPath()),
                pageRequest);
    }

    private RegionModelData _createPageRegionData(String id, String name, String path, PageRequestDto pageRequest) {
        RegionModelData region = new RegionModelData(name, id, null, null);

        region.setMvcData(MvcUtils.parseMvcQualifiedViewName(name, false));
        region.setXpmMetadata(new XpmUtils.RegionXpmBuilder()
                .setIncludedFromPageID(TcmUtils.buildPageTcmUri(pageRequest.getPublicationId(), id))
                .setIncludedFromPageTitle(name)
                .setIncludedFromPageFileName(path)
                .buildXpm());

        return region;
    }

    private ListWrapper<String> _processIncludes(Object includes) throws ContentProviderException {
        ArrayList<String> list = new ArrayList<>();
        if (includes instanceof ListWrapper) {
            return (ListWrapper<String>) includes;
        } else if (includes instanceof String) {
            list.add(String.valueOf(includes));
        }

        return new ListWrapper<>(list);
    }

    private PageTemplateData _buildPageTemplate(PageTemplate pageTemplate, int publicationId) throws ContentProviderException {
        PageTemplateData templateData = new PageTemplateData();
        templateData.setId(String.valueOf(TcmUtils.getItemId(pageTemplate.getId())));
        templateData.setTitle(pageTemplate.getTitle());
        templateData.setFileExtension(pageTemplate.getFileExtension());
        templateData.setRevisionDate(pageTemplate.getRevisionDate());
        templateData.setMetadata(_convertContent(pageTemplate.getMetadata(), publicationId));
        return templateData;
    }

    @Nullable
    private ContentModelData _convertContent(Map<String, Field> content, int publicationId) throws ContentProviderException {
        if (content == null || content.isEmpty()) {
            return null;
        }

        ContentModelData data = new ContentModelData();
        for (Map.Entry<String, Field> entry : content.entrySet()) {
            String key = entry.getKey();
            Field value = entry.getValue();

            data.put(key, _convertField(value, publicationId));
        }
        return data;
    }

    private ExternalContentData _convertExternalContent(Component component, int publicationId) throws ContentProviderException {
        if (component == null || component.getExtensionData() == null) {
            return null;
        }

        Map<String, FieldSet> extensionData = component.getExtensionData();

        FieldSet externalData = extensionData.get("ECL");
        FieldSet externalMetadata = extensionData.get("ECL-ExternalMetadata");
        ContentModelData metadata = null;
        if (externalMetadata != null) {
            metadata = _convertContent(externalMetadata.getContent(), publicationId);
        }
        return new ExternalContentData(
                _getValueFromFieldSet(externalData, "DisplayTypeId"),
                component.getEclId(),
                _getValueFromFieldSet(externalData, "TemplateFragment"),
                metadata
        );
    }

    private BinaryContentData _convertMultimediaContent(Multimedia content) {
        if (content == null) {
            return null;
        }
        return new BinaryContentData(content.getFileName(), content.getSize(), content.getMimeType(), content.getUrl());
    }

    private Object _convertField(Field field, int publicationId) throws ContentProviderException {
        switch (field.getFieldType()) {
            case EMBEDDED:
                return _convertEmbeddedField((EmbeddedField) field, publicationId);
            case COMPONENTLINK:
            case MULTIMEDIALINK:
                return _convertComponentLink((ComponentLinkField) field, publicationId);
            case KEYWORD:
                return _convertKeyword((KeywordField) field, publicationId);
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

    private Object _convertKeyword(KeywordField field, int publicationId) throws ContentProviderException {
        return _convertField(field, new SingleOrMultipleFork() {
            @Override
            public Object onSingleValue() throws ContentProviderException {
                return _convertKeyword(field.getKeywordValues().get(0), publicationId);
            }

            @Override
            public Object onMultipleValues() throws ContentProviderException {
                List<KeywordModelData> list = new ArrayList<>();
                for (Keyword keyword : field.getKeywordValues()) {
                    KeywordModelData keywordModelData = _convertKeyword(keyword, publicationId);
                    list.add(keywordModelData);
                }
                return new ListWrapper.KeywordModelDataListWrapper(list);
            }
        });

    }

    private KeywordModelData _convertKeyword(Keyword keyword, int publicationId) throws ContentProviderException {
        KeywordModelData data = new KeywordModelData(
                String.valueOf(TcmUtils.getItemId(keyword.getId())),
                keyword.getDescription(),
                keyword.getKey(),
                String.valueOf(TcmUtils.getItemId(keyword.getTaxonomyId())),
                keyword.getTitle());

        Map<String, FieldSet> extensionData = keyword.getExtensionData();
        if (extensionData != null && extensionData.get("DXA") != null) {
            String schemaUri = _getValueFromFieldSet(extensionData.get("DXA"), "MetadataSchemaId");
            data.setSchemaId(String.valueOf(TcmUtils.getItemId(schemaUri)));
        }

        Map<String, Field> metadata = keyword.getMetadata();
        if (metadata != null) {
            data.setMetadata(_convertContent(metadata, publicationId));
        }
        return data;
    }

    private Object _convertEmbeddedField(EmbeddedField field, int publicationId) throws ContentProviderException {
        return _convertField(field,
                new SingleOrMultipleFork() {
                    @Override
                    public Object onSingleValue() throws ContentProviderException {
                        return _convertContent(field.getEmbeddedValues().get(0).getContent(), publicationId);
                    }

                    @Override
                    public Object onMultipleValues() throws ContentProviderException {
                        List<ContentModelData> list = new ArrayList<>();
                        for (FieldSet fieldSet : field.getEmbeddedValues()) {
                            list.add(_convertContent(fieldSet.getContent(), publicationId));
                        }
                        return new ListWrapper.ContentModelDataListWrapper(list);
                    }
                });
    }

    private Object _convertComponentLink(ComponentLinkField linkField, int publicationId) throws ContentProviderException {
        return _convertField(linkField,
                new SingleOrMultipleFork() {
                    @Override
                    public Object onSingleValue() throws ContentProviderException {
                        return _buildEntity(linkField.getLinkedComponentValues().get(0), null, publicationId);
                    }

                    @Override
                    public Object onMultipleValues() throws ContentProviderException {
                        List<EntityModelData> list = new ArrayList<>();
                        for (Component component : linkField.getLinkedComponentValues()) {
                            list.add(_buildEntity(component, null, publicationId));
                        }
                        return new ListWrapper.EntityModelDataListWrapper(list);
                    }
                });
    }

    private EntityModelData _buildEntity(@NotNull Component component, @Nullable ComponentPresentation componentPresentation, int publicationId) throws ContentProviderException {
        Component _component = component;
        ComponentPresentation _componentPresentation = componentPresentation;

        EntityModelData entity = new EntityModelData();
        int componentId = TcmUtils.getItemId(_component.getId());
        entity.setId(String.valueOf(componentId));

        if (_component.getComponentType() == Component.ComponentType.MULTIMEDIA) {
            entity.setBinaryContent(_convertMultimediaContent(_component.getMultimedia()));
            if (StringUtils.isNotEmpty(_component.getEclId())) {
                entity.setExternalContent(_convertExternalContent(_component, publicationId));
            }
        }

        if (_componentPresentation != null) {
            ComponentTemplate componentTemplate = _componentPresentation.getComponentTemplate();

            if (_componentPresentation.isDynamic()) {
                String dcpId = _createDcpId(_component.getId(), componentTemplate.getId());
                _componentPresentation = this.legacyEntityModelService.loadLegacyEntityModel(EntityRequestDto.builder(publicationId, dcpId).build());
                _component = _componentPresentation.getComponent();
                componentTemplate = _componentPresentation.getComponentTemplate();
                entity.setId(dcpId);
            }
            if (_componentPresentation.getExtensionData() != null && !_componentPresentation.getExtensionData().isEmpty()) {
                entity.setExtensionData(_convertExtensionData(_componentPresentation.getExtensionData(), publicationId));
            }

            if (componentTemplate != null) {
                // if CT is null, then we have a DCP and thus no component template
                entity.setMvcData(_getMvcModelData(componentTemplate.getMetadata()));

                ComponentTemplateData templateData = new ComponentTemplateData();
                templateData.setId(String.valueOf(TcmUtils.getItemId(componentTemplate.getId())));
                templateData.setTitle(componentTemplate.getTitle());
                templateData.setRevisionDate(componentTemplate.getRevisionDate());
                templateData.setMetadata(_convertContent(componentTemplate.getMetadata(), publicationId));
                if (componentTemplate instanceof ComponentTemplateImpl) {
                    templateData.setOutputFormat(((ComponentTemplateImpl) componentTemplate).getOutputFormat());
                }
                entity.setComponentTemplate(templateData);

                entity.setXpmMetadata(new XpmUtils.EntityXpmBuilder()
                        .setComponentId(_component.getId())
                        .setComponentModified(_component.getRevisionDate())
                        .setComponentTemplateID(componentTemplate.getId())
                        .setComponentTemplateModified(componentTemplate.getRevisionDate())
                        .setRepositoryPublished(_componentPresentation.isDynamic())
                        .buildXpm());
            }
        }

        entity.setMetadata(_convertContent(_component.getMetadata(), publicationId));
        entity.setContent(_convertContent(_component.getContent(), publicationId));
        entity.setSchemaId(String.valueOf(TcmUtils.getItemId(_component.getSchema().getId())));
        return entity;
    }

    private Map<String, Object> _convertExtensionData(Map<String, FieldSet> data, int publicationId) throws ContentProviderException {
        Map<String, Object> result = new HashMap<>();

        for (Map.Entry<String, FieldSet> entry : data.entrySet()) {
            String entryKey = entry.getKey();
            FieldSet entryValue = entry.getValue();
            Map<String, Field> c = entryValue.getContent();
            result.put(entryKey, _convertContent(c, publicationId));
        }

        return result;
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
