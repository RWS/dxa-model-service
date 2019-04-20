package com.sdl.dxa.tridion.linking.descriptors;

import com.sdl.dxa.tridion.linking.api.processors.LinkProcessor;

import static com.sdl.web.util.ContentServiceQueryConstants.LINK_TYPE_BINARY;
import static com.sdl.web.util.ContentServiceQueryConstants.LINK_TYPE_COMPONENT;

public class ComponentLinkDescriptor extends BaseLinkDescriptor {
    public ComponentLinkDescriptor(Integer publicationId, Integer sourcePageId, LinkProcessor linkProcessor) {
        super(publicationId, sourcePageId, linkProcessor, LINK_TYPE_COMPONENT);
    }

    @Override
    public String getLinkId() {
        return String.format("%s-%s-%s", this.getPublicationId(), this.getPageId(), this.getComponentId().toString());
    }
}

