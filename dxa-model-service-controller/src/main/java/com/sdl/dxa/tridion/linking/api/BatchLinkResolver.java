package com.sdl.dxa.tridion.linking.api;

import com.sdl.dxa.tridion.linking.api.descriptors.MultipleLinksDescriptor;
import com.sdl.dxa.tridion.linking.api.descriptors.SingleLinkDescriptor;

public interface BatchLinkResolver {

    void dispatchLinkResolution(SingleLinkDescriptor descriptor);

    /**
     *
     * @param descriptor @see {@link MultipleLinksDescriptor}
     */
    void dispatchMultipleLinksResolution(MultipleLinksDescriptor descriptor);

    /**
     * Method initiates resolution of links that has been dispatched. After resolution has finished it flushes Resolver
     */
    void resolveAndFlush();
}
