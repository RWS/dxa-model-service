package com.sdl.dxa.modelservice.controller;

import com.sdl.dxa.api.datamodel.model.EntityModelData;
import com.sdl.dxa.modelservice.service.ContentService;
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
@RequestMapping("/EntityModel/{uriType}/{localizationId}/{componentId:\\d+}-{templateId:\\d+}")
public class EntityModelController {

    private final ContentService contentService;

    @Autowired
    public EntityModelController(ContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public EntityModelData getEntityModel(@PathVariable String uriType,
                                          @PathVariable int localizationId,
                                          @PathVariable int componentId,
                                          @PathVariable int templateId) throws ContentProviderException {

        log.debug("trying to load an entity with URI type = '{}' and localization id = '{}', and componentId = '{}', templateId = '{}'",
                uriType, localizationId, componentId, templateId);

        return contentService.loadEntity(localizationId, componentId, templateId);
    }
}
