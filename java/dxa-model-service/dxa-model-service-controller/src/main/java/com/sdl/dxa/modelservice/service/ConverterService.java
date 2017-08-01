package com.sdl.dxa.modelservice.service;

import com.google.common.collect.Lists;
import com.sdl.dxa.api.datamodel.model.ContentModelData;
import com.sdl.dxa.api.datamodel.model.EntityModelData;
import com.sdl.dxa.api.datamodel.model.PageModelData;
import com.sdl.dxa.api.datamodel.model.RegionModelData;
import com.sdl.dxa.api.datamodel.model.util.ListWrapper;
import com.sdl.dxa.common.dto.PageRequestDto;
import com.sdl.dxa.common.dto.PageRequestDto.DataModelType;
import com.sdl.webapp.common.util.TcmUtils;
import com.tridion.broker.StorageException;
import com.tridion.dcp.ComponentPresentationFactory;
import com.tridion.meta.PageMeta;
import com.tridion.meta.PageMetaFactory;
import com.tridion.meta.PublicationMeta;
import com.tridion.meta.PublicationMetaFactory;
import lombok.extern.slf4j.Slf4j;
import org.dd4t.contentmodel.Component;
import org.dd4t.contentmodel.ComponentPresentation;
import org.dd4t.contentmodel.ComponentTemplate;
import org.dd4t.contentmodel.Field;
import org.dd4t.contentmodel.FieldSet;
import org.dd4t.contentmodel.FieldType;
import org.dd4t.contentmodel.Page;
import org.dd4t.contentmodel.Publication;
import org.dd4t.contentmodel.impl.ComponentImpl;
import org.dd4t.contentmodel.impl.ComponentLinkField;
import org.dd4t.contentmodel.impl.ComponentPresentationImpl;
import org.dd4t.contentmodel.impl.ComponentTemplateImpl;
import org.dd4t.contentmodel.impl.EmbeddedField;
import org.dd4t.contentmodel.impl.FieldSetImpl;
import org.dd4t.contentmodel.impl.PageImpl;
import org.dd4t.contentmodel.impl.PublicationImpl;
import org.dd4t.contentmodel.impl.TextField;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public Page convertToDd4t(@Nullable PageModelData toConvert, @NotNull PageRequestDto pageRequestDto) throws StorageException {
        if (toConvert == null) {
            log.warn("Model to convert is null, returning null");
            return null;
        }

        PageMeta pageMeta = new PageMetaFactory(pageRequestDto.getPublicationId()).getMeta(toConvert.getId());

        PageImpl page = new PageImpl();

        page.setId(TcmUtils.buildPageTcmUri(String.valueOf(pageRequestDto.getPublicationId()), toConvert.getId()));
        page.setTitle(toConvert.getTitle());
        page.setVersion(pageMeta.getMajorVersion());
        page.setLastPublishedDate(new DateTime(pageMeta.getLastPublicationDate()));
        page.setRevisionDate(new DateTime(pageMeta.getModificationDate()));
        page.setFileName(pageMeta.getPath()); // todo extract index out of /path/index.html
        page.setFileExtension(pageMeta.getPath()); // todo extract html out of /path/index.html or page template


        PublicationMeta publicationMeta = new PublicationMetaFactory().getMeta(pageRequestDto.getPublicationId());
        Publication publication = new PublicationImpl(TcmUtils.buildPublicationTcmUri(publicationMeta.getId()));
        publication.setTitle(publicationMeta.getTitle());
        page.setPublication(publication);


        PublicationMeta owningPublicationMeta = new PublicationMetaFactory().getMeta(String.valueOf(pageMeta.getOwningPublicationId()));
        PublicationImpl owningPublication = new PublicationImpl(TcmUtils.buildPublicationTcmUri(owningPublicationMeta.getId()));
        owningPublication.setTitle(owningPublicationMeta.getTitle());
        page.setOwningPublication(owningPublication);


        // todo structure group

        // todo metadata fields

        //todo load page template and fill /PageTemplate

        List<ComponentPresentation> componentPresentations = new ArrayList<>();
        for (RegionModelData region : toConvert.getRegions()) {
            componentPresentations.addAll(_loadComponentPresentations(region, pageRequestDto, new ComponentPresentationFactory(pageRequestDto.getPublicationId())));
        }
        page.setComponentPresentations(componentPresentations);

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


    public ComponentPresentation _fromEntityModel(EntityModelData entity, @NotNull PageRequestDto pageRequestDto, ComponentPresentationFactory factory) {
//        String componentUri = TcmUtils.buildTcmUri(String.valueOf(pageRequestDto.getPublicationId()), entity.getId());
//        com.tridion.dcp.ComponentPresentation cp = factory.getComponentPresentation(componentUri, entity.getComponentTemplateId());


        ComponentPresentation presentation = new ComponentPresentationImpl();

        ComponentTemplate template = new ComponentTemplateImpl();
//        template.setId(TcmUtils.buildTemplateTcmUri(pageRequestDto.getPublicationId(), cp.getComponentTemplateId()));
        //todo load template and fill the rest


        presentation.setComponent(_fromEntity(entity, pageRequestDto));
        presentation.setComponentTemplate(template);

        return presentation;
    }

    private Component _fromEntity(EntityModelData entity, @NotNull PageRequestDto pageRequestDto) {
        Component component = new ComponentImpl();
        component.setId(TcmUtils.buildTcmUri(String.valueOf(pageRequestDto.getPublicationId()), entity.getId()));

        component.setContent(convertContent(entity.getContent(), pageRequestDto));
        return component;
    }

    private Map<String, Field> convertContent(ContentModelData contentModelData, @NotNull PageRequestDto pageRequestDto) {
        Map<String, Field> fields = new HashMap<>(contentModelData.size());

        for (Map.Entry<String, Object> entry : contentModelData.entrySet()) {
            Object value = entry.getValue();
            String name = entry.getKey();

            if (ListWrapper.ContentModelDataListWrapper.class.isAssignableFrom(value.getClass())) {
                EmbeddedField embeddedField = new EmbeddedField();
                fields.put(name, embeddedField);

                List<FieldSet> list = new ArrayList<>();
                ListWrapper.ContentModelDataListWrapper wrapper = (ListWrapper.ContentModelDataListWrapper) value;
                for (ContentModelData data : wrapper.getValues()) {
                    FieldSetImpl fieldSet = new FieldSetImpl();
                    list.add(fieldSet);
                    fieldSet.setContent(convertContent(data, pageRequestDto));
                }

                embeddedField.setName(name);
                embeddedField.setEmbeddedValues(list);
            } else if (ContentModelData.class.isAssignableFrom(value.getClass())) {
                EmbeddedField embeddedField = new EmbeddedField();
                fields.put(name, embeddedField);
                List<FieldSet> list = new ArrayList<>();

                FieldSetImpl fieldSet = new FieldSetImpl();
                fieldSet.setContent(convertContent((ContentModelData) value, pageRequestDto));
                list.add(fieldSet);

                embeddedField.setName(name);
                embeddedField.setEmbeddedValues(list);
            } else if (value instanceof String) {
                TextField textField = new TextField();
                fields.put(name, textField);
                textField.setName(name);
                textField.setTextValues(Lists.newArrayList((String) value));
            } else if (EntityModelData.class.isAssignableFrom(value.getClass())) {
                ComponentLinkField linkField = new ComponentLinkField();
                linkField.setName(name);
                linkField.setLinkedComponentValues(Lists.newArrayList(_fromEntity((EntityModelData) value, pageRequestDto)));
                fields.put(name, linkField);
            } else { // todo add KMD
                log.warn("Field of type {} is not supported", value.getClass());
            }
        }

        return fields;
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
        //todo UrlPath?


        Map<String, RegionModelData> regions = new HashMap<>();
        for (ComponentPresentation componentPresentation : toConvert.getComponentPresentations()) {
            ComponentTemplate componentTemplate = componentPresentation.getComponentTemplate();
            Component component = componentPresentation.getComponent();

            EntityModelData entity = new EntityModelData();
            entity.setId(String.valueOf(TcmUtils.getItemId(component.getId())));

            entity.setContent(convertContent(component.getContent()));

            String regionName = componentTemplate.getMetadata().containsKey("regionView") ?
                    componentTemplate.getMetadata().get("regionView").getValues().get(0).toString() : "Main";

            RegionModelData currentRegion = regions.containsKey(regionName) ? regions.get(regionName) : new RegionModelData();
            currentRegion.getEntities().add(entity);
        }

        return page;
    }

    private ContentModelData convertContent(Map<String, Field> content) {
        ContentModelData data = new ContentModelData();
        for (Map.Entry<String, Field> entry : content.entrySet()) {
            String key = entry.getKey();
            Field value = entry.getValue();
            FieldType fieldType = value.getFieldType();
            switch (fieldType) {
                case EMBEDDED:
                    if (value.getValues().size() == 1) {
                        data.put(key, convertContent(((EmbeddedField) value).getEmbeddedValues().get(0).getContent()));
                    } else {

                        ListWrapper.ContentModelDataListWrapper wrapper = new ListWrapper.ContentModelDataListWrapper(
                                ((EmbeddedField) value).getEmbeddedValues().stream()
                                        .map(fieldSet -> convertContent(fieldSet.getContent()))
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
