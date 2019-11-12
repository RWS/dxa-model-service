package com.sdl.dxa.tridion.linking.api;

import com.sdl.dxa.tridion.linking.api.descriptors.MultipleLinksDescriptor;
import com.sdl.dxa.tridion.linking.api.descriptors.SingleLinkDescriptor;

import java.util.Set;

public interface BatchLinkResolver {

    void dispatchLinkResolution(SingleLinkDescriptor descriptor);

    /**
     *
     * @param descriptor @see {@link MultipleLinksDescriptor}
     * @param notResolvedLinks
     */
    void dispatchMultipleLinksResolution(MultipleLinksDescriptor descriptor, Set<String> notResolvedLinks);

    /**
     * Method initiates resolution of links that has been dispatched. After resolution has finished it flushes Resolver
     * @param notResolvedLinks
     */
    void resolveAndFlush(Set<String> notResolvedLinks);
}
