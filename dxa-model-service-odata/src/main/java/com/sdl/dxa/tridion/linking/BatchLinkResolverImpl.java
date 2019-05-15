package com.sdl.dxa.tridion.linking;


import com.sdl.dxa.common.util.PathUtils;
import com.sdl.dxa.tridion.linking.api.BatchLinkResolver;
import com.sdl.dxa.tridion.linking.api.descriptors.MultipleLinksDescriptor;
import com.sdl.dxa.tridion.linking.api.descriptors.SingleLinkDescriptor;
import com.sdl.dxa.tridion.linking.api.processors.LinkProcessor;
import com.sdl.dxa.tridion.linking.descriptors.ComponentLinkDescriptor;
import com.sdl.dxa.tridion.linking.processors.EntryLinkProcessor;
import com.sdl.web.api.linking.BatchLinkRequest;
import com.sdl.web.api.linking.BatchLinkRequestImpl;
import com.sdl.web.api.linking.BatchLinkRetriever;
import com.sdl.web.api.linking.Link;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.sdl.web.util.ContentServiceQueryConstants.LINK_TYPE_BINARY;
import static com.sdl.web.util.ContentServiceQueryConstants.LINK_TYPE_COMPONENT;
import static com.sdl.web.util.ContentServiceQueryConstants.LINK_TYPE_DYNAMIC_COMPONENT;
import static com.sdl.web.util.ContentServiceQueryConstants.LINK_TYPE_PAGE;

@Slf4j
public class BatchLinkResolverImpl implements BatchLinkResolver {

    private static final String DEFAULT_VARIANT_ID  = "[#def#]";

    private boolean shouldRemoveExtension;
    private boolean shouldStripIndexPath;
    private boolean shouldKeepTrailingSlash;
    private BatchLinkRetriever retriever;

    private List<SingleLinkDescriptor> descriptors = new ArrayList<>();

    public BatchLinkResolverImpl(boolean shouldRemoveExtension, boolean shouldStripIndexPath, boolean shouldKeepTrailingSlash, BatchLinkRetriever retriever) {
        this.shouldRemoveExtension = shouldRemoveExtension;
        this.shouldStripIndexPath = shouldStripIndexPath;
        this.shouldKeepTrailingSlash = shouldKeepTrailingSlash;
        this.retriever = retriever;
    }

    public void dispatchLinkResolution(SingleLinkDescriptor descriptor) {
        if (descriptor == null) {
            return;
        }

        this.descriptors.add(descriptor);
    }

    @Override
    public void dispatchMultipleLinksResolution(MultipleLinksDescriptor descriptor) {
        Map<String, String> links = descriptor.getLinks();
        Integer pubId = descriptor.getPublicationId();
        Integer contextId = descriptor.getPageId();

        for (Map.Entry<String, String> linkEntry : links.entrySet()) {
            if (LINK_TYPE_BINARY.equals(descriptor.getType()) || LINK_TYPE_COMPONENT.equals(descriptor.getType())) {
                LinkProcessor processor = new EntryLinkProcessor(links, linkEntry.getKey());
                dispatchLinkResolution(new ComponentLinkDescriptor(pubId, contextId, processor, descriptor.getType()));
            }
        }
    }

    @Override
    public void resolveAndFlush() {
        resolveAndFlush(this.descriptors);

        for (SingleLinkDescriptor descriptor : descriptors) {
            descriptor.update();
        }

        this.descriptors.clear();
    }

    public void resolveAndFlush(List<SingleLinkDescriptor> myDescriptors) {
        HashMap<String, SingleLinkDescriptor> uniqueLinks = new HashMap<>();

        //Remove duplicates in the link by using a HashMap.
        //This way a link only gets added to the request once, even if it is multiple times on the page.
        for (SingleLinkDescriptor singleLinkDescriptor : myDescriptors) {
            String key = singleLinkDescriptor.getLinkId();
            SingleLinkDescriptor descriptorForLink = uniqueLinks.get(key);

            if (!uniqueLinks.containsKey(key))  {
                BatchLinkRequest linkRequest = createBatchLinkRequest(singleLinkDescriptor);
                String subscriptionId = retriever.addLinkRequest(linkRequest);
                singleLinkDescriptor.subscribe(subscriptionId);
                uniqueLinks.put(key, singleLinkDescriptor);
                descriptorForLink = singleLinkDescriptor;
            }
            singleLinkDescriptor.subscribe(descriptorForLink.getSubscription());
        }

        //Execute a single request for all the links:
        this.retriever.executeRequest();

        //Then process the result of the request:
        List<SingleLinkDescriptor> retryBinaryLink = new ArrayList<>();
        for (SingleLinkDescriptor descriptor : descriptors) {

            Link link = this.retriever.getLink(descriptor.getSubscription());
            if (link == null) {
                continue;
            }

            this.updateLink(descriptor, link);

            //Sometimes Binary links don't get resolved, and it has to be retried as a component link:
            if (descriptor.getType().equals(LINK_TYPE_BINARY) && !link.isResolved()) {
                descriptor.setType(LINK_TYPE_COMPONENT);
                retryBinaryLink.add(descriptor);
            }
        }
        this.retriever.clearRequestData();

        //Retry any binary links that failed:
        if (!retryBinaryLink.isEmpty()) {
            this.resolveAndFlush(retryBinaryLink);
        }
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

    public int nrDescriptors() {
        return descriptors.size();
    }
}
