package com.sdl.dxa.tridion.linking.processors;

public interface LinkProcessor {
    void update(String url);

    String getId();
}
