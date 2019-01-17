package com.sdl.dxa.tridion.linking.descriptors;

import com.sdl.dxa.tridion.linking.processors.LinkListProcessor;

import java.util.List;

import static com.sdl.web.util.ContentServiceQueryConstants.LINK_TYPE_COMPONENT;

public class RichTextLinkDescriptor extends BaseMultipleLinksDescriptor {
    public RichTextLinkDescriptor(Integer publicationId, List<String> links, LinkListProcessor linkProcessor) {
        super(publicationId, links, linkProcessor);
    }

    @Override
    public String getType() {
        return LINK_TYPE_COMPONENT;
    }
}

