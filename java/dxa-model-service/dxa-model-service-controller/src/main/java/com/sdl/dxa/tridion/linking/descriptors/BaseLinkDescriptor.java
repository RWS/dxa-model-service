package com.sdl.dxa.tridion.linking.descriptors;

import com.sdl.dxa.tridion.linking.descriptors.api.SingleLinkDescriptor;
import com.sdl.dxa.tridion.linking.processors.LinkProcessor;

public abstract class BaseLinkDescriptor implements SingleLinkDescriptor {
    private LinkProcessor linkProcessor;

    private Integer publicationId;

    private String subscriptionId;

    BaseLinkDescriptor(Integer publicationId, LinkProcessor linkProcessor) {
        this.linkProcessor = linkProcessor;
        this.publicationId = publicationId;
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
}
