package com.sdl.dxa.modelservice.service.processing.links;

import com.sdl.dxa.modelservice.service.processing.links.processors.LinkProcessor;

public abstract class BaseLinkDescriptor implements LinkDescriptor {
    private LinkProcessor _linkProcessor;

    private Integer _publicationId;

    private String _subscriptionId;

    BaseLinkDescriptor(int publicationId, LinkProcessor linkProcessor) {
        this._linkProcessor = linkProcessor;
        this._publicationId = publicationId;
    }

    @Override
    public void subscribe(String subscriptionId) {
        this._subscriptionId = subscriptionId;
    }

    @Override
    public String getSubscription() {
        return this._subscriptionId;
    }

    @Override
    public boolean couldBeResolved() {
        return this.getSubscription() != null && !this.getSubscription().isEmpty();
    }

    @Override
    public String getId() {
        return String.format("%s-%s", this.getPublicationId(), this.getComponentId().toString());
    }

    @Override
    public Integer getComponentId() {
        return Integer.parseInt(this._linkProcessor.getId());
    }

    @Override
    public int getPublicationId() {
        return this._publicationId;
    }

    @Override
    public void setLinkUrl(String url) {
        this._linkProcessor.updateUrl(url);
    }
}
