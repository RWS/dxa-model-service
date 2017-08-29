package com.sdl.dxa.modelservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdl.dxa.api.datamodel.model.EntityModelData;
import com.sdl.dxa.api.datamodel.model.ViewModelData;
import com.sdl.dxa.common.dto.EntityRequestDto;
import com.sdl.webapp.common.api.content.ContentProviderException;
import com.sdl.webapp.common.api.content.LinkResolver;
import com.sdl.webapp.common.exceptions.DxaItemNotFoundException;
import com.sdl.webapp.common.util.TcmUtils;
import com.tridion.dcp.ComponentPresentation;
import com.tridion.dcp.ComponentPresentationFactory;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
public class DefaultEntityModelService implements EntityModelService, ComponentPresentationService {

    private final ObjectMapper objectMapper;

    private final LinkResolver linkResolver;

    private final ConfigService configService;

    @Autowired
    public DefaultEntityModelService(@Qualifier("dxaR2ObjectMapper") ObjectMapper objectMapper,
                                     LinkResolver linkResolver,
                                     ConfigService configService) {
        this.objectMapper = objectMapper;
        this.linkResolver = linkResolver;
        this.configService = configService;
    }

    @Override
    @NotNull
    @Cacheable(value = "entityModels", key = "{ #root.methodName, #entityRequest }")
    public EntityModelData loadEntity(EntityRequestDto entityRequest) throws ContentProviderException {
        int publicationId = entityRequest.getPublicationId();

        ComponentPresentation componentPresentation = loadComponentPresentation(entityRequest);

        EntityModelData modelData = _parseR2Content(componentPresentation.getContent(), EntityModelData.class);
        if (entityRequest.isResolveLink()) {
            modelData.setLinkUrl(linkResolver.resolveLink(TcmUtils.buildTcmUri(publicationId, entityRequest.getComponentId()), String.valueOf(publicationId)));
        }

        return modelData;
    }

    @Override
    @NotNull
    @Cacheable(value = "entityModels", key = "{ #root.methodName, #entityRequest}")
    public ComponentPresentation loadComponentPresentation(EntityRequestDto entityRequest) throws DxaItemNotFoundException {
        int publicationId = entityRequest.getPublicationId();

        String componentUri = TcmUtils.buildTcmUri(publicationId, entityRequest.getComponentId());
        ComponentPresentationFactory componentPresentationFactory = new ComponentPresentationFactory(componentUri);

        ComponentPresentation componentPresentation;

        if (entityRequest.getDcpType() == EntityRequestDto.DcpType.HIGHEST_PRIORITY && entityRequest.getTemplateId() <= 0) {
            log.debug("Load Component Presentation with component id = {} with highest priority", componentUri);
            componentPresentation = componentPresentationFactory.getComponentPresentationWithHighestPriority(componentUri);
        } else {
            String templateUri;
            if (entityRequest.getTemplateId() > 0) {
                templateUri = TcmUtils.buildTemplateTcmUri(publicationId, entityRequest.getTemplateId());
            } else {
                templateUri = TcmUtils.buildTemplateTcmUri(publicationId, configService.getDefaults().getDynamicTemplateId(publicationId));
            }

            log.debug("Load Component Presentation with component uri = {} and template uri = {}", componentUri, templateUri);
            componentPresentation = componentPresentationFactory.getComponentPresentation(componentUri, templateUri);
        }

        if (componentPresentation == null) {
            throw new DxaItemNotFoundException("Cannot find a CP for componentUri = " + componentUri + ", template id = " + entityRequest.getTemplateId());
        }
        return componentPresentation;
    }

    private <T extends ViewModelData> T _parseR2Content(String content, Class<T> expectedClass) throws ContentProviderException {
        try {
            return objectMapper.readValue(content, expectedClass);
        } catch (IOException e) {
            throw new ContentProviderException("Couldn't deserialize content '" + content + "' for " + expectedClass, e);
        }
    }
}
