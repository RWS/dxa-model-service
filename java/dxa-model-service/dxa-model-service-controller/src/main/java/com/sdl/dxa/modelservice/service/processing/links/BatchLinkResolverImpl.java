package com.sdl.dxa.modelservice.service.processing.links;

import com.sdl.dxa.common.util.PathUtils;
import com.sdl.dxa.modelservice.service.processing.links.processors.EntryLinkProcessor;
import com.sdl.web.api.linking.BatchLinkRequest;
import com.sdl.web.api.linking.BatchLinkRequestImpl;
import com.sdl.web.api.linking.BatchLinkRetriever;
import com.sdl.web.api.linking.BatchLinkRetrieverImpl;
import com.sdl.webapp.common.util.TcmUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.sdl.web.util.ContentServiceQueryConstants.LINK_TYPE_BINARY;
import static com.sdl.web.util.ContentServiceQueryConstants.LINK_TYPE_COMPONENT;
import static com.sdl.web.util.ContentServiceQueryConstants.LINK_TYPE_PAGE;

@Component
@Scope(value = "prototype")
public class BatchLinkResolverImpl implements BatchLinkResolver {

    @Value("${dxa.web.link-resolver.remove-extension:#{true}}")
    private boolean shouldRemoveExtension;

    private BatchLinkRetriever retriever;

    private Map<String, List<LinkDescriptor>> subscribers = new HashMap<>();

    private List<ImmutablePair<LinkListDescriptor, Map<String, String>>> subscriberLists = new ArrayList<>();

    public BatchLinkResolverImpl() {
        this.retriever = new BatchLinkRetrieverImpl();
    }

    @Override
    public void dispatchLinkResolution(LinkDescriptor descriptor) {

        if(descriptor == null) {
            return;
        }

        descriptor.subscribe(this.retriever.addLinkRequest(createBatchLinkRequest(descriptor)));
        List<LinkDescriptor> descriptors = this.subscribers.computeIfAbsent(descriptor.getId(), k -> new ArrayList<>());
        descriptors.add(descriptor);
    }

    @Override
    public void dispatchLinkListResolution(LinkListDescriptor descriptor) {
        Map<String, String> links = descriptor.getLinks();
        for (Map.Entry<String, String> linkEntry : links.entrySet()) {

            Integer pubId = TcmUtils.getPublicationId(linkEntry.getKey());
            LinkDescriptor ld = null;

            if (descriptor.getType() == LINK_TYPE_BINARY) {
                ld = new BinaryLinkDescriptor(pubId, new EntryLinkProcessor(links, linkEntry.getKey(), linkEntry.getValue()));
            }

            if (descriptor.getType() == LINK_TYPE_COMPONENT) {
                ld = new ComponentLinkDescriptor(pubId, new EntryLinkProcessor(links, linkEntry.getKey(), linkEntry.getValue()));
            }


            dispatchLinkResolution(ld);
        }
        this.subscriberLists.add(new ImmutablePair<>(descriptor, links));
    }

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
        this.subscribers.clear();
        this.subscriberLists.clear();
        this.retriever.clearRequestData();
    }

    private void updateLists() {
        for (ImmutablePair<LinkListDescriptor, Map<String, String>> entry : subscriberLists) {
            LinkListDescriptor descriptor = entry.getLeft();
            descriptor.update(entry.getRight());
        }
    }

    private void updateRefs() {
        for (List<LinkDescriptor> descriptors : this.subscribers.values()) {
            for (LinkDescriptor descriptor : descriptors) {
                if (descriptor != null && descriptor.couldBeResolved()) {
                    String resolvedUrl = this.retriever.getLink(descriptor.getSubscription()).getURL();
                    if (resolvedUrl != null) {
                        descriptor.update(this.shouldRemoveExtension ? PathUtils.stripDefaultExtension(resolvedUrl) : resolvedUrl);
                    } else {
                        descriptor.update("");
                    }
                }
            }
        }
    }

    private BatchLinkRequest createBatchLinkRequest(LinkDescriptor descriptor) {
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
