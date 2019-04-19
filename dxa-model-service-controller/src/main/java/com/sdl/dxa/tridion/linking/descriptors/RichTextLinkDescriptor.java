package com.sdl.dxa.tridion.linking.descriptors;

import com.sdl.dxa.tridion.linking.api.processors.LinkListProcessor;

import java.util.List;

import static com.sdl.web.util.ContentServiceQueryConstants.LINK_TYPE_BINARY;
import static com.sdl.web.util.ContentServiceQueryConstants.LINK_TYPE_COMPONENT;

public class RichTextLinkDescriptor extends BaseMultipleLinksDescriptor {
    private String type = LINK_TYPE_COMPONENT;

    @Override
    public String getType() {
        return this.type;
    }

    public RichTextLinkDescriptor(Integer publicationId, Integer sourcePageId, List<String> links,
                                  LinkListProcessor linkProcessor) {
        super(publicationId, sourcePageId, links, linkProcessor, LINK_TYPE_BINARY);
    }
    @Override
    public void setType(String type) {
        this.type = type;
    }
}

