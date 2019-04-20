package com.sdl.dxa.tridion.linking.descriptors;

import com.sdl.dxa.tridion.linking.api.descriptors.SingleLinkDescriptor;
import com.sdl.dxa.tridion.linking.api.processors.LinkProcessor;

import javax.validation.constraints.NotNull;

import static com.sdl.web.util.ContentServiceQueryConstants.LINK_TYPE_COMPONENT;

public abstract class BaseLinkDescriptor implements SingleLinkDescriptor {
    private LinkProcessor linkProcessor;

    private Integer publicationId;

    private Integer sourcePageId;

    private String subscriptionId;

    private String type;

    protected BaseLinkDescriptor(Integer publicationId, Integer sourcePageId, @NotNull LinkProcessor linkProcessor, String type) {
        this.publicationId = publicationId;
        this.sourcePageId = sourcePageId;
        this.linkProcessor = linkProcessor;
        this.type = type;
    }

    BaseLinkDescriptor(Integer publicationId, int sourcePageId, LinkProcessor linkProcessor) {
        this(publicationId, sourcePageId, linkProcessor, LINK_TYPE_COMPONENT);
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
    public boolean canBeResolved() {
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
    public Integer getPageId() {
        return this.sourcePageId;
    }

    @Override
    public LinkProcessor getLinkProcessor() {
        return this.linkProcessor;
    }

    @Override
    public String getType() {
        return this.type;
    }
}
