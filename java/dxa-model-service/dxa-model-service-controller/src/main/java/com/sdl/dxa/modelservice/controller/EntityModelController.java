package com.sdl.dxa.modelservice.controller;

import com.sdl.dxa.api.datamodel.model.EntityModelData;
import com.sdl.dxa.common.dto.EntityRequestDto;
import com.sdl.dxa.common.dto.EntityRequestDto.DcpType;
import com.sdl.dxa.modelservice.service.EntityModelService;
import com.sdl.webapp.common.api.content.ContentProviderException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/EntityModel/{uriType}/{localizationId}")
public class EntityModelController {

    private final EntityModelService entityModelService;

    @Autowired
    public EntityModelController(EntityModelService entityModelService) {
        this.entityModelService = entityModelService;
    }

    @GetMapping(path = {
            "/{componentId:\\d+}-{templateId:\\d+}",
            "/{componentId:\\d+}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public EntityModelData getEntityModel(@PathVariable String uriType,
                                          @PathVariable int localizationId,
                                          @PathVariable int componentId,
                                          @PathVariable(required = false) Integer templateId,
                                          @RequestParam(required = false, name = "dcpType") DcpType dcpType) throws ContentProviderException {
        log.debug("trying to load an entity with URI type = '{}' and localization id = '{}', and componentId = '{}', " +
                "templateId (<= 0 for no template) = '{}', and DCP type = '{}'", uriType, localizationId, componentId, templateId, dcpType);

        EntityRequestDto entityRequest = EntityRequestDto.builder(localizationId, componentId, templateId == null ? 0 : templateId)
                .dcpType(dcpType == null ? DcpType.DEFAULT : DcpType.HIGHEST_PRIORITY)
                .build();

        return entityModelService.loadEntity(entityRequest);
    }

}
