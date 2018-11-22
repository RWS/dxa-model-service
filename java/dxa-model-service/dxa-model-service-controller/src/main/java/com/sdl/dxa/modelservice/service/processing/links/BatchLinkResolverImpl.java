package com.sdl.dxa.modelservice.service.processing.links;

import com.sdl.dxa.common.util.PathUtils;
import com.sdl.web.api.linking.BatchLinkRequest;
import com.sdl.web.api.linking.BatchLinkRequestImpl;
import com.sdl.web.api.linking.BatchLinkRetriever;
import com.sdl.web.api.linking.BatchLinkRetrieverImpl;
import lombok.extern.slf4j.Slf4j;
import org.simpleframework.xml.core.Commit;
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
@Scope(value="prototype")
public class BatchLinkResolverImpl implements BatchLinkResolver {

    @Value("${dxa.web.link-resolver.remove-extension:#{true}}")
    private boolean shouldRemoveExtension;

    private BatchLinkRetriever _retriever;

    private Map<String, List<LinkDescriptor>> _subscribers = new HashMap<>();

    public BatchLinkResolverImpl() {
        this._retriever = new BatchLinkRetrieverImpl();
    }

    @Override
    public void dispatchLinkResolution(LinkDescriptor descriptor) {
        descriptor.subscribe(_retriever.addLinkRequest(_createBatchLinkRequest(descriptor)));
        if(this._subscribers.containsKey(descriptor.getId())) {
            List<LinkDescriptor> descriptors = this._subscribers.get(descriptor.getId());
            descriptors.add(descriptor);
        } else {
            List<LinkDescriptor> list = new ArrayList<>();
            list.add(descriptor);
            this._subscribers.put(descriptor.getId(), list);
        }
    }

    public void flush() {
        this._resolve();
        this._flush();
    }

    private void _resolve() {
        this._retriever.executeRequest();
    }

    private void _flush() {
        this._updateRefs();
        this._subscribers.clear();
        this._retriever.clearRequestData();
    }

    private void _updateRefs() {
        for (List<LinkDescriptor> descriptors : this._subscribers.values()) {
            for(LinkDescriptor descriptor : descriptors) {
                if (descriptor != null && descriptor.couldBeResolved()) {
                    String resolvedUrl = this._retriever.getLink(descriptor.getSubscription()).getURL();
                    descriptor.setLinkUrl(this.shouldRemoveExtension ? PathUtils.stripDefaultExtension(resolvedUrl) : resolvedUrl);
                }
            }
        }
    }

    private BatchLinkRequest _createBatchLinkRequest(LinkDescriptor descriptor) {
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
