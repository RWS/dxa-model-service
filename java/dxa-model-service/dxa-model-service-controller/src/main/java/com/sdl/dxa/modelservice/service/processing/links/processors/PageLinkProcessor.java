package com.sdl.dxa.modelservice.service.processing.links.processors;

import com.sdl.dxa.api.datamodel.model.PageModelData;

public class PageLinkProcessor implements LinkProcessor {
    private PageModelData _model;

    public PageLinkProcessor(PageModelData pageModelData) {
        _model = pageModelData;
    }

    @Override
    public void updateUrl(String url) {
        this._model.setUrlPath(url);
    }

    @Override
    public String getId() {
        return this._model.getId();
    }
}
