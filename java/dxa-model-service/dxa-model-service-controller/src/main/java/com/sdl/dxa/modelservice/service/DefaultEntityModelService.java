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
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class DefaultEntityModelService implements EntityModelService {

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
        int componentId = entityRequest.getComponentId();
        int templateId = entityRequest.getTemplateId() <= 0 ?
                configService.getDefaults().getDynamicTemplateId(publicationId) : entityRequest.getTemplateId();

        String componentUri = TcmUtils.buildTcmUri(publicationId, componentId);
        String templateUri = TcmUtils.buildTemplateTcmUri(publicationId, templateId);

        ComponentPresentationFactory componentPresentationFactory = new ComponentPresentationFactory(componentUri);
        ComponentPresentation componentPresentation = componentPresentationFactory.getComponentPresentation(componentUri, templateUri);

        if (componentPresentation == null) {
            throw new DxaItemNotFoundException("Cannot find a CP for componentUri" + componentUri + ", templateUri" + templateUri);
        }

        EntityModelData modelData = _parseR2Content(componentPresentation.getContent(), EntityModelData.class);
        if (entityRequest.isResolveLink()) {
            modelData.setLinkUrl(linkResolver.resolveLink(componentUri, String.valueOf(publicationId)));
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
