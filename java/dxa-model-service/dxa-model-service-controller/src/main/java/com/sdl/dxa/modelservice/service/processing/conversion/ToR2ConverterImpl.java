package com.sdl.dxa.modelservice.service.processing.conversion;

import com.sdl.dxa.api.datamodel.model.ContentModelData;
import com.sdl.dxa.api.datamodel.model.EntityModelData;
import com.sdl.dxa.api.datamodel.model.PageModelData;
import com.sdl.dxa.api.datamodel.model.RegionModelData;
import com.sdl.dxa.api.datamodel.model.util.ListWrapper;
import com.sdl.dxa.common.dto.PageRequestDto;
import com.sdl.webapp.common.util.TcmUtils;
import lombok.extern.slf4j.Slf4j;
import org.dd4t.contentmodel.Component;
import org.dd4t.contentmodel.ComponentPresentation;
import org.dd4t.contentmodel.ComponentTemplate;
import org.dd4t.contentmodel.Field;
import org.dd4t.contentmodel.FieldType;
import org.dd4t.contentmodel.Page;
import org.dd4t.contentmodel.impl.EmbeddedField;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
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
        //todo UrlPath?


        Map<String, RegionModelData> regions = new HashMap<>();
        for (ComponentPresentation componentPresentation : toConvert.getComponentPresentations()) {
            ComponentTemplate componentTemplate = componentPresentation.getComponentTemplate();
            Component component = componentPresentation.getComponent();

            EntityModelData entity = new EntityModelData();
            entity.setId(String.valueOf(TcmUtils.getItemId(component.getId())));

            entity.setContent(_convertContent(component.getContent()));

            String regionName = componentTemplate.getMetadata().containsKey("regionView") ?
                    componentTemplate.getMetadata().get("regionView").getValues().get(0).toString() : "Main";

            RegionModelData currentRegion = regions.containsKey(regionName) ? regions.get(regionName) : new RegionModelData();
            currentRegion.getEntities().add(entity);
        }

        return page;
    }

    private ContentModelData _convertContent(Map<String, Field> content) {
        ContentModelData data = new ContentModelData();
        for (Map.Entry<String, Field> entry : content.entrySet()) {
            String key = entry.getKey();
            Field value = entry.getValue();
            FieldType fieldType = value.getFieldType();
            switch (fieldType) {
                case EMBEDDED:
                    if (value.getValues().size() == 1) {
                        data.put(key, _convertContent(((EmbeddedField) value).getEmbeddedValues().get(0).getContent()));
                    } else {

                        ListWrapper.ContentModelDataListWrapper wrapper = new ListWrapper.ContentModelDataListWrapper(
                                ((EmbeddedField) value).getEmbeddedValues().stream()
                                        .map(fieldSet -> _convertContent(fieldSet.getContent()))
                                        .collect(Collectors.toList()));
                        data.put(key, wrapper);
                    }
                    break;
                case COMPONENTLINK:
                    data.put(key, new EntityModelData());
                    break;
                case TEXT:
                default:
                    data.put(key, value.getValues().get(0));
            }
        }
        return data;
    }

}
