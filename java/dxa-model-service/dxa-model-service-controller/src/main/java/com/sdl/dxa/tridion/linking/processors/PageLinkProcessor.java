package com.sdl.dxa.tridion.linking.processors;

import com.sdl.dxa.api.datamodel.model.PageModelData;

public class PageLinkProcessor implements LinkProcessor {
    private PageModelData model;

    public PageLinkProcessor(PageModelData pageModelData) {
        model = pageModelData;
    }

    @Override
    public void update(String url) {
        this.model.setUrlPath(url);
    }

    @Override
    public String getId() {
        return this.model.getId();
    }
}
