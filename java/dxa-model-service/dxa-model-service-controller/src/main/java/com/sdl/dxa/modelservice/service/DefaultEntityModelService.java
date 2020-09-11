package com.sdl.dxa.modelservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdl.dxa.api.datamodel.model.EntityModelData;
import com.sdl.dxa.api.datamodel.model.ViewModelData;
import com.sdl.dxa.common.dto.DataModelType;
import com.sdl.dxa.common.dto.EntityRequestDto;
import com.sdl.dxa.modelservice.service.processing.conversion.ToDd4tConverter;
import com.sdl.dxa.modelservice.service.processing.conversion.ToR2Converter;
import com.sdl.dxa.modelservice.service.processing.expansion.EntityModelExpander;
import com.sdl.dxa.tridion.linking.RichTextLinkResolver;
import com.sdl.dxa.tridion.linking.api.BatchLinkResolver;
import com.sdl.webapp.common.api.content.ContentProviderException;
import com.sdl.webapp.common.api.content.LinkResolver;
import com.sdl.webapp.common.api.content.PageNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.dd4t.contentmodel.ComponentPresentation;
import org.dd4t.contentmodel.impl.ComponentPresentationImpl;
import org.dd4t.core.databind.DataBinder;
import org.dd4t.core.exceptions.SerializationException;
import org.dd4t.core.processors.impl.RichTextResolver;
import org.dd4t.core.util.HttpRequestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import static com.sdl.dxa.modelservice.service.ContentService.getModelType;

@Slf4j
@Service
public class DefaultEntityModelService implements EntityModelServiceSuppressLinks, LegacyEntityModelService {

    private final ObjectMapper objectMapper;

    private final LinkResolver linkResolver;

    private final ContentService contentService;

    private final RichTextLinkResolver richTextLinkResolver;

    private final ConfigService configService;

    private DataBinder dd4tDataBinder;

    private RichTextResolver dd4tRichTextResolver;

    private ToDd4tConverter toDd4tConverter;

    private ToR2Converter toR2Converter;


    @Autowired
    public DefaultEntityModelService(@Qualifier("dxaR2ObjectMapper") ObjectMapper objectMapper,
                                     @Qualifier("dxaLinkResolver") LinkResolver linkResolver,
                                     ContentService contentService,
                                     DataBinder dd4tDataBinder,
                                     RichTextResolver dd4tRichTextResolver,
                                     RichTextLinkResolver richTextLinkResolver,
                                     ConfigService configService) {
        this.objectMapper = objectMapper;
        this.linkResolver = linkResolver;
        this.contentService = contentService;
        this.richTextLinkResolver = richTextLinkResolver;
        this.configService = configService;

        this.dd4tDataBinder = dd4tDataBinder;
        this.dd4tRichTextResolver = dd4tRichTextResolver;
    }

    public void setToDd4tConverter(ToDd4tConverter toDd4tConverter) {
        this.toDd4tConverter = toDd4tConverter;
    }

    @Autowired
    public void setToR2Converter(ToR2Converter toR2Converter) {
        this.toR2Converter = toR2Converter;
    }

    public EntityModelData loadEntity(EntityRequestDto entityRequest, boolean resolveLinks) throws ContentProviderException {
        try {
            String content = contentService.loadComponentPresentationNotCached(entityRequest).getContent();
            log.trace("Loaded entity content for {}", entityRequest);
            return _processR2EntityModel(content, entityRequest, resolveLinks);
        } catch (PageNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Could not load entity model for {}", entityRequest, ex);
            throw new PageNotFoundException(ex);
        }
    }

    @Override
    @Cacheable(value = "entityModels", key = "{ #root.methodName, #entityRequest }", sync = true)
    public EntityModelData loadEntity(EntityRequestDto entityRequest) throws ContentProviderException {
        return loadEntityNotCached(entityRequest);
    }

    public EntityModelData loadEntityNotCached(EntityRequestDto entityRequest) throws ContentProviderException {
        return loadEntity(entityRequest, true);
    }

    public org.dd4t.contentmodel.ComponentPresentation loadLegacyEntityModel(EntityRequestDto entityRequest) throws ContentProviderException {
        try {
            String content = contentService.loadComponentPresentationNotCached(entityRequest).getContent();
            log.trace("Loaded entity content for {}", entityRequest);
            return _processDd4tEntityModel(content, entityRequest);
        } catch (PageNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Could not load legacy entity model for {}", entityRequest, ex);
            throw new PageNotFoundException(ex);
        }
    }

    private ComponentPresentation _processDd4tEntityModel(String content, EntityRequestDto entityRequest) throws ContentProviderException {
        DataModelType publishedModelType = getModelType(content);
        if (publishedModelType == DataModelType.R2) {
            log.info("Found R2 model while requested DD4T, need to process R2 and convert, request {}", entityRequest);
            EntityModelData r2entity = _processR2EntityModel(content, entityRequest, true);
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
            } catch (Exception e) {
                throw new ContentProviderException("Couldn't process DD4T content for request " + entityRequest, e);
            }
        }
    }

    private EntityModelData _processR2EntityModel(String entityContent, EntityRequestDto entityRequest, boolean resolveLinks) throws ContentProviderException {
        log.trace("processing entity model for entity request {}", entityRequest);

        EntityModelData modelData;
        DataModelType publishedModelType = getModelType(entityContent);
        if (publishedModelType == DataModelType.DD4T) {
            log.info("Found DD4T model while requested R2, need to convert, no expansion needed, request {}", entityRequest);
            modelData = toR2Converter.convertToR2(_processDd4tEntityModel(entityContent, entityRequest), entityRequest);
        } else {
            log.trace("Parsing entity content {}", entityContent);
            modelData = _parseR2Content(entityContent, EntityModelData.class);
        }
        log.trace("processing entity model {} for entity request {}", modelData, entityRequest);
        _getModelExpander(entityRequest, resolveLinks).expandEntity(modelData);
        log.trace("expanded the whole model for {}", entityRequest);
        return modelData;
    }

    private EntityModelExpander _getModelExpander(EntityRequestDto entityRequestDto, boolean resolveLinks) {
        return new EntityModelExpander(
                entityRequestDto,
                richTextLinkResolver,
                configService, resolveLinks, getBatchLinkResolver());
    }

    private <T extends ViewModelData> T _parseR2Content(String content, Class<T> expectedClass) throws ContentProviderException {
        try {
            return objectMapper.readValue(content, expectedClass);
        } catch (Exception e) {
            throw new ContentProviderException("Couldn't deserialize content '" + content + "' for " + expectedClass, e);
        }
    }

    @Lookup
    public BatchLinkResolver getBatchLinkResolver() {
        return null;
    }

}
