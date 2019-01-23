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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hibernate.mapping.Collection;
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

    private volatile ConcurrentMap<String, List<SingleLinkDescriptor>> subscribers = new ConcurrentHashMap<>();

    private volatile ConcurrentLinkedQueue<ImmutablePair<MultipleLinksDescriptor, Map<String, String>>> subscriberLists
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
        Integer pubId = descriptor.getPublicationId();
        for (Map.Entry<String, String> linkEntry : links.entrySet()) {
            EntryLinkProcessor processor = new EntryLinkProcessor(links, linkEntry.getKey());
            if (LINK_TYPE_BINARY.equals(descriptor.getType())) {
                dispatchLinkResolution(new BinaryLinkDescriptor(pubId, processor));
            } else if (LINK_TYPE_COMPONENT.equals(descriptor.getType())) {
                dispatchLinkResolution(new ComponentLinkDescriptor(pubId, processor));
            }
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
        ConcurrentLinkedQueue<ImmutablePair<MultipleLinksDescriptor, Map<String, String>>> oldSubscribers = subscriberLists;
        subscriberLists = new ConcurrentLinkedQueue<>();
        for (ImmutablePair<MultipleLinksDescriptor, Map<String, String>> entry : oldSubscribers) {
            MultipleLinksDescriptor descriptor = entry.getLeft();
            descriptor.update(entry.getRight());
        }
    }

    private void updateRefs() {
        ConcurrentMap<String, List<SingleLinkDescriptor>> oldDescriptors = this.subscribers;
        subscribers = new ConcurrentHashMap<>();
        for (List<SingleLinkDescriptor> descriptors : oldDescriptors.values()) {
            for (SingleLinkDescriptor descriptor : descriptors) {
                if (descriptor == null || !descriptor.couldBeResolved()) {
                    continue;
                }
                String resolvedUrl = this.retriever.getLink(descriptor.getSubscription()).getURL();
                if (resolvedUrl == null) {
                    descriptor.update("");
                    continue;
                }
                String resolvedLink = shouldStripIndexPath
                        ? PathUtils.stripIndexPath(resolvedUrl)
                        : resolvedUrl;
                if (shouldKeepTrailingSlash && PathUtils.isIndexPath(resolvedUrl)) {
                    resolvedLink = resolvedLink + "/";
                }
                descriptor.update(this.shouldRemoveExtension
                        ? PathUtils.stripDefaultExtension(resolvedLink)
                        : resolvedLink);
            }
        }
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
