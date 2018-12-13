package com.sdl.dxa.tridion.linking.descriptors.api;

public interface SingleLinkDescriptor extends LinkDescriptor {

    void subscribe(String subscriptionId);

    void update(String url);

    boolean couldBeResolved();

    String getId();

    String getSubscription();

    Integer getComponentId();
}
