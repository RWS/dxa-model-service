package com.sdl.dxa.tridion.linking;

import com.sdl.dxa.common.util.PathUtils;
import com.sdl.dxa.tridion.linking.api.BatchLinkResolver;
import com.sdl.dxa.tridion.linking.api.descriptors.MultipleLinksDescriptor;
import com.sdl.dxa.tridion.linking.api.descriptors.SingleLinkDescriptor;
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
            Integer pageId = descriptor.getPageId();
            SingleLinkDescriptor ld = null;

            if (descriptor.getType().equals(LINK_TYPE_BINARY) || descriptor.getType().equals(LINK_TYPE_COMPONENT)) {
                ld = new ComponentLinkDescriptor(pubId, pageId, new MultipleEntryLinkProcessor(links, linkEntry.getKey()), descriptor.getType());
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

        final Integer pubId = descriptor.getPublicationId();
        final Integer pageId = descriptor.getPageId();
        final Integer componentId = descriptor.getComponentId();

        switch (descriptor.getType()) {
            case LINK_TYPE_PAGE:

                final PageLink pageLink = new PageLink(pubId);
                updateDescriptor(descriptor, pageLink.getLink(pageId));
                break;

            case LINK_TYPE_DYNAMIC_COMPONENT:

                final DynamicComponentLink dynamicComponentLink =
                        new DynamicComponentLink(pubId);
                updateDescriptor(descriptor, dynamicComponentLink
                        .getLink(pageId, componentId, descriptor.getTemplateId(), "",
                                "", false));
                break;
            case LINK_TYPE_BINARY:
                final Link binaryLink = this.resolveBinaryLink(pubId, componentId);

                if (binaryLink.isResolved()) {
                    updateDescriptor(descriptor, binaryLink);
                } else {
                    final Link componentLink = resolveComponentLink(pubId, pageId, componentId);
                    updateDescriptor(descriptor, componentLink);
                }

                break;
            case LINK_TYPE_COMPONENT:
            default:
                final Link componentLink = resolveComponentLink(pubId, pageId, componentId);
                updateDescriptor(descriptor, componentLink);
                break;
        }
    }

    private Link resolveBinaryLink(final Integer publicationId, final Integer componentId) {
        final BinaryLink binaryLink = new BinaryLink(publicationId);
        return binaryLink.getLink(
                TcmUtils.buildTcmUri(publicationId, componentId),
                "",
                "",
                "",
                "",
                false);

    }

    private Link resolveComponentLink(final Integer publicationId, final Integer pageId, final Integer componentId) {
        final ComponentLink componentLink = new ComponentLink(publicationId);
        return componentLink.getLink(
                pageId,
                componentId,
                -1,
                "",
                "",
                false,
                false);
    }

    private void updateDescriptor(final SingleLinkDescriptor descriptor, final Link link) {

        if (link.isResolved()) {

            String resolvedLink = link.getURL();
            String resolvedUrl = shouldStripIndexPath ? PathUtils.stripIndexPath(resolvedLink) : resolvedLink;
            if (shouldKeepTrailingSlash && (! "/".equals(resolvedUrl)) && PathUtils.isIndexPath(resolvedLink)) {
                resolvedUrl = resolvedUrl + "/";
            }

            descriptor.setResolvedLink(
                    this.shouldRemoveExtension ? PathUtils.stripDefaultExtension(resolvedUrl) :
                    resolvedUrl);
        } else {
            descriptor.setResolvedLink("");
        }

        descriptor.update();
    }
}
