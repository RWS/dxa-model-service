package com.sdl.dxa.modelservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdl.dxa.api.datamodel.model.EntityModelData;
import com.sdl.dxa.api.datamodel.model.ViewModelData;
import com.sdl.dxa.common.dto.DataModelType;
import com.sdl.dxa.common.dto.EntityRequestDto;
import com.sdl.dxa.modelservice.service.processing.conversion.ToDd4tConverter;
import com.sdl.dxa.modelservice.service.processing.conversion.ToR2Converter;
import com.sdl.webapp.common.api.content.ContentProviderException;
import com.sdl.webapp.common.api.content.LinkResolver;
import com.sdl.webapp.common.util.TcmUtils;
import lombok.extern.slf4j.Slf4j;
import org.dd4t.contentmodel.ComponentPresentation;
import org.dd4t.contentmodel.impl.ComponentPresentationImpl;
import org.dd4t.core.databind.DataBinder;
import org.dd4t.core.exceptions.ProcessorException;
import org.dd4t.core.exceptions.SerializationException;
import org.dd4t.core.processors.impl.RichTextResolver;
import org.dd4t.core.util.HttpRequestContext;
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

    private DataBinder dd4tDataBinder;

    private RichTextResolver dd4tRichTextResolver;

    private ToDd4tConverter toDd4tConverter;

    private ToR2Converter toR2Converter;


    @Autowired
    public DefaultEntityModelService(@Qualifier("dxaR2ObjectMapper") ObjectMapper objectMapper,
                                     LinkResolver linkResolver,
                                     ContentService contentService,
                                     DataBinder dd4tDataBinder,
                                     RichTextResolver dd4tRichTextResolver) {
        this.objectMapper = objectMapper;
        this.linkResolver = linkResolver;
        this.contentService = contentService;
        this.dd4tDataBinder = dd4tDataBinder;
        this.dd4tRichTextResolver = dd4tRichTextResolver;
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
        String content = contentService.loadComponentPresentation(entityRequest).getContent();
        log.trace("Loaded entity content for {}", entityRequest);
        return _processR2EntityModel(content, entityRequest);
    }

    @NotNull
    @Cacheable(value = "entityModels", key = "{ #root.methodName, #entityRequest }")
    public org.dd4t.contentmodel.ComponentPresentation loadLegacyEntityModel(EntityRequestDto entityRequest) throws ContentProviderException {
        String content = contentService.loadRenderedComponentPresentation(entityRequest.getPublicationId(), entityRequest.getComponentId(), entityRequest.getTemplateId());
        log.trace("Loaded entity content for {}", entityRequest);
        return _processDd4tEntityModel(content, entityRequest);
    }

    @Contract("!null, _ -> !null")
    private ComponentPresentation _processDd4tEntityModel(String content, EntityRequestDto entityRequest) throws ContentProviderException {
        DataModelType publishedModelType = getModelType(content);
        if (publishedModelType == DataModelType.R2) {
            log.info("Found R2 model while requested DD4T, need to process R2 and convert, request {}", entityRequest);
            EntityModelData r2entity = _processR2EntityModel(content, entityRequest);

            // Expand entity if it's dynamic
            if(r2entity.getId().matches("\\d+-\\d+")) {
                r2entity = this.loadEntity(entityRequest);
            }

            return toDd4tConverter.convertToDd4t(r2entity, entityRequest);
        } else {
            try {
                log.trace("parsing entity content {}", content);
                ComponentPresentation componentPresentation = dd4tDataBinder.buildComponentPresentation(content, ComponentPresentationImpl.class);

                // we only resolve links using DD4T if we request content also in DD4T, otherwise our resolver is used
                if (entityRequest.getDataModelType() == DataModelType.DD4T) {
                    dd4tRichTextResolver.execute(componentPresentation.getComponent(), new HttpRequestContext());
                }

                return componentPresentation;
            } catch (SerializationException e) {
                throw new ContentProviderException("Couldn't deserialize DD4T content for request " + entityRequest, e);
            } catch (ProcessorException e) {
                throw new ContentProviderException("Couldn't process DD4T content for request " + entityRequest, e);
            }
        }
    }

    @Contract("!null, _ -> !null")
    private EntityModelData _processR2EntityModel(String entityContent, EntityRequestDto entityRequest) throws ContentProviderException {
        log.trace("processing entity model for entity request {}", entityRequest);

        EntityModelData modelData;
        DataModelType publishedModelType = getModelType(entityContent);
        if (publishedModelType == DataModelType.DD4T) {
            log.info("Found DD4T model while requested R2, need to convert, no expansion needed, request {}", entityRequest);
            modelData = toR2Converter.convertToR2(_processDd4tEntityModel(entityContent, entityRequest), entityRequest);
        } else {
            log.trace("Parsing entity content {}", entityContent);
            modelData = _parseR2Content(entityContent, EntityModelData.class);
            return modelData;
        }

        int publicationId = entityRequest.getPublicationId();
        if (entityRequest.isResolveLink()) {
            modelData.setLinkUrl(linkResolver.resolveLink(TcmUtils.buildTcmUri(publicationId, entityRequest.getComponentId()), String.valueOf(publicationId)));
        }

        return modelData;
    }

    private <T extends ViewModelData> T _parseR2Content(String content, Class<T> expectedClass) throws ContentProviderException {
        try {
            return objectMapper.readValue(content, expectedClass);
        } catch (IOException e) {
            throw new ContentProviderException("Couldn't deserialize content '" + content + "' for " + expectedClass, e);
        }
    }
}
