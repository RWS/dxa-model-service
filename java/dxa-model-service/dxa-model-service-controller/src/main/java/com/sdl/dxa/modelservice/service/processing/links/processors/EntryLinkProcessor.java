package com.sdl.dxa.modelservice.service.processing.links.processors;

import com.sdl.webapp.common.util.TcmUtils;

import java.util.Map;

public class EntryLinkProcessor implements LinkProcessor {
    private Map<String, String> _model;

    private String _id;
    private String _key;
    private String _value;

    public EntryLinkProcessor(Map<String, String> map, String key, String value) {
        this._model = map;

        this._key = key;
        this._value = value;

        this._id = this._createId();
    }

    private String _createId() {
        return Integer.toString(TcmUtils.getItemId(this._value));
    }
    @Override
    public void updateUrl(String url) {
        this._model.replace(this._key, this._value);
    }

    @Override
    public String getId() {
        return this._id;
    }
}
