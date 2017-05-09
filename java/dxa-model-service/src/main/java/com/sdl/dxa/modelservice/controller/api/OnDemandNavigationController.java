package com.sdl.dxa.modelservice.controller.api;

import com.sdl.dxa.api.datamodel.model.SitemapItemModelData;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collection;
import java.util.Collections;

@RequestMapping(value = "/api/navigation/subtree")
@Controller
public class OnDemandNavigationController {

    @GetMapping
    @ResponseBody
    public Collection<SitemapItemModelData> handle(@RequestParam(value = "includeAncestors", required = false, defaultValue = "false") Boolean includeAncestors,
                                                   @RequestParam(value = "descendantLevels", required = false, defaultValue = "1") Integer descendantLevels) {
        return Collections.singletonList(new SitemapItemModelData().setId(String.format("subtree-%s-%s", includeAncestors, descendantLevels)));
    }

    @GetMapping(value = "/{sitemapItemId}")
    @ResponseBody
    public Collection<SitemapItemModelData> handle(@PathVariable("sitemapItemId") String sitemapItemId,
                                                   @RequestParam(value = "includeAncestors", required = false, defaultValue = "false") Boolean includeAncestors,
                                                   @RequestParam(value = "descendantLevels", required = false, defaultValue = "1") Integer descendantLevels) {
        return Collections.singletonList(new SitemapItemModelData().setId(String.format("subtree/%s-%s-%s", sitemapItemId, includeAncestors, descendantLevels)));
    }
}
