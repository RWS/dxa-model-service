package com.sdl.dxa.tridion.linking.descriptors;

import com.sdl.dxa.tridion.linking.processors.LinkProcessor;

import static com.sdl.web.util.ContentServiceQueryConstants.LINK_TYPE_COMPONENT;

public class ComponentLinkDescriptor extends BaseLinkDescriptor {

    public ComponentLinkDescriptor(Integer publicationId, LinkProcessor linkProcessor) {
        super(publicationId, linkProcessor);
    }

    @Override
    public String getType() {
        return LINK_TYPE_COMPONENT;
    }
}

