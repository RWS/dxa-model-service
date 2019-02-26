package com.sdl.dxa.tridion.linking.descriptors;

import com.sdl.dxa.tridion.linking.api.processors.LinkProcessor;

import static com.sdl.web.util.ContentServiceQueryConstants.LINK_TYPE_BINARY;

public class BinaryLinkDescriptor extends BaseLinkDescriptor {

    /**
     * Note - required to resolve a component link in case no real binary URL can't be found
     */
    private Integer sourcePageId;

    public BinaryLinkDescriptor(Integer publicationId, Integer sourcePageId,  LinkProcessor processor) {
        super(publicationId, processor, LINK_TYPE_BINARY);
        this.sourcePageId = sourcePageId;
    }

    @Override
    public String getLinkId() {
        return String.format("%s-%s-%s", this.getPublicationId(), this.sourcePageId, this.getComponentId().toString());
    }

    @Override
    public Integer getPageId() {
        return this.sourcePageId;
    }
}

