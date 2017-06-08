package com.sdl.dxa.modelservice.controller.api;

import com.sdl.dxa.api.datamodel.model.SitemapItemModelData;
import com.sdl.dxa.api.datamodel.model.TaxonomyNodeModelData;
import com.sdl.dxa.common.dto.DepthCounter;
import com.sdl.dxa.common.dto.SitemapRequestDto;
import com.sdl.dxa.tridion.navigation.dynamic.NavigationModelProvider;
import com.sdl.dxa.tridion.navigation.dynamic.OnDemandNavigationModelProvider;
import com.sdl.webapp.common.api.navigation.NavigationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RequestMapping(value = "/api/navigation/{localizationId}")
@RestController
public class DynamicNavigationController {

    private final NavigationModelProvider navigationModelProvider;

    private final OnDemandNavigationModelProvider onDemandNavigationModelProvider;

    @Autowired
    public DynamicNavigationController(NavigationModelProvider navigationModelProvider,
                                       OnDemandNavigationModelProvider onDemandNavigationModelProvider) {
        this.navigationModelProvider = navigationModelProvider;
        this.onDemandNavigationModelProvider = onDemandNavigationModelProvider;
    }

    @RequestMapping
    public ResponseEntity<TaxonomyNodeModelData> navigationModel(@PathVariable(value = "localizationId", required = false) Integer localizationId) {
        SitemapRequestDto requestDto = SitemapRequestDto.wholeTree(localizationId).build();

        return navigationModelProvider.getNavigationModel(requestDto).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping(value = {"/subtree", "/subtree/{sitemapItemId}"}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Collection<SitemapItemModelData>> subtree(
            @PathVariable(value = "localizationId", required = false) Integer localizationId,
            @PathVariable(value = "sitemapItemId", required = false) String sitemapItemId,
            @RequestParam(value = "includeAncestors", required = false, defaultValue = "false") Boolean includeAncestors,
            @RequestParam(value = "descendantLevels", required = false, defaultValue = "1") Integer descendantLevels) {
        NavigationFilter navigationFilter = new NavigationFilter();
        navigationFilter.setDescendantLevels(descendantLevels);
        navigationFilter.setWithAncestors(includeAncestors);

        return onDemandNavigationModelProvider.getNavigationSubtree(
                SitemapRequestDto.builder(localizationId)
                        .sitemapId(sitemapItemId)
                        .navigationFilter(navigationFilter)
                        .localizationId(localizationId)
                        .expandLevels(new DepthCounter(descendantLevels))
                        .build())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
