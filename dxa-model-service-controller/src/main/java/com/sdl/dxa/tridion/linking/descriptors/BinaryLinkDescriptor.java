package com.sdl.dxa.tridion.linking.descriptors;

import com.sdl.dxa.tridion.linking.api.processors.LinkProcessor;

import static com.sdl.web.util.ContentServiceQueryConstants.LINK_TYPE_BINARY;

public class BinaryLinkDescriptor extends BaseLinkDescriptor {
    public BinaryLinkDescriptor(Integer publicationId, LinkProcessor processor) {
        super(publicationId, processor, LINK_TYPE_BINARY);
    }
}

