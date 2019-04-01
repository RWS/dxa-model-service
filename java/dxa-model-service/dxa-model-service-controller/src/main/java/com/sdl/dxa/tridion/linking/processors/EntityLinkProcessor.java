package com.sdl.dxa.tridion.linking.processors;

import com.sdl.dxa.api.datamodel.model.EntityModelData;
import com.sdl.dxa.tridion.linking.api.processors.LinkProcessor;

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
