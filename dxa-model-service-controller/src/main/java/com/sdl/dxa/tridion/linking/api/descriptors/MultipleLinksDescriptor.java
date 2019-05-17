package com.sdl.dxa.tridion.linking.api.descriptors;

import java.util.Map;

/**
 * MultipleLinksDescriptor holds list of the links of particular type and exposes interface for .
 */
public interface MultipleLinksDescriptor extends LinkDescriptor {

    /**
     * Returns map that holds TCM ids
     *
     * @return map that contains map of pairs like { "tcm:1-16" -> null }, for further resolution
     */
    Map<String, String> getLinks();

    /**
     * Represents logic that updates source where the links has been retrieved from with the resolved urls.
     */
    void update();
}
