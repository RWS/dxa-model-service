package com.sdl.dxa.tridion.linking.descriptors;

import com.sdl.dxa.tridion.linking.api.processors.LinkProcessor;

public class ComponentLinkDescriptor extends BaseLinkDescriptor {

    private Integer componentId;

    public ComponentLinkDescriptor(Integer publicationId, Integer sourcePageId, int componentId, LinkProcessor linkProcessor, String type) {
        super(publicationId, sourcePageId, linkProcessor, type);
        this.componentId = componentId;
    }

    @Override
    public String getLinkId() {
        return String.format("%s:%s-%s-%s", this.getIdPrefix(), this.getPublicationId(), this.getPageId(), this.getComponentId().toString());
    }

    @Override
    public Integer getComponentId() {
        return componentId;
    }
}

