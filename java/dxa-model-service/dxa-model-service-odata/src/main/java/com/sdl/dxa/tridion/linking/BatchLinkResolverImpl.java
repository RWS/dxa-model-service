package com.sdl.dxa.tridion.linking;

import com.sdl.dxa.common.util.PathUtils;
import com.sdl.dxa.tridion.linking.api.BatchLinkResolver;
import com.sdl.dxa.tridion.linking.descriptors.BinaryLinkDescriptor;
import com.sdl.dxa.tridion.linking.descriptors.ComponentLinkDescriptor;
import com.sdl.dxa.tridion.linking.api.descriptors.MultipleLinksDescriptor;
import com.sdl.dxa.tridion.linking.api.descriptors.SingleLinkDescriptor;
import com.sdl.dxa.tridion.linking.processors.EntryLinkProcessor;
import com.sdl.web.api.linking.BatchLinkRequest;
import com.sdl.web.api.linking.BatchLinkRequestImpl;
import com.sdl.web.api.linking.BatchLinkRetriever;
import com.sdl.web.api.linking.BatchLinkRetrieverImpl;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import static com.sdl.web.util.ContentServiceQueryConstants.LINK_TYPE_BINARY;
import static com.sdl.web.util.ContentServiceQueryConstants.LINK_TYPE_COMPONENT;
import static com.sdl.web.util.ContentServiceQueryConstants.LINK_TYPE_DYNAMIC_COMPONENT;
import static com.sdl.web.util.ContentServiceQueryConstants.LINK_TYPE_PAGE;

@Component
@Scope(value = "prototype")
public class BatchLinkResolverImpl implements BatchLinkResolver {

    @Value("${dxa.web.link-resolver.remove-extension:#{true}}")
    private boolean shouldRemoveExtension;

    @Value("${dxa.web.link-resolver.strip-index-path:#{true}}")
    private boolean shouldStripIndexPath;

    @Value("${dxa.web.link-resolver.keep-trailing-slash:#{false}}")
    private boolean shouldKeepTrailingSlash;

    private BatchLinkRetriever retriever;

    private ConcurrentMap<String, List<SingleLinkDescriptor>> subscribers = new ConcurrentHashMap<>();

    private ConcurrentLinkedQueue<ImmutablePair<MultipleLinksDescriptor, Map<String, String>>> subscriberLists
            = new ConcurrentLinkedQueue<>();

    public BatchLinkResolverImpl() {
        this.retriever = new BatchLinkRetrieverImpl();
    }

    @Override
    public void dispatchLinkResolution(SingleLinkDescriptor descriptor) {

        if(descriptor == null) {
            return;
        }

        List<SingleLinkDescriptor> descriptors = this.subscribers.computeIfAbsent(descriptor.getLinkId(), k -> new ArrayList<>());

        // Plan link for resolution only once per unique ID
        if(descriptors.isEmpty()) {
            descriptor.subscribe(this.retriever.addLinkRequest(createBatchLinkRequest(descriptor)));
        }
        descriptors.add(descriptor);
    }

    @Override
    public void dispatchMultipleLinksResolution(MultipleLinksDescriptor descriptor) {
        Map<String, String> links = descriptor.getLinks();
        for (Map.Entry<String, String> linkEntry : links.entrySet()) {

            Integer pubId = descriptor.getPublicationId();
            SingleLinkDescriptor ld = null;

            if (descriptor.getType() == LINK_TYPE_BINARY) {
                ld = new BinaryLinkDescriptor(pubId, new EntryLinkProcessor(links, linkEntry.getKey()));
            }

            if (descriptor.getType() == LINK_TYPE_COMPONENT) {
                ld = new ComponentLinkDescriptor(pubId, new EntryLinkProcessor(links, linkEntry.getKey()));
            }

            dispatchLinkResolution(ld);
        }
        this.subscriberLists.add(new ImmutablePair<>(descriptor, links));
    }

    @Override
    public void resolveAndFlush() {
        this.resolve();
        this.flush();
    }

    private void resolve() {
        this.retriever.executeRequest();
    }

    private void flush() {
        this.updateRefs();
        this.updateLists();
        this.retriever.clearRequestData();
    }

    private void updateLists() {
        for (ImmutablePair<MultipleLinksDescriptor, Map<String, String>> entry : subscriberLists) {
            MultipleLinksDescriptor descriptor = entry.getLeft();
            descriptor.update(entry.getRight());
        }

        this.subscriberLists.clear();
    }

    private void updateRefs() {
        for (List<SingleLinkDescriptor> descriptors : this.subscribers.values()) {
            for (SingleLinkDescriptor descriptor : descriptors) {
                if (descriptor != null && descriptor.couldBeResolved()) {
                    String resolvedUrl = this.retriever.getLink(descriptor.getSubscription()).getURL();



                    if (resolvedUrl != null) {

                        String resolvedLink = shouldStripIndexPath ? PathUtils.stripIndexPath(resolvedUrl) : resolvedUrl;
                        if (shouldKeepTrailingSlash && (! resolvedUrl.equals("/")) && PathUtils.isIndexPath(resolvedUrl)) {
                            resolvedLink = resolvedLink + "/";
                        }

                        descriptor.update(this.shouldRemoveExtension ? PathUtils.stripDefaultExtension(resolvedLink) :
                                resolvedLink);
                    } else {
                        descriptor.update("");
                    }
                }
            }
        }

        this.subscribers.clear();
    }

    private BatchLinkRequest createBatchLinkRequest(SingleLinkDescriptor descriptor) {
        BatchLinkRequest request;
        switch (descriptor.getType()) {
            case LINK_TYPE_PAGE:
                request = new BatchLinkRequestImpl.PageLinkRequestBuilder()
                        .withTargetPageId(descriptor.getPageId())
                        .withPublicationId(descriptor.getPublicationId())
                        .build();
                break;
            case LINK_TYPE_BINARY:
                request = new BatchLinkRequestImpl.BinaryLinkRequestBuilder()
                        .withBinaryComponentId(descriptor.getComponentId())
                        .withPublicationId(descriptor.getPublicationId())
                        .build();
                break;
            case LINK_TYPE_DYNAMIC_COMPONENT:
                request = new BatchLinkRequestImpl.DynamicComponentLinkRequestBuilder()
                        .withTargetPageId(descriptor.getPageId())
                        .withPublicationId(descriptor.getPublicationId())
                        .withTargetTemplateId(descriptor.getTemplateId())
                        .withTargetComponentId(descriptor.getComponentId())
                        .withShowTextOnFail(false)
                        .build();
                break;
            case LINK_TYPE_COMPONENT:
            default:
                request = new BatchLinkRequestImpl.ComponentLinkRequestBuilder()
                        .withExcludeTemplateId(-1)
                        .withSourcePageId(descriptor.getPageId())
                        .withPublicationId(descriptor.getPublicationId())
                        .withTargetComponentId(descriptor.getComponentId())
                        .withShowTextOnFail(false)
                        .withShowAnchor(false)
                        .build();

        }

        return request;
    }
}
