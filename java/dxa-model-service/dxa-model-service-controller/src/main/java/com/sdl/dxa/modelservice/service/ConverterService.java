package com.sdl.dxa.modelservice.service;

import com.sdl.dxa.api.datamodel.model.EntityModelData;
import com.sdl.dxa.api.datamodel.model.PageModelData;
import com.sdl.dxa.api.datamodel.model.RegionModelData;
import com.sdl.dxa.common.dto.PageRequestDto;
import com.sdl.dxa.common.dto.PageRequestDto.DataModelType;
import com.sdl.webapp.common.util.TcmUtils;
import com.tridion.dcp.ComponentPresentationFactory;
import lombok.extern.slf4j.Slf4j;
import org.dd4t.contentmodel.Component;
import org.dd4t.contentmodel.ComponentPresentation;
import org.dd4t.contentmodel.ComponentTemplate;
import org.dd4t.contentmodel.Page;
import org.dd4t.contentmodel.impl.ComponentImpl;
import org.dd4t.contentmodel.impl.ComponentPresentationImpl;
import org.dd4t.contentmodel.impl.ComponentTemplateImpl;
import org.dd4t.contentmodel.impl.PageImpl;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Converter service is capable to convert R2 to DD4T data models both ways.
 */
@Slf4j
@Service
public class ConverterService {

    /**
     * Detects model type from json content string.
     *
     * @param jsonContent json content of a page
     * @return type of the model
     */
    public static DataModelType getModelType(String jsonContent) {
        // todo implement
        return DataModelType.DD4T;
    }

    /**
     * Converts the given R2 data model to DD4T data model.
     *
     * @param toConvert      R2 page model to convert
     * @param pageRequestDto current page request
     * @return equal DD4T model, {@code null} in case parameter is {@code null}
     */
    @Contract("!null, _ -> !null; null, _ -> null")
    public Page convertToDd4t(@Nullable PageModelData toConvert, @NotNull PageRequestDto pageRequestDto) {
        if (toConvert == null) {
            log.warn("Model to convert is null, returning null");
            return null;
        }

        PageImpl page = new PageImpl();

        page.setId(TcmUtils.buildPageTcmUri(String.valueOf(pageRequestDto.getPublicationId()), toConvert.getId()));
        page.setTitle(toConvert.getTitle());


        List<ComponentPresentation> componentPresentations = new ArrayList<>();
        for (RegionModelData region : toConvert.getRegions()) {
            componentPresentations.addAll(_loadComponentPresentations(region, pageRequestDto, new ComponentPresentationFactory(pageRequestDto.getPublicationId())));
        }
        page.setComponentPresentations(componentPresentations);


        //todo load page template and fill /PageTemplate

        return page;
    }

    private List<ComponentPresentation> _loadComponentPresentations(RegionModelData region,
                                                                    @NotNull PageRequestDto pageRequestDto, ComponentPresentationFactory factory) {
        Stream<ComponentPresentation> nestedRegions = region.getRegions().parallelStream()
                .map(nested -> _loadComponentPresentations(nested, pageRequestDto, factory))
                .flatMap(List::stream);

        Stream<ComponentPresentation> currentRegion = region.getEntities().parallelStream()
                .map(entity -> _fromEntityModel(entity, pageRequestDto, factory));

        return Stream.concat(nestedRegions, currentRegion).collect(Collectors.toList());
    }


    private ComponentPresentation _fromEntityModel(EntityModelData entity, @NotNull PageRequestDto pageRequestDto, ComponentPresentationFactory factory) {
        String componentUri = TcmUtils.buildTcmUri(String.valueOf(pageRequestDto.getPublicationId()), entity.getId());
        com.tridion.dcp.ComponentPresentation cp = factory.getComponentPresentation(componentUri, entity.getComponentTemplateId());


        ComponentPresentation presentation = new ComponentPresentationImpl();

        Component component = new ComponentImpl();
        component.setId(TcmUtils.buildTcmUri(String.valueOf(pageRequestDto.getPublicationId()), entity.getId()));
//        entity.getContent().entrySet()


        ComponentTemplate template = new ComponentTemplateImpl();
        template.setId(TcmUtils.buildTemplateTcmUri(pageRequestDto.getPublicationId(), cp.getComponentTemplateId()));
        //todo load template and fill the rest


        presentation.setComponent(component);
        presentation.setComponentTemplate(template);

        return presentation;
    }

    /**
     * Converts the given DD4T data model to R2 data model.
     *
     * @param toConvert      DD4T page model to convert
     * @param pageRequestDto current page request
     * @return equal R2 model, {@code null} in case parameter is {@code null}
     */
    @Contract("!null, _ -> !null; null, _ -> null")
    public PageModelData convertToR2(@Nullable Page toConvert, @NotNull PageRequestDto pageRequestDto) {
        if (toConvert == null) {
            log.warn("Model to convert is null, returning null");
            return null;
        }

        PageModelData page = new PageModelData();

        page.setId(String.valueOf(TcmUtils.getItemId(toConvert.getId())));
        page.setTitle(toConvert.getTitle());

        return page;
    }
}
