package com.sdl.dxa.tridion.linking.descriptors;

import com.sdl.dxa.tridion.linking.api.descriptors.SingleLinkDescriptor;
import com.sdl.dxa.tridion.linking.api.processors.LinkProcessor;

import javax.validation.constraints.NotNull;

public abstract class BaseLinkDescriptor implements SingleLinkDescriptor {
    private LinkProcessor linkProcessor;

    private Integer publicationId;

    private Integer sourcePageId;

    private String subscriptionId;

    private String type;

    private String resolvedLink;

    private boolean resolved = false;

    BaseLinkDescriptor(Integer publicationId, Integer sourcePageId, @NotNull LinkProcessor linkProcessor, String type) {
        this.publicationId = publicationId;
        this.sourcePageId = sourcePageId;
        this.linkProcessor = linkProcessor;
        this.type = type;
    }

    @Override
    public void subscribe(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    @Override
    public void update() {
        this.linkProcessor.update(this.getResolvedLink());
    }

    @Override
    public boolean canBeResolved() {
        return this.getSubscription() != null && !this.getSubscription().isEmpty();
    }

    @Override
    public String getLinkId() {
        return String.format("%s:%s-%s", this.getIdPrefix(), this.getPublicationId(), this.getComponentId().toString());
    }

    @Override
    public String getSubscription() {
        return this.subscriptionId;
    }

    @Override
    public Integer getPublicationId() {
        return this.publicationId;
    }

    @Override
    public Integer getPageId() {
        return this.sourcePageId;
    }

    @Override
    public LinkProcessor getLinkProcessor() {
        return this.linkProcessor;
    }

    @Override
    public String getType() { return this.type; }

    @Override
    public void setType(String type) { this.type = type; }

    @Override
    public String getResolvedLink() { return this.resolvedLink; }

    @Override
    public void setResolvedLink(String link) {
        this.resolvedLink = link;
        this.resolved = true;
    }

    @Override
    public boolean isResolved() {
        return this.resolved;
    }

    String getIdPrefix() {
        return this.getType().toLowerCase();
    }
}
