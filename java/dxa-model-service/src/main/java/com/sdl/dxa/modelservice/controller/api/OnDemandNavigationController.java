package com.sdl.dxa.modelservice.controller.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@RequestMapping(value = "/api/navigation/subtree")
@Controller
public class OnDemandNavigationController {

    @GetMapping
    @ResponseBody
    public String handle(@RequestParam(value = "includeAncestors", required = false, defaultValue = "false") Boolean includeAncestors,
                         @RequestParam(value = "descendantLevels", required = false, defaultValue = "1") Integer descendantLevels) {
        return String.format("subtree, includeAncestors = %s, descendantLevels = %s", includeAncestors, descendantLevels);
    }

    @GetMapping(value = "/{sitemapItemId}")
    @ResponseBody
    public String handle(@PathVariable("sitemapItemId") String sitemapItemId,
                         @RequestParam(value = "includeAncestors", required = false, defaultValue = "false") Boolean includeAncestors,
                         @RequestParam(value = "descendantLevels", required = false, defaultValue = "1") Integer descendantLevels) {
        return String.format("subtree/%s, includeAncestors = %s, descendantLevels = %s", sitemapItemId, includeAncestors, descendantLevels);
    }
}
