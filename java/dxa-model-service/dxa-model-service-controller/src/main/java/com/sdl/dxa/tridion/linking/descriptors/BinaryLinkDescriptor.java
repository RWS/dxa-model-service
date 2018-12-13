package com.sdl.dxa.tridion.linking.descriptors;

import com.sdl.dxa.tridion.linking.processors.LinkProcessor;

import static com.sdl.web.util.ContentServiceQueryConstants.LINK_TYPE_BINARY;

public class BinaryLinkDescriptor extends BaseLinkDescriptor {

    public BinaryLinkDescriptor(Integer publicationId, LinkProcessor processor) {
        super(publicationId, processor);
    }

    @Override
    public String getType() {
        return LINK_TYPE_BINARY;
    }
}

