package com.sdl.dxa.modelservice.service.processing.conversion;

import com.sdl.dxa.api.datamodel.model.ContentModelData;
import com.sdl.dxa.api.datamodel.model.EntityModelData;
import com.sdl.dxa.api.datamodel.model.PageModelData;
import com.sdl.dxa.api.datamodel.model.PageTemplateData;
import com.sdl.dxa.api.datamodel.model.RegionModelData;
import com.sdl.dxa.api.datamodel.model.util.ListWrapper;
import com.sdl.dxa.common.dto.PageRequestDto;
import com.sdl.webapp.common.util.TcmUtils;
import lombok.extern.slf4j.Slf4j;
import org.dd4t.contentmodel.Component;
import org.dd4t.contentmodel.ComponentPresentation;
import org.dd4t.contentmodel.ComponentTemplate;
import org.dd4t.contentmodel.Field;
import org.dd4t.contentmodel.Page;
import org.dd4t.contentmodel.PageTemplate;
import org.dd4t.contentmodel.impl.ComponentLinkField;
import org.dd4t.contentmodel.impl.EmbeddedField;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@org.springframework.stereotype.Component
public class ToR2ConverterImpl implements ToR2Converter {

    @Override
    @Contract("!null, _ -> !null; null, _ -> null")
    public PageModelData convertToR2(@Nullable Page toConvert, @NotNull PageRequestDto pageRequestDto) {
        if (toConvert == null) {
            log.warn("Model to convert is null, returning null");
            return null;
        }

        PageModelData page = new PageModelData();

        page.setId(String.valueOf(TcmUtils.getItemId(toConvert.getId())));
        page.setTitle(toConvert.getTitle());
        page.setPageTemplate(_buildPageTemplate(toConvert.getPageTemplate()));
        // todo UrlPath
        // todo Meta

        Map<String, RegionModelData> regions = new TreeMap<>();
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
            currentRegion.getEntities().add(_buildEntity(component, componentTemplate));
        }
        page.setRegions(new ArrayList<>(regions.values()));

        // todo MvcData
        // todo XpmMetadata
        // todo Metadata
        // todo SchemaId

        return page;
    }

    private PageTemplateData _buildPageTemplate(PageTemplate pageTemplate) {
        PageTemplateData templateData = new PageTemplateData();
        templateData.setId(String.valueOf(TcmUtils.getItemId(pageTemplate.getId())));
        templateData.setTitle(pageTemplate.getTitle());
        templateData.setFileExtension(pageTemplate.getFileExtension());
        templateData.setRevisionDate(pageTemplate.getRevisionDate());
        templateData.setMetadata(_convertContent(pageTemplate.getMetadata()));
        return templateData;
    }

    private ContentModelData _convertContent(Map<String, Field> content) {
        ContentModelData data = new ContentModelData();

        for (Map.Entry<String, Field> entry : content.entrySet()) {
            String key = entry.getKey();
            Field value = entry.getValue();

            data.put(key, _convertField(value));
        }

        return data;
    }

    private Object _convertField(Field field) {
        switch (field.getFieldType()) {
            case EMBEDDED:
                return _convertEmbeddedField((EmbeddedField) field);
            case COMPONENTLINK:
                return _convertComponentLink((ComponentLinkField) field);
            default:
                return _convertNotSpecificField(field);
        }
    }

    private Object _convertEmbeddedField(EmbeddedField field) {
        return _convertField(field,

                () -> _convertContent(field.getEmbeddedValues().get(0).getContent()),

                () -> new ListWrapper.ContentModelDataListWrapper(
                        field.getEmbeddedValues().stream()
                                .map(fieldSet -> _convertContent(fieldSet.getContent()))
                                .collect(Collectors.toList())));
    }

    private Object _convertComponentLink(ComponentLinkField linkField) {
        return _convertField(linkField,

                () -> _buildEntity(linkField.getLinkedComponentValues().get(0), null),

                () -> new ListWrapper.EntityModelDataListWrapper(
                        linkField.getLinkedComponentValues().stream()
                                .map(component -> _buildEntity(component, null))
                                .collect(Collectors.toList())));
    }

    private EntityModelData _buildEntity(Component component, @Nullable ComponentTemplate componentTemplate) {
        EntityModelData entity = new EntityModelData();
        entity.setId(String.valueOf(TcmUtils.getItemId(component.getId())));
        entity.setContent(_convertContent(component.getContent()));
        if (componentTemplate != null) {
            // if CT is null, then we have a DCP and thus no component template
            entity.setComponentTemplateId(String.valueOf(TcmUtils.getItemId(componentTemplate.getId())));
        }
        // todo schema id
        return entity;
    }

    private Object _convertNotSpecificField(Field field) {
        return _convertField(field,

                () -> String.valueOf(field.getValues().get(0)),

                () -> new ListWrapper<>(field.getValues().stream()
                        .map(String::valueOf)
                        .collect(Collectors.toList())));
    }

    private Object _convertField(Field field, Supplier<Object> singleValue, Supplier<Object> multipleValues) {
        return field.getValues().size() == 1 ? singleValue.get() : multipleValues.get();
    }

}
