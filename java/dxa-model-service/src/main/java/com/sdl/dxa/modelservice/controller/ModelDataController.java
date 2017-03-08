package com.sdl.dxa.modelservice.controller;

import com.sdl.dxa.api.datamodel.model.EntityModelData;
import com.sdl.dxa.api.datamodel.model.PageModelData;
import com.sdl.dxa.modelservice.service.ModelDataService;
import com.sdl.webapp.common.api.content.ContentProviderException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
public class ModelDataController {

    /**
     * {@code /PageModel/tcm/42/example/path/to/site}<br/>
     * {@code /PageModel/tcm/42//example/path/to/site}<br/>
     * <p>Explanation:</p>
     * <ol>
     * <li>Starts with {@code /}</li>
     * <li>Followed by not {@code /}</li>
     * <li>1 & 2 repeats 3 times: {@code /PageModel}, {@code /tcm}, {@code /42}</li>
     * <li>Page path can start with {@code /} or {@code //}, so {@code /example/path/to/site} or {@code //example/path/to/site} both are ok</li>
     * </ol>
     */
    private static final String PAGE_URL_REGEX = "/[^/]+/[^/]+/[^/]+//?";

    private final ModelDataService modelDataService;

    @Autowired
    public ModelDataController(ModelDataService modelDataService) {
        this.modelDataService = modelDataService;
    }

    @GetMapping(value = "/PageModel/{uriType}/{localizationId}/**")
    public PageModelData getPageModel(@PathVariable String uriType,
                                      @PathVariable int localizationId,
                                      HttpServletRequest request) throws ContentProviderException {

        String pageUrl = request.getRequestURI().replaceFirst(PAGE_URL_REGEX, "/");

        log.debug("Trying to request a page with URI type = '{}' and localization id = '{}' and path = '{}'", uriType, localizationId, pageUrl);

        return modelDataService.loadPage(localizationId, pageUrl);
    }

    @GetMapping(value = "/EntityModel/{uriType}/{localizationId}/{componentId:\\d+}-{templateId:\\d+}")
    public EntityModelData getEntityModel(@PathVariable String uriType,
                                          @PathVariable int localizationId,
                                          @PathVariable int componentId,
                                          @PathVariable int templateId) throws ContentProviderException {

        log.debug("trying to load an entity with URI type = '{}' and localization id = '{}', and componentId = '{}', templateId = '{}'",
                uriType, localizationId, componentId, templateId);

        return modelDataService.loadEntity(localizationId, componentId, templateId);
    }
}
