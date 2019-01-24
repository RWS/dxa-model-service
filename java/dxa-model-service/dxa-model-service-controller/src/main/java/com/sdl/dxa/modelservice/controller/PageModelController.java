package com.sdl.dxa.modelservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sdl.dxa.common.dto.ContentType;
import com.sdl.dxa.common.dto.DataModelType;
import com.sdl.dxa.common.dto.PageRequestDto;
import com.sdl.dxa.common.dto.PageRequestDto.PageInclusion;
import com.sdl.dxa.modelservice.ModelServiceLocalizationIdProvider;
import com.sdl.dxa.modelservice.controller.Utils.ClaimCookieUtils;
import com.sdl.dxa.modelservice.service.ContentService;
import com.sdl.dxa.modelservice.service.LegacyPageModelService;
import com.sdl.dxa.modelservice.service.PageModelService;
import com.sdl.webapp.common.api.content.ContentProviderException;
import lombok.extern.slf4j.Slf4j;
import org.dd4t.databind.builder.json.JsonDataBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sdl.dxa.common.dto.ContentType.MODEL;
import static com.sdl.dxa.common.dto.ContentType.RAW;

@Slf4j
@RestController
@RequestMapping(value = "/PageModel/{uriType}")
public class PageModelController {

    /**
     * {@code /PageModel/tcm/42/example/path/to/site}<br/>
     * {@code /PageModel/tcm/42//example/path/to/site}<br/>
     * {@code /context/PageModel/tcm/42/example/path/to/site}<br/>
     * {@code /context/PageModel/tcm/42//example/path/to/site}<br/>
     * <p>Selects {@code /example/path/to/site} in all four cases.</p>
     * <p>Explanation of Regex:</p>
     * <ol>
     * <li>Starts with 1+ (lazily) amount of not empty non-numbers with leading slash: {@code (/[^\\d]+)+?}</li> which is everything before localization ID
     * <li>Followed by slash and not empty number {@code /\\d+}</li> which is the localization ID
     * <li>Followed by optional slash {@code /?}</li>
     * <li>Followed by named group pageUrl with the rest of URL {@code (?<pageUrl>/.*)}</li> which is the page URL
     * <li>Page path can start with {@code /} or {@code //}, so {@code /example/path/to/site} or {@code //example/path/to/site} both are ok</li>
     * </ol>
     */
    private static final Pattern PAGE_URL_REGEX = Pattern.compile("(/[^\\d]+)+?/\\d+/?(?<pageUrl>/.*)", Pattern.CASE_INSENSITIVE);

    private final PageModelService pageModelService;

    private final LegacyPageModelService legacyPageModelService;

    private final ContentService contentService;

    private ModelServiceLocalizationIdProvider localizationIdProvider;

    @Autowired
    public PageModelController(PageModelService pageModelService,
                               LegacyPageModelService legacyPageModelService,
                               ContentService contentService,
                               ModelServiceLocalizationIdProvider localizationIdProvider) {
        this.pageModelService = pageModelService;
        this.legacyPageModelService = legacyPageModelService;
        this.contentService = contentService;
        this.localizationIdProvider = localizationIdProvider;
    }

    @RequestMapping(value = {"/{localizationId}-{pageId}", "/{localizationId}/**"},
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity getPage(@PathVariable String uriType,
                                  @PathVariable int localizationId,
                                  @PathVariable(required = false) Optional<Integer> pageId,
                                  @RequestParam(value = "includes", required = false, defaultValue = "INCLUDE") PageInclusion pageInclusion,
                                  @RequestParam(value = "modelType", required = false, defaultValue = "R2") DataModelType dataModelType,
                                  @RequestParam(value = "raw", required = false, defaultValue = "false") boolean isRawContent,
                                  HttpServletRequest request) throws ContentProviderException, JsonProcessingException {

        ClaimCookieUtils.setupClaimStore(request);

        PageRequestDto pageRequestDto = buildPageRequest(uriType, localizationId, pageId, pageInclusion, dataModelType, isRawContent, request);

        if (pageRequestDto == null) {
            return ResponseEntity.badRequest().build();
        }

        localizationIdProvider.setCurrentId(pageRequestDto.getPublicationId());

        log.trace("requesting pageSource with {}", pageRequestDto);
        Object result;
        if (pageRequestDto.getContentType() == ContentType.RAW) {
            result = contentService.loadPageContent(pageRequestDto);
        } else {

            result = pageRequestDto.getDataModelType() == DataModelType.R2 ?
                    pageModelService.loadPageModel(pageRequestDto) :
                    JsonDataBinder.getGenericMapper().writeValueAsString(legacyPageModelService.loadLegacyPageModel(pageRequestDto));
        }
        return ResponseEntity.ok(result);
    }

    private PageRequestDto buildPageRequest(String uriType, int localizationId, Optional<Integer> pageId,
                                            PageInclusion pageInclusion, DataModelType dataModelType, boolean isRawContent,
                                            HttpServletRequest request) {

        PageRequestDto.PageRequestDtoBuilder builder;
        if (pageId.isPresent()) {
            builder = PageRequestDto.builder(localizationId, pageId.get());
        } else {
            Optional<String> pageUrl = getPageUrl(request);
            if (!pageUrl.isPresent()) {
                log.warn("Page URL is not found in request URI {}", request.getRequestURI());

                return null;
            }

            builder = PageRequestDto.builder(localizationId, pageUrl.get());
        }

        return builder.uriType(uriType)
                .dataModelType(dataModelType)
                .includePages(pageInclusion)
                .contentType(isRawContent ? RAW : MODEL)
                .build();
    }

    private Optional<String> getPageUrl(HttpServletRequest request) {
        String requestUri = request.getRequestURI().substring(request.getContextPath().length());
        Matcher m = PAGE_URL_REGEX.matcher(requestUri);
        if (m.matches()) {
            return Optional.of(m.group("pageUrl"));
        }

        return Optional.empty();
    }
}
