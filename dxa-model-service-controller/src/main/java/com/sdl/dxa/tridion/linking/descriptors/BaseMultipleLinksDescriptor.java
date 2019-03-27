package com.sdl.dxa.tridion.linking.descriptors;

import com.sdl.dxa.tridion.linking.api.descriptors.MultipleLinksDescriptor;
import com.sdl.dxa.tridion.linking.api.processors.LinkListProcessor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.sdl.web.util.ContentServiceQueryConstants.LINK_TYPE_COMPONENT;

public abstract class BaseMultipleLinksDescriptor implements MultipleLinksDescriptor {

    private String type;

    private LinkListProcessor linkProcessor;

    private Map<String, String> links;

    private Integer publicationId;

    BaseMultipleLinksDescriptor(Integer publicationId, List<String> links, LinkListProcessor linkProcessor, String type) {
        this.publicationId = publicationId;
        this.links = this.prepareLinks(links);
        this.linkProcessor = linkProcessor;
        this.type = type;
    }

    BaseMultipleLinksDescriptor(Integer publicationId, List<String> links, LinkListProcessor linkProcessor) {
        this(publicationId, links, linkProcessor, LINK_TYPE_COMPONENT);
    }

    @Override
    public Integer getPublicationId() {
        return this.publicationId;
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

    @Override
    public String getType() {
        return this.type;
    }
}
