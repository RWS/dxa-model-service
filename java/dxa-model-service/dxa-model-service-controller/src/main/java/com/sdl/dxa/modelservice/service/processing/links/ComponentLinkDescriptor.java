package com.sdl.dxa.modelservice.service.processing.links;

import com.sdl.dxa.api.datamodel.model.EntityModelData;

import static com.sdl.web.util.ContentServiceQueryConstants.LINK_TYPE_COMPONENT;

public class ComponentLinkDescriptor implements LinkDescriptor {

    private EntityModelData _targetEntity;

    private int _publicationId;

    private String _subscriptionId;

    public ComponentLinkDescriptor(int publicationId, EntityModelData entityModelData) {
        this._targetEntity = entityModelData;
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
        //TODO add publication id to it
        return String.format("%s-%s", this.getPublicationId(), this.getComponentId().toString());
    }

    @Override
    public Integer getComponentId() {
        return Integer.parseInt(this._targetEntity.getId());
    }

    @Override
    public int getPublicationId() {
        return this._publicationId;
    }

    @Override
    public void setLinkUrl(String url) {
        this._targetEntity.setLinkUrl(url);
    }

    @Override
    public String getType() {
        return LINK_TYPE_COMPONENT;
    }
}

