package com.sdl.dxa.modelservice.controller;

import com.sdl.dxa.api.datamodel.model.PageModelData;
import com.sdl.dxa.common.dto.PageRequestDto;
import com.sdl.dxa.common.dto.PageRequestDto.PageInclusion;
import com.sdl.dxa.modelservice.service.PageModelService;
import com.sdl.webapp.common.api.content.ContentProviderException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
     * <p>Explanation:</p>
     * <ol>
     * <li>Starts with {@code /}</li>
     * <li>Followed by not {@code /}</li>
     * <li>1 & 2 repeats 3 times: {@code /PageModel}, {@code /tcm}, {@code /42}</li>
     * <li>Page path can start with {@code /} or {@code //}, so {@code /example/path/to/site} or {@code //example/path/to/site} both are ok</li>
     * </ol>
     */
    private static final Pattern PAGE_URL_REGEX = Pattern.compile("(/[^\\d]+)+?/\\d+/?(?<pageUrl>/.*)", Pattern.CASE_INSENSITIVE);

    private final PageModelService contentService;

    @Autowired
    public PageModelController(PageModelService contentService) {
        this.contentService = contentService;
    }

    @RequestMapping(
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<PageModelData> getPageModel(@PathVariable String uriType,
                                                      @PathVariable int localizationId,
                                                      @RequestParam(value = "includes", required = false, defaultValue = "INCLUDE") PageInclusion pageInclusion,
                                                      HttpServletRequest request) throws ContentProviderException {
        Optional<String> pageUrl = getPageUrl(request);
        if(pageUrl.isPresent()) {
            PageRequestDto pageRequest = PageRequestDto.builder()
                    .publicationId(localizationId)
                    .uriType(uriType)
                    .path(pageUrl.get())
                    .includePages(pageInclusion)
                    .contentType(MODEL)
                    .build();

            log.trace("requesting pageModel with {}", pageRequest);
            return new ResponseEntity<>(contentService.loadPageModel(pageRequest), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    private Optional<String> getPageUrl(HttpServletRequest request) {
        Matcher m = PageModelController.PAGE_URL_REGEX.matcher(request.getRequestURI().substring(request.getContextPath().length()));
        if(m.matches()) {
            return Optional.of(m.group("pageUrl"));
        }

        return Optional.empty();
    }

    @RequestMapping(
            method = RequestMethod.GET,
            params = "raw"
    )
    public ResponseEntity<String> getPageSource(@PathVariable String uriType,
                                @PathVariable int localizationId,
                                @RequestParam(value = "includes", required = false, defaultValue = "INCLUDE") PageInclusion pageInclusion,
                                HttpServletRequest request) throws ContentProviderException {
        Optional<String> pageUrl = getPageUrl(request);
        if (pageUrl.isPresent()) {
            PageRequestDto pageRequest = PageRequestDto.builder()
                    .publicationId(localizationId)
                    .uriType(uriType)
                    .path(pageUrl.get())
                    .includePages(pageInclusion)
                    .contentType(RAW)
                    .build();
            log.trace("requesting pageSource with {}", pageRequest);
            return new ResponseEntity<>(contentService.loadPageContent(pageRequest), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
}
