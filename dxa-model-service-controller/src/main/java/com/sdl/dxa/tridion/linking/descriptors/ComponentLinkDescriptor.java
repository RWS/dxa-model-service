package com.sdl.dxa.tridion.linking.descriptors;

import com.sdl.dxa.tridion.linking.api.processors.LinkProcessor;

public class ComponentLinkDescriptor extends BaseLinkDescriptor {

    public ComponentLinkDescriptor(Integer publicationId, Integer sourcePageId, LinkProcessor linkProcessor, String type) {
        super(publicationId, sourcePageId, linkProcessor, type);
    }

    @Override
    public String getLinkId() {
        return String.format("%s-%s-%s", this.getPublicationId(), this.getPageId(), this.getComponentId().toString());
    }
}

