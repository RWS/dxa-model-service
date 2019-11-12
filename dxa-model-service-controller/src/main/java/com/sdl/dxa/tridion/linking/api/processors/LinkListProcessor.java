package com.sdl.dxa.tridion.linking.api.processors;

import java.util.Map;
import java.util.Set;

public interface LinkListProcessor {
    void update(Map<String, String> links, Set<String> notResolvedLinks);
}
