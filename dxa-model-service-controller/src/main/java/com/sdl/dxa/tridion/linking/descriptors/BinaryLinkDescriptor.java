package com.sdl.dxa.tridion.linking.descriptors;

import com.sdl.dxa.tridion.linking.api.processors.LinkProcessor;

import static com.sdl.web.util.ContentServiceQueryConstants.LINK_TYPE_BINARY;

public class BinaryLinkDescriptor extends BaseLinkDescriptor {

    public BinaryLinkDescriptor(Integer publicationId, Integer sourcePageId, LinkProcessor processor) {
        super(publicationId, sourcePageId, processor, LINK_TYPE_BINARY);
    }

    @Override
    public String getLinkId() {
        return String.format("%s-%s-%s", this.getPublicationId(), this.getPageId(), this.getComponentId().toString());
    }
}

