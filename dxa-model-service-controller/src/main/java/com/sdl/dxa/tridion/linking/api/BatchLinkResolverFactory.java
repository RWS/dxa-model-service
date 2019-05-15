package com.sdl.dxa.tridion.linking.api;

public interface BatchLinkResolverFactory {

    /**
     * Get a batchLinkresolver instance.
     *
     * Batchlinkresolvers can remember state and should not be shared between requests or threads
     *
     * @return
     */
    BatchLinkResolver getBatchLinkResolver();
}
