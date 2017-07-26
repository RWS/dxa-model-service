package com.sdl.dxa.modelservice.controller;

import com.sdl.dxa.api.datamodel.model.PageModelData;
import com.sdl.dxa.common.dto.PageRequestDto;
import com.sdl.dxa.common.dto.PageRequestDto.DataModelType;
import com.sdl.dxa.common.dto.PageRequestDto.PageInclusion;
import com.sdl.dxa.modelservice.service.PageModelService;
import com.sdl.webapp.common.api.content.ContentProviderException;
import lombok.extern.slf4j.Slf4j;
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

import static com.sdl.dxa.common.dto.PageRequestDto.ContentType.MODEL;
import static com.sdl.dxa.common.dto.PageRequestDto.ContentType.RAW;

@Slf4j
@RestController
@RequestMapping(value = "/PageModel/{uriType}/{localizationId}/**")
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

    private final PageModelService contentService;

    @Autowired
    public PageModelController(PageModelService contentService) {
        this.contentService = contentService;
    }

    @RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<PageModelData> getPageModel(@PathVariable String uriType,
                                                      @PathVariable int localizationId,
                                                      @RequestParam(value = "includes", required = false, defaultValue = "INCLUDE") PageInclusion pageInclusion,
                                                      @RequestParam(value = "modelType", required = false, defaultValue = "R2") DataModelType dataModelType,
                                                      HttpServletRequest request) throws ContentProviderException {


        PageRequestDto pageRequestDto = buildPageRequest(uriType, localizationId, pageInclusion, dataModelType, MODEL, request);

        if (pageRequestDto == null) {
            return ResponseEntity.badRequest().build();
        }

        log.trace("requesting pageModel with {}", pageRequestDto);
        return ResponseEntity.ok(contentService.loadPageModel(pageRequestDto));
    }

    @RequestMapping(params = "raw")
    public ResponseEntity<String> getPageSource(@PathVariable String uriType,
                                                @PathVariable int localizationId,
                                                @RequestParam(value = "includes", required = false, defaultValue = "INCLUDE") PageInclusion pageInclusion,
                                                @RequestParam(value = "modelType", required = false, defaultValue = "R2") DataModelType dataModelType,
                                                HttpServletRequest request) throws ContentProviderException {
        PageRequestDto pageRequestDto = buildPageRequest(uriType, localizationId, pageInclusion, dataModelType, RAW, request);

        if (pageRequestDto == null) {
            return ResponseEntity.badRequest().build();
        }

        log.trace("requesting pageSource with {}", pageRequestDto);
        return ResponseEntity.ok(contentService.loadPageContent(pageRequestDto));
    }

    private PageRequestDto buildPageRequest(String uriType, int localizationId, PageInclusion pageInclusion,
                                            DataModelType dataModelType, PageRequestDto.ContentType contentType,
                                            HttpServletRequest request) {
        Optional<String> pageUrl = getPageUrl(request);
        if (!pageUrl.isPresent()) {
            log.warn("Page URL is not found in request URI {}", request.getRequestURI());

            return null;
        }

        return PageRequestDto.builder()
                .publicationId(localizationId)
                .uriType(uriType)
                .path(pageUrl.get())
                .dataModelType(dataModelType)
                .includePages(pageInclusion)
                .contentType(contentType)
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
