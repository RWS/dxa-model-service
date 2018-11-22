package com.sdl.dxa.modelservice.service.processing.links;

import org.jetbrains.annotations.Contract;

public interface BatchLinkResolver {

    /**
     * Adds link in a batch to be resolved later.
     *
     * @param descriptor             The TCM URI to resolve.
     * @return The translated URL.
     */
    @Contract("null, _ -> null; !null, _ -> !null")
    void dispatchLinkResolution(LinkDescriptor descriptor);

    /**
     * Initiates resolution.
     *
     */
    void resolveAndFlush();
}
