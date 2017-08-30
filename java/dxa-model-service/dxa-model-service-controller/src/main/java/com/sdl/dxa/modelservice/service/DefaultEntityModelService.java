package com.sdl.dxa.modelservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdl.dxa.api.datamodel.model.EntityModelData;
import com.sdl.dxa.api.datamodel.model.ViewModelData;
import com.sdl.dxa.common.dto.EntityRequestDto;
import com.sdl.dxa.common.dto.PageRequestDto;
import com.sdl.dxa.modelservice.service.processing.conversion.ToDd4tConverter;
import com.sdl.dxa.modelservice.service.processing.conversion.ToR2Converter;
import com.sdl.webapp.common.api.content.ContentProviderException;
import com.sdl.webapp.common.api.content.LinkResolver;
import com.sdl.webapp.common.util.TcmUtils;
import lombok.extern.slf4j.Slf4j;
import org.dd4t.contentmodel.ComponentPresentation;
import org.dd4t.contentmodel.impl.ComponentPresentationImpl;
import org.dd4t.core.exceptions.SerializationException;
import org.dd4t.databind.DataBindFactory;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;

import static com.sdl.dxa.modelservice.service.ContentService.getModelType;

@Slf4j
@Service
public class DefaultEntityModelService implements EntityModelService, LegacyEntityModelService {

    private final ObjectMapper objectMapper;

    private final LinkResolver linkResolver;

    private final ContentService contentService;

    private ToDd4tConverter toDd4tConverter;

    private ToR2Converter toR2Converter;


    @Autowired
    public DefaultEntityModelService(@Qualifier("dxaR2ObjectMapper") ObjectMapper objectMapper,
                                     LinkResolver linkResolver,
                                     ContentService contentService) {
        this.objectMapper = objectMapper;
        this.linkResolver = linkResolver;
        this.contentService = contentService;
    }

    @Autowired
    public void setToDd4tConverter(ToDd4tConverter toDd4tConverter) {
        this.toDd4tConverter = toDd4tConverter;
    }

    @Autowired
    public void setToR2Converter(ToR2Converter toR2Converter) {
        this.toR2Converter = toR2Converter;
    }

    @Override
    @NotNull
    @Cacheable(value = "entityModels", key = "{ #root.methodName, #entityRequest }")
    public EntityModelData loadEntity(EntityRequestDto entityRequest) throws ContentProviderException {
        String content = contentService.loadEntityContent(entityRequest);

        int pubId = entityRequest.getPublicationId();
        int componentId = entityRequest.getComponentId();
        String componentUri = TcmUtils.buildTcmUri(pubId, componentId);

        EntityModelData modelData = _processR2EntityModel(content, entityRequest);
        if (entityRequest.isResolveLink()) {
            modelData.setLinkUrl(linkResolver.resolveLink(componentUri, String.valueOf(pubId)));
        }

        return modelData;
    }

    @Cacheable(value = "entityModels", key = "{ #root.methodName, #entityRequest }")
    public org.dd4t.contentmodel.ComponentPresentation loadLegacyEntityModel(EntityRequestDto entityRequest) throws ContentProviderException {
        String content = contentService.loadEntityContent(entityRequest);
        log.trace("Loaded entity content for {}", entityRequest);
        return _processDd4tEntityModel(content, entityRequest);
    }


    private <T extends ViewModelData> T _parseR2Content(String content, Class<T> expectedClass) throws ContentProviderException {
        try {
            return objectMapper.readValue(content, expectedClass);
        } catch (IOException e) {
            throw new ContentProviderException("Couldn't deserialize content '" + content + "' for " + expectedClass, e);
        }
    }

    @Contract("!null, _ -> !null")
    private ComponentPresentation _processDd4tEntityModel(String entityContent, EntityRequestDto entityRequest) throws ContentProviderException {
        ComponentPresentation cp;
        PageRequestDto.DataModelType publishedModelType = getModelType(entityContent);
        if (publishedModelType == PageRequestDto.DataModelType.R2) {
            log.info("Found R2 model while requested DD4T, need to process R2 and convert, request {}", entityRequest);
            EntityModelData r2entity = _processR2EntityModel(entityContent, entityRequest);
            cp = toDd4tConverter.convertToDd4t(r2entity, entityRequest);
        } else {
            try {
                cp = DataBindFactory.buildDynamicComponentPresentation(entityContent, ComponentPresentationImpl.class);
                log.trace("Parsed page content to page model {}", cp);
                return cp;
            } catch (SerializationException e) {
                throw new ContentProviderException("Couldn't deserialize DD4T content for request " + entityRequest, e);
            }
        }
        return cp;
    }

    @Contract("!null, _ -> !null")
    private EntityModelData _processR2EntityModel(String entityContent, EntityRequestDto entityRequest) throws ContentProviderException {
        PageRequestDto.DataModelType publishedModelType = getModelType(entityContent);
        EntityModelData entityModel;
        if (publishedModelType == PageRequestDto.DataModelType.DD4T) {
            log.info("Found DD4T model while requested R2, need to convert, no expansion needed, request {}", entityRequest);
            ComponentPresentation cp = this._processDd4tEntityModel(entityContent, entityRequest);
            entityModel = toR2Converter.convertToR2(cp, entityRequest);
        } else {
            entityModel = _parseR2Content(entityContent, EntityModelData.class);
            log.trace("Parsed entity content to entity model {}", entityModel);
        }

        log.trace("processing entity model {} for entity request {}", entityModel, entityRequest);

        return entityModel;
    }
}
