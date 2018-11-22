package com.sdl.dxa.modelservice.service.processing.links.processors;

import com.sdl.dxa.api.datamodel.model.EntityModelData;

public class EntityLinkProcessor implements LinkProcessor {
    private EntityModelData _model;

    public EntityLinkProcessor(EntityModelData entityModelData) {
        _model = entityModelData;
    }

    @Override
    public void updateUrl(String url) {
        this._model.setLinkUrl(url);
    }

    @Override
    public String getId() {
        return this._model.getId();
    }
}
