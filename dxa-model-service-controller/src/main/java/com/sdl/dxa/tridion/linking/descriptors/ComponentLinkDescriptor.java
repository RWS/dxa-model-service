package com.sdl.dxa.tridion.linking.descriptors;

import com.sdl.dxa.tridion.linking.api.processors.LinkProcessor;

import static com.sdl.web.util.ContentServiceQueryConstants.LINK_TYPE_BINARY;

public class ComponentLinkDescriptor extends BaseLinkDescriptor {

    String type = LINK_TYPE_BINARY;

    private Integer sourcePageId;

    public ComponentLinkDescriptor(Integer publicationId, Integer sourcePageId, LinkProcessor linkProcessor) {
        super(publicationId, linkProcessor);
        this.sourcePageId = sourcePageId;
    }

    @Override
    public String getLinkId() {
        return String.format("%s-%s-%s", this.getPublicationId(), this.sourcePageId, this.getComponentId().toString());
    }

    @Override
    public Integer getPageId() {
        return this.sourcePageId;
    }

    public String getType() {
        return this.type;
    }
    public void setType(String type) {
        this.type = type;
    }
}

