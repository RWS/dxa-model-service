package com.sdl.dxa.tridion.linking.api.descriptors;

import com.sdl.dxa.tridion.linking.api.processors.LinkProcessor;

public interface SingleLinkDescriptor extends LinkDescriptor {

    void subscribe(String subscriptionId);

    void update(String url);

    boolean couldBeResolved();

    String getLinkId();

    Integer getComponentId();

    LinkProcessor getLinkProcessor();

    /**
     * @return ID of the template the component is rendered with.
     */
    default Integer getTemplateId() {
        return -1;
    };

    String getSubscription();
}
