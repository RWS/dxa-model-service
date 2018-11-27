package com.sdl.dxa.modelservice.service.processing.links;

import com.sdl.dxa.modelservice.service.processing.links.processors.LinkListProcessor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseLinkListDescriptor implements LinkListDescriptor {

    private LinkListProcessor linkProcessor;

    private Map<String, String> links;


    BaseLinkListDescriptor(List<String> links, LinkListProcessor linkProcessor) {
        this.linkProcessor = linkProcessor;

        this.links = this.prepareLinks(links);

    }

    private Map<String, String> prepareLinks(List<String> links) {
        Map<String, String> map = new HashMap<>();
        for (String uri : links) {
            map.put(uri, null);
        }

        return map;
    }

    @Override
    public Map<String, String> getLinks() {
        return this.links;
    }

    public void update(Map<String, String> links) {
        this.linkProcessor.update(links);
    }
}
