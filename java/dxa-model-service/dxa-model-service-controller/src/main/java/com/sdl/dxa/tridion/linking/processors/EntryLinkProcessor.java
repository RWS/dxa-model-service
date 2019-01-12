package com.sdl.dxa.tridion.linking.processors;

import com.sdl.dxa.tridion.linking.api.processors.LinkProcessor;
import com.sdl.webapp.common.util.TcmUtils;

import java.util.Map;

public class EntryLinkProcessor implements LinkProcessor {
    private Map<String, String> model;

    private String id;
    private String key;

    public EntryLinkProcessor(Map<String, String> map, String key) {
        this.model = map;
        this.key = key;
        this.id = this._createId();
    }

    private String _createId() {
        return Integer.toString(TcmUtils.getItemId(this.key));
    }
    @Override
    public void update(String url) {
        this.model.replace(this.key, url);
    }

    @Override
    public String getId() {
        return this.id;
    }
}
