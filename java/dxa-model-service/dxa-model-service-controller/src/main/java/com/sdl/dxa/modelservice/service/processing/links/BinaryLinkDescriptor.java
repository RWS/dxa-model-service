package com.sdl.dxa.modelservice.service.processing.links;

import com.sdl.dxa.modelservice.service.processing.links.processors.LinkProcessor;

import static com.sdl.web.util.ContentServiceQueryConstants.LINK_TYPE_BINARY;

public class BinaryLinkDescriptor extends BaseLinkDescriptor {

    public BinaryLinkDescriptor(int publicationId, LinkProcessor processor) {
        super(publicationId, processor);
    }

    @Override
    public String getType() {
        return LINK_TYPE_BINARY;
    }
}

