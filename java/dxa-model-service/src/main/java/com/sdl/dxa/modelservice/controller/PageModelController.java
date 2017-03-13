package com.sdl.dxa.modelservice.controller;

import com.sdl.dxa.api.datamodel.model.PageModelData;
import com.sdl.dxa.modelservice.service.ContentService;
import com.sdl.webapp.common.api.content.ContentProviderException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/PageModel/{uriType}/{localizationId}/**")
public class PageModelController {

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

    private final ContentService contentService;

    @Autowired
    public PageModelController(ContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public PageModelData getPageModel(@PathVariable String uriType,
                                      @PathVariable int localizationId,
                                      HttpServletRequest request) throws ContentProviderException {
        return contentService.loadPageModel(localizationId, getPageUrl(request));
    }

    private String getPageUrl(HttpServletRequest request) {
        return request.getRequestURI().replaceFirst(PageModelController.PAGE_URL_REGEX, "/");
    }

    @GetMapping(params = "raw")
    public String getPageSource(@PathVariable String uriType,
                                @PathVariable int localizationId,
                                HttpServletRequest request) throws ContentProviderException {
        return contentService.loadPageContent(localizationId, getPageUrl(request));
    }
}
