package com.sdl.dxa.tridion.linking.descriptors;

import com.sdl.dxa.tridion.linking.api.descriptors.SingleLinkDescriptor;
import com.sdl.dxa.tridion.linking.api.processors.LinkProcessor;

import static com.sdl.web.util.ContentServiceQueryConstants.LINK_TYPE_COMPONENT;

public abstract class BaseLinkDescriptor implements SingleLinkDescriptor {
    private String type;

    private LinkProcessor linkProcessor;

    private Integer publicationId;

    private String subscriptionId;

    protected BaseLinkDescriptor(Integer publicationId, LinkProcessor linkProcessor, String type) {
        this.linkProcessor = linkProcessor;
        this.publicationId = publicationId;
        this.type = type;
    }

    BaseLinkDescriptor(Integer publicationId, LinkProcessor linkProcessor) {
        this(publicationId, linkProcessor, LINK_TYPE_COMPONENT);
    }

    @Override
    public void subscribe(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    @Override
    public void update(String url) {
        this.linkProcessor.update(url);
    }

    @Override
    public boolean couldBeResolved() {
        return this.getSubscription() != null && !this.getSubscription().isEmpty();
    }

    @Override
    public String getLinkId() {
        return String.format("%s-%s", this.getPublicationId(), this.getComponentId().toString());
    }

    @Override
    public String getSubscription() {
        return this.subscriptionId;
    }

    @Override
    public Integer getComponentId() {
        return Integer.parseInt(this.linkProcessor.getId());
    }

    @Override
    public Integer getPublicationId() {
        return this.publicationId;
    }

    @Override
    public String getType() {
        return this.type;
    }

    @Override
    public LinkProcessor getLinkProcessor() {
        return this.linkProcessor;
    }
}
