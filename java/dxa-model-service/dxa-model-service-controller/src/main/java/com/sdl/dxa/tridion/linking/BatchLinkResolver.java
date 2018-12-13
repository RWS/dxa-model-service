package com.sdl.dxa.tridion.linking;

import com.sdl.dxa.tridion.linking.descriptors.api.MultipleLinksDescriptor;
import com.sdl.dxa.tridion.linking.descriptors.api.SingleLinkDescriptor;

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
