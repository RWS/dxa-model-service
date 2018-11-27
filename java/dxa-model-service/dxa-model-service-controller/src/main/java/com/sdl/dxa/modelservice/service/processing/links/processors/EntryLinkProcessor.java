package com.sdl.dxa.modelservice.service.processing.links.processors;

import com.sdl.webapp.common.util.TcmUtils;

import java.util.Map;

public class EntryLinkProcessor implements LinkProcessor {
    private Map<String, String> model;

    private String id;
    private String key;
    private String value;

    public EntryLinkProcessor(Map<String, String> map, String key, String value) {
        this.model = map;

        this.key = key;
        this.value = value;

        this.id = this._createId();
    }

    private String _createId() {
        return Integer.toString(TcmUtils.getItemId(this.value));
    }
    @Override
    public void update(String url) {
        this.model.replace(this.key, this.value);
    }

    @Override
    public String getId() {
        return this.id;
    }
}
