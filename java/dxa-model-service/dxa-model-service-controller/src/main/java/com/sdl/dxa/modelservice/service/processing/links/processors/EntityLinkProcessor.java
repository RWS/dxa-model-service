package com.sdl.dxa.modelservice.service.processing.links.processors;

import com.sdl.dxa.api.datamodel.model.EntityModelData;

public class EntityLinkProcessor implements LinkProcessor {
    private EntityModelData model;

    public EntityLinkProcessor(EntityModelData entityModelData) {
        model = entityModelData;
    }

    @Override
    public void update(String url) {
        this.model.setLinkUrl(url);
    }

    @Override
    public String getId() {
        return this.model.getId();
    }
}
