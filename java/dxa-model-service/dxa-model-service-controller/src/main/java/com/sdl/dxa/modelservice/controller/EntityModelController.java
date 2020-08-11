package com.sdl.dxa.modelservice.controller;

import com.sdl.dxa.modelservice.ModelServiceLocalizationIdProvider;
import com.sdl.dxa.common.dto.ContentType;
import com.sdl.dxa.common.dto.DataModelType;
import com.sdl.dxa.common.dto.EntityRequestDto;
import com.sdl.dxa.common.dto.EntityRequestDto.DcpType;
import com.sdl.dxa.modelservice.service.ContentService;
import com.sdl.dxa.modelservice.service.EntityModelService;
import com.sdl.webapp.common.api.content.ContentProviderException;
import com.sdl.webapp.common.api.content.PageNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/EntityModel/{uriType}/{localizationId}")
public class EntityModelController {
    private static final Logger LOG = LoggerFactory.getLogger(EntityModelController.class);

    private final ContentService contentService;

    private ModelServiceLocalizationIdProvider localizationIdProvider;

    private final EntityModelService entityModelService;

    @Autowired
    public EntityModelController(EntityModelService entityModelService,
                                 ContentService contentService,
                                 ModelServiceLocalizationIdProvider localizationIdProvider) {
        this.entityModelService = entityModelService;
        this.contentService = contentService;
        this.localizationIdProvider = localizationIdProvider;
    }

    @GetMapping(path = {
            "/{componentId:\\d+}-{templateId:\\d+}",
            "/{componentId:\\d+}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getEntityModel(@PathVariable String uriType,
                                         @PathVariable int localizationId,
                                         @PathVariable int componentId,
                                         @PathVariable(required = false) Integer templateId,
                                         @RequestParam(value = "raw", required = false, defaultValue = "false") boolean isRawContent,
                                         @RequestParam(value = "modelType", required = false, defaultValue = "R2") DataModelType dataModelType,
                                         @RequestParam(required = false, name = "dcpType") DcpType dcpType) throws ContentProviderException {
        localizationIdProvider.setCurrentId(localizationId);

        log.debug("trying to load an entity with URI type = '{}' and localization id = '{}', and componentId = '{}', " +
                "templateId (<= 0 for no template) = '{}', and DCP type = '{}', and raw = '{}'", uriType, localizationId, componentId, templateId, dcpType, isRawContent);

        EntityRequestDto entityRequest = EntityRequestDto.builder(localizationId, componentId, templateId == null ? 0 : templateId)
                .dcpType(dcpType == null ? DcpType.DEFAULT : DcpType.HIGHEST_PRIORITY)
                .dataModelType(dataModelType)
                .contentType(isRawContent ? ContentType.RAW : ContentType.MODEL)
                .build();

        return ResponseEntity.ok(isRawContent ?
                contentService.loadComponentPresentation(entityRequest) :
                entityModelService.loadEntity(entityRequest));
    }

    @ExceptionHandler({ Exception.class })
    public void handleException(Exception ex) throws PageNotFoundException {
        LOG.error("Could not load entity model", ex);
        throw new PageNotFoundException(ex);
    }
}
