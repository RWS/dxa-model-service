package com.sdl.dxa.modelservice.controller;

import com.sdl.dxa.api.datamodel.model.EntityModelData;
import com.sdl.dxa.common.dto.EntityRequestDto;
import com.sdl.dxa.modelservice.service.EntityModelService;
import com.sdl.webapp.common.api.content.ContentProviderException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/EntityModel/{uriType}/{localizationId}")
public class EntityModelController {

    private final EntityModelService contentService;

    @Autowired
    public EntityModelController(EntityModelService contentService) {
        this.contentService = contentService;
    }

    @GetMapping(path = "/{componentId:\\d+}-{templateId:\\d+}", produces = MediaType.APPLICATION_JSON_VALUE)
    public EntityModelData getEntityModel(@PathVariable String uriType,
                                          @PathVariable int localizationId,
                                          @PathVariable int componentId,
                                          @PathVariable int templateId) throws ContentProviderException {
        return _getEntityModel(uriType, localizationId, componentId, templateId);
    }

    @GetMapping(path = "/{componentId:\\d+}", produces = MediaType.APPLICATION_JSON_VALUE)
    public EntityModelData getEntityModel(@PathVariable String uriType,
                                          @PathVariable int localizationId,
                                          @PathVariable int componentId) throws ContentProviderException {

        return _getEntityModel(uriType, localizationId, componentId, 0);
    }

    private EntityModelData _getEntityModel(String uriType, int localizationId, int componentId, int templateId) throws ContentProviderException {
        log.debug("trying to load an entity with URI type = '{}' and localization id = '{}', and componentId = '{}', templateId (-1 for no template) = '{}'",
                uriType, localizationId, componentId, templateId);

        return contentService.loadEntity(EntityRequestDto.builder(localizationId, componentId, templateId).build());
    }
}
