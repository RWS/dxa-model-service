package com.sdl.dxa.modelservice.service.caching;

import com.sdl.dxa.api.datamodel.model.EntityModelData;

import java.util.HashMap;
import java.util.Map;

public class EntityCache <KType, VType>{

    private Map<KType, VType> store = new HashMap<>();

    public Map<KType, VType> getStore() {
        return store;
    }


    public VType get(KType key) {
        return store.get(key);
    }

    public VType put(KType key, VType value) {
        return store.put(key, value);
    }

    public void clear() {
        store.clear();
    }
}
