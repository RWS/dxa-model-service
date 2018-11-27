package com.sdl.dxa.modelservice.service.processing.links;

import java.util.Map;

public interface LinkListDescriptor {
    Map<String, String> getLinks();

    void update(Map<String, String> links);

    String getType();
}
