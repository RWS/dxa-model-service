package com.sdl.dxa.tridion.linking;

import com.sdl.dxa.common.util.PathUtils;
import com.sdl.dxa.tridion.linking.descriptors.BinaryLinkDescriptor;
import com.sdl.dxa.tridion.linking.descriptors.ComponentLinkDescriptor;
import com.sdl.dxa.tridion.linking.descriptors.api.MultipleLinksDescriptor;
import com.sdl.dxa.tridion.linking.descriptors.api.SingleLinkDescriptor;
import com.sdl.dxa.tridion.linking.processors.EntryLinkProcessor;
import com.sdl.webapp.common.util.TcmUtils;
import com.tridion.linking.BinaryLink;
import com.tridion.linking.ComponentLink;
import com.tridion.linking.DynamicComponentLink;
import com.tridion.linking.Link;
import com.tridion.linking.PageLink;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;

import static com.sdl.web.util.ContentServiceQueryConstants.LINK_TYPE_BINARY;
import static com.sdl.web.util.ContentServiceQueryConstants.LINK_TYPE_COMPONENT;
import static com.sdl.web.util.ContentServiceQueryConstants.LINK_TYPE_DYNAMIC_COMPONENT;
import static com.sdl.web.util.ContentServiceQueryConstants.LINK_TYPE_PAGE;

/**
 * TridionBatchLinkResolver.
 */
public class TridionBatchLinkResolver implements BatchLinkResolver {

    @Value("${dxa.web.link-resolver.remove-extension:#{true}}")
    private boolean shouldRemoveExtension;

    @Override
    public void dispatchLinkResolution(final SingleLinkDescriptor descriptor) {
        if (descriptor == null) {
            return;
        }

        descriptor.subscribe(descriptor.getLinkId());
        resolveLink(descriptor);
    }

    @Override
    public void dispatchMultipleLinksResolution(final MultipleLinksDescriptor descriptor) {

        if (descriptor == null) {
            return;
        }

        Map<String, String> links = descriptor.getLinks();
        for (Map.Entry<String, String> linkEntry : links.entrySet()) {

            Integer pubId = descriptor.getPublicationId();
            SingleLinkDescriptor ld = null;

            if (descriptor.getType().equals(LINK_TYPE_BINARY)) {
                ld = new BinaryLinkDescriptor(pubId, new EntryLinkProcessor(links, linkEntry.getKey()));
            }

            if (descriptor.getType().equals(LINK_TYPE_COMPONENT)) {
                ld = new ComponentLinkDescriptor(pubId, new EntryLinkProcessor(links, linkEntry.getKey()));
            }



            dispatchLinkResolution(ld);
        }

        descriptor.update(links);
    }

    @Override
    public void resolveAndFlush() {
    }

    private void resolveLink(SingleLinkDescriptor descriptor) {

        if (descriptor == null) {
            return;
        }

        switch (descriptor.getType()) {
            case LINK_TYPE_PAGE:

                final PageLink pageLink = new PageLink(descriptor.getPublicationId());
                updateDescriptor(descriptor, pageLink.getLink(descriptor.getPageId()));
                break;

            case LINK_TYPE_BINARY:

                final BinaryLink binaryLink = new BinaryLink(descriptor.getPublicationId());
                updateDescriptor(descriptor, binaryLink
                        .getLink(TcmUtils.buildTcmUri(descriptor.getPublicationId(), descriptor.getComponentId()), "",
                                "", "", "", false));
                break;

            case LINK_TYPE_DYNAMIC_COMPONENT:

                final DynamicComponentLink dynamicComponentLink =
                        new DynamicComponentLink(descriptor.getPublicationId());
                updateDescriptor(descriptor, dynamicComponentLink
                        .getLink(descriptor.getPageId(), descriptor.getComponentId(), descriptor.getTemplateId(), "",
                                "", false));
                break;
            case LINK_TYPE_COMPONENT:
            default:

                final ComponentLink componentLink = new ComponentLink(descriptor.getPublicationId());
                updateDescriptor(descriptor, componentLink
                        .getLink(descriptor.getPageId(), descriptor.getComponentId(), -1, "", "", false, false));
        }
    }

    private void updateDescriptor(final SingleLinkDescriptor descriptor, final Link link) {

        if (link.isResolved()) {
            descriptor.update(this.shouldRemoveExtension ? PathUtils.stripDefaultExtension(link.getURL()) :
                    link.getURL());
        } else {
            descriptor.update("");
        }
    }
}
