package com.sdl.dxa.tridion.linking;


import com.sdl.dxa.common.util.PathUtils;
import com.sdl.dxa.tridion.linking.api.BatchLinkResolver;
import com.sdl.dxa.tridion.linking.api.descriptors.MultipleLinksDescriptor;
import com.sdl.dxa.tridion.linking.api.descriptors.SingleLinkDescriptor;
import com.sdl.dxa.tridion.linking.api.processors.LinkProcessor;
import com.sdl.dxa.tridion.linking.descriptors.ComponentLinkDescriptor;
import com.sdl.dxa.tridion.linking.processors.MultipleEntryLinkProcessor;
import com.sdl.web.api.linking.BatchLinkRequest;
import com.sdl.web.api.linking.BatchLinkRequestImpl;
import com.sdl.web.api.linking.BatchLinkRetriever;
import com.sdl.web.api.linking.Link;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
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
@Slf4j
public class BatchLinkResolverImpl implements BatchLinkResolver {

    @Value("${dxa.web.link-resolver.remove-extension:#{true}}")
    private boolean shouldRemoveExtension;

    @Value("${dxa.web.link-resolver.strip-index-path:#{true}}")
    private boolean shouldStripIndexPath;

    @Value("${dxa.web.link-resolver.keep-trailing-slash:#{false}}")
    private boolean shouldKeepTrailingSlash;

    private BatchLinkRetriever retriever;

    private String DEFAULT_VARIANT_ID  = "[#def#]";

    private volatile ConcurrentMap<String, List<SingleLinkDescriptor>> subscribers = new ConcurrentHashMap<>();

    private volatile ConcurrentMap<String, List<SingleLinkDescriptor>> unresolvedSubscribers = new ConcurrentHashMap<>();

    private volatile List<SingleLinkDescriptor> unresolvedDescriptors = new ArrayList<>();

    private volatile ConcurrentLinkedQueue<ImmutablePair<MultipleLinksDescriptor, Map<String, String>>> subscriberLists
            = new ConcurrentLinkedQueue<>();

    public BatchLinkResolverImpl(@Autowired BatchLinkRetriever retriever) {
        this.retriever = retriever;
    }

    private void dispatchLinkResolution(SingleLinkDescriptor descriptor, ConcurrentMap<String, List<SingleLinkDescriptor>> target) {
        List<SingleLinkDescriptor> descriptors = target.computeIfAbsent(descriptor.getLinkId(), k -> new ArrayList<>());

        // Plan link for resolution only once per unique ID
        if(descriptors.isEmpty()) {
            descriptor.subscribe(this.retriever.addLinkRequest(createBatchLinkRequest(descriptor)));
        } else {
            descriptor.subscribe(descriptors.get(0).getSubscription());
        }
        descriptors.add(descriptor);
    }

    public void dispatchLinkResolution(SingleLinkDescriptor descriptor) {
        if(descriptor == null) {
            return;
        }

        this.dispatchLinkResolution(descriptor, this.subscribers);
    }

    @Override
    public void dispatchMultipleLinksResolution(MultipleLinksDescriptor descriptor) {
        Map<String, String> links = descriptor.getLinks();
        Integer pubId = descriptor.getPublicationId();
        Integer contextId = descriptor.getPageId();

        for (Map.Entry<String, String> linkEntry : links.entrySet()) {
            LinkProcessor processor = new MultipleEntryLinkProcessor(links, linkEntry.getKey());
            if (LINK_TYPE_BINARY.equals(descriptor.getType()) || LINK_TYPE_COMPONENT.equals(descriptor.getType())) {
                dispatchLinkResolution(new ComponentLinkDescriptor(pubId, contextId, processor, descriptor.getType()));
            }
        }
        this.subscriberLists.add(new ImmutablePair<>(descriptor, links));
    }

    private void dispatchLinkResolutionAgain(SingleLinkDescriptor descriptor) {
        if(descriptor == null) {
            return;
        }

        this.dispatchLinkResolution(descriptor, this.unresolvedSubscribers);
    }

    @Override
    public void resolveAndFlush() {
        this.resolve();
        this.resolveLeftovers();
        this.flush();
    }

    private void resolve() {
        this.retriever.executeRequest();
        this.updateRefs();
        this.flushRetriever();
    }

    private void resolveLeftovers() {
        this.subscribeUnresolved();
        this.resolveUnresolved();
        this.flushRetriever();
    }

    private void flushRetriever() {
        this.retriever.clearRequestData();
    }

    private void flush() {
        this.flushDescriptors();
        this.flushLists();
    }

    private void flushLists() {
        ConcurrentLinkedQueue<ImmutablePair<MultipleLinksDescriptor, Map<String, String>>> oldSubscribers = subscriberLists;
        subscriberLists = new ConcurrentLinkedQueue<>();
        for (ImmutablePair<MultipleLinksDescriptor, Map<String, String>> entry : oldSubscribers) {
            MultipleLinksDescriptor descriptor = entry.getLeft();
            descriptor.update(entry.getRight());
        }
    }

    private void subscribeUnresolved() {
        if(!this.unresolvedDescriptors.isEmpty()) {
            for(SingleLinkDescriptor descriptor : this.unresolvedDescriptors) {
                this.dispatchLinkResolutionAgain(descriptor);
            }

            this.unresolvedDescriptors = new ArrayList<>();
        }
    }

    private void resolveUnresolved() {
        this.retriever.executeRequest();
        if(!this.unresolvedSubscribers.isEmpty()) {
            ConcurrentMap<String, List<SingleLinkDescriptor>> oldDescriptors = this.unresolvedSubscribers;
            unresolvedSubscribers = new ConcurrentHashMap<>();
            this.iterateDescriptors(oldDescriptors, false);
        }
    }

    private void iterateDescriptors(ConcurrentMap<String, List<SingleLinkDescriptor>> descriptorsList, boolean hasLeftOvers) {
        for (List<SingleLinkDescriptor> descriptors : descriptorsList.values()) {
            for (SingleLinkDescriptor descriptor : descriptors) {
                if (descriptor == null || !descriptor.canBeResolved()) {
                    continue;
                }

                Link link = this.retriever.getLink(descriptor.getSubscription());

                if (link == null) {
                    continue;
                }

                if (hasLeftOvers && descriptor.getType().equals(LINK_TYPE_BINARY) && !link.isResolved()) {
                    descriptor.setType(LINK_TYPE_COMPONENT);
                    this.unresolvedDescriptors.add(descriptor);
                    continue;
                }

                this.updateLink(descriptor, link);
            }
        }
    }

    private void flushDescriptors() {
        for (List<SingleLinkDescriptor> descriptors : subscribers.values()) {
            for (SingleLinkDescriptor descriptor : descriptors) {
                if (descriptor == null || !descriptor.isResolved()) {
                    continue;
                }
                descriptor.update();
            }
        }
    }

    private void updateRefs() {
        this.iterateDescriptors(this.subscribers, true);
    }

    private void updateLink(SingleLinkDescriptor descriptor, Link link) {
        String resolvedLink = shouldStripIndexPath
                ? PathUtils.stripIndexPath(link.getURL())
                : link.getURL();
        if (shouldKeepTrailingSlash && !PathUtils.isIndexPath(resolvedLink)) {
            resolvedLink = resolvedLink + "/";
        }

        descriptor.setResolvedLink(this.shouldRemoveExtension
                ? PathUtils.stripDefaultExtension(resolvedLink)
                : resolvedLink);
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
                        .withVariantId(DEFAULT_VARIANT_ID)
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
