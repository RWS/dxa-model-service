package com.sdl.dxa.modelservice.service.processing.conversion;

import com.fasterxml.jackson.databind.JsonNode;
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
import com.sdl.webapp.common.api.content.ContentProviderException;
import com.sdl.webapp.common.util.TcmUtils;
import lombok.extern.slf4j.Slf4j;
import org.dd4t.contentmodel.Component;
import org.dd4t.contentmodel.ComponentPresentation;
import org.dd4t.contentmodel.ComponentTemplate;
import org.dd4t.contentmodel.Field;
import org.dd4t.contentmodel.FieldSet;
import org.dd4t.contentmodel.Page;
import org.dd4t.contentmodel.PageTemplate;
import org.dd4t.contentmodel.impl.ComponentLinkField;
import org.dd4t.contentmodel.impl.EmbeddedField;
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

    @Autowired
    public ToR2ConverterImpl(ConfigService configService,
                             ContentService contentService,
                             @Qualifier("dxaR2ObjectMapper") ObjectMapper objectMapper, MetadataService metadataService) {
        this.configService = configService;
        this.contentService = contentService;
        this.objectMapper = objectMapper;
        this.metadataService = metadataService;
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
            currentRegion.getEntities().add(_buildEntity(component, componentTemplate, pageRequest));
        }
        for (RegionModelData regionModelData : _loadIncludes(page.getPageTemplate(), pageRequest)) {
            regions.put(regionModelData.getName(), regionModelData);
        }
        page.setRegions(new ArrayList<>(regions.values()));

        // todo MvcData
        // todo XpmMetadata

        page.setMetadata(_convertContent(toConvert.getMetadata(), pageRequest));

        // todo SchemaId ???

        return page;
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
            String includeUrl = metadataService.getPublicationMeta(pageRequest.getPublicationId()).getPublicationUrl() + "/" + include;
            try {
                JsonNode tree = objectMapper.readTree(contentService.loadPageContent(pageRequest.toBuilder().path(includeUrl).build()));
                String id = tree.has("Id") ? String.valueOf(TcmUtils.getItemId(tree.get("Id").asText())) : tree.get("IncludePageId").asText();
                String name = (tree.has("Title") ? tree.get("Title") : tree.get("Name")).asText();
                list.add(new RegionModelData(name, id, null, null));
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

    private ContentModelData _convertContent(Map<String, Field> content, PageRequestDto pageRequest) throws ContentProviderException {
        ContentModelData data = new ContentModelData();

        for (Map.Entry<String, Field> entry : content.entrySet()) {
            String key = entry.getKey();
            Field value = entry.getValue();

            data.put(key, _convertField(value, pageRequest));
        }

        return data;
    }

    private Object _convertField(Field field, PageRequestDto pageRequest) throws ContentProviderException {
        switch (field.getFieldType()) {
            case EMBEDDED:
                return _convertEmbeddedField((EmbeddedField) field, pageRequest);
            case COMPONENTLINK:
                return _convertComponentLink((ComponentLinkField) field, pageRequest);
            // todo support other types
            default:
                return _convertNotSpecificField(field);
        }
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

    private EntityModelData _buildEntity(Component component, @Nullable ComponentTemplate componentTemplate, PageRequestDto pageRequest) throws ContentProviderException {
        EntityModelData entity = new EntityModelData();
        entity.setId(String.valueOf(TcmUtils.getItemId(component.getId())));
        entity.setContent(_convertContent(component.getContent(), pageRequest));
        if (componentTemplate != null) {
            // if CT is null, then we have a DCP and thus no component template
            entity.setComponentTemplateId(String.valueOf(TcmUtils.getItemId(componentTemplate.getId())));
        }

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
