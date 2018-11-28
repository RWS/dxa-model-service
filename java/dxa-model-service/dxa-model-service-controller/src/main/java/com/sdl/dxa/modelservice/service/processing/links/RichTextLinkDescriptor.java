package com.sdl.dxa.modelservice.service.processing.links;

import com.sdl.dxa.modelservice.service.processing.links.processors.LinkListProcessor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.sdl.web.util.ContentServiceQueryConstants.LINK_TYPE_BINARY;
import static com.sdl.web.util.ContentServiceQueryConstants.LINK_TYPE_COMPONENT;

public class RichTextLinkDescriptor extends BaseLinkListDescriptor {


    public RichTextLinkDescriptor(List<String> links, LinkListProcessor linkProcessor) {
        super(links, linkProcessor);
    }

    @Override
    public String getType() {
        return LINK_TYPE_COMPONENT;
    }
}

