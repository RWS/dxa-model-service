package com.sdl.dxa.tridion.linking;

import com.sdl.dxa.common.util.PathUtils;
import com.sdl.dxa.tridion.linking.api.BatchLinkResolver;
import com.sdl.dxa.tridion.linking.api.descriptors.MultipleLinksDescriptor;
import com.sdl.dxa.tridion.linking.api.descriptors.SingleLinkDescriptor;
import com.sdl.dxa.tridion.linking.descriptors.BinaryLinkDescriptor;
import com.sdl.dxa.tridion.linking.descriptors.ComponentLinkDescriptor;
import com.sdl.dxa.tridion.linking.processors.MultipleEntryLinkProcessor;
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

    @Value("${dxa.web.link-resolver.strip-index-path:#{true}}")
    private boolean shouldStripIndexPath;

    @Value("${dxa.web.link-resolver.keep-trailing-slash:#{false}}")
    private boolean shouldKeepTrailingSlash;

    @Value("${dxa.web.link-resolver.relative-urls:#{true}}")
    private boolean useRelativeUrls;

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
                ld = new BinaryLinkDescriptor(pubId, new MultipleEntryLinkProcessor(links, linkEntry.getKey()));
            }

            if (descriptor.getType().equals(LINK_TYPE_COMPONENT)) {
                ld = new ComponentLinkDescriptor(pubId, new MultipleEntryLinkProcessor(links, linkEntry.getKey()));
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

                final PageLink pageLink = new PageLink(null, descriptor.getPublicationId(), useRelativeUrls);
                updateDescriptor(descriptor, pageLink.getLink(descriptor.getPageId()));
                break;

            case LINK_TYPE_DYNAMIC_COMPONENT:

                final DynamicComponentLink dynamicComponentLink =
                        new DynamicComponentLink(null, descriptor.getPublicationId(), useRelativeUrls);
                updateDescriptor(descriptor, dynamicComponentLink
                        .getLink(descriptor.getPageId(), descriptor.getComponentId(), descriptor.getTemplateId(), "",
                                "", false));
                break;
            case LINK_TYPE_BINARY:
                Link binaryLink = this.resolveBinaryLink(descriptor);
                if(binaryLink.isResolved()) {
                    updateDescriptor(descriptor, this.resolveBinaryLink(descriptor));
                    break;
                }
            case LINK_TYPE_COMPONENT:
            default:
                final ComponentLink componentLink = new ComponentLink(null, descriptor.getPublicationId(), useRelativeUrls);
                Link resolvedLink = componentLink.getLink(
                        descriptor.getPageId(),
                        descriptor.getComponentId(),
                        -1,
                        "",
                        "",
                        false,
                        false);
                updateDescriptor(descriptor, resolvedLink.isResolved() ? resolvedLink : this.resolveBinaryLink(descriptor));
                break;


        }
    }

    private Link resolveBinaryLink(SingleLinkDescriptor descriptor) {
        final BinaryLink binaryLink = new BinaryLink(null, descriptor.getPublicationId(), useRelativeUrls);
        return binaryLink.getLink(
                TcmUtils.buildTcmUri(descriptor.getPublicationId(), descriptor.getComponentId()),
                "",
                "",
                "",
                "",
                false);
    }

    private void updateDescriptor(final SingleLinkDescriptor descriptor, final Link link) {

        if (link.isResolved()) {

            String resolvedLink = link.getURL();
            String resolvedUrl = shouldStripIndexPath ? PathUtils.stripIndexPath(resolvedLink) : resolvedLink;
            if (shouldKeepTrailingSlash && (!resolvedUrl.equals("/")) && PathUtils.isIndexPath(resolvedLink)) {
                resolvedUrl = resolvedUrl + "/";
            }

            descriptor.update(
                    this.shouldRemoveExtension ? PathUtils.stripDefaultExtension(resolvedUrl) :
                            resolvedUrl);
        } else {
            descriptor.update("");
        }
    }
}
