package com.sdl.dxa.modelservice.controller.api;

import com.sdl.dxa.api.datamodel.model.SitemapItemModelData;
import com.sdl.dxa.common.dto.SitemapRequestDto;
import com.sdl.dxa.modelservice.service.api.navigation.dynamic.DynamicNavigationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.Collections;

@RequestMapping(value = "/api/navigation")
@RestController
public class DynamicNavigationController {

    @Autowired
    private DynamicNavigationProvider dynamicNavigationProvider;

    @RequestMapping
    public ResponseEntity<SitemapItemModelData> navigationModel(@RequestParam(value = "localizationId") Integer localizationId) {
        SitemapRequestDto requestDto = SitemapRequestDto.builder().localizationId(localizationId).build();

        return dynamicNavigationProvider.getNavigationModel(requestDto).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping(value = {"/subtree", "/subtree/{sitemapItemId}"}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Collection<SitemapItemModelData> subtree(
            @PathVariable(value = "sitemapItemId", required = false) String sitemapItemId,
            @RequestParam(value = "includeAncestors", required = false, defaultValue = "false") Boolean includeAncestors,
            @RequestParam(value = "descendantLevels", required = false, defaultValue = "1") Integer descendantLevels) {
        return Collections.singletonList(new SitemapItemModelData().setId(
                String.format("subtree/%s-%s-%s", sitemapItemId, includeAncestors, descendantLevels)));
    }
}
