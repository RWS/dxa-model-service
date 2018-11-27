package com.sdl.dxa.modelservice.service.processing.links;

public interface LinkDescriptor {

    void subscribe(String subscriptionId);

    void update(String url);

    boolean couldBeResolved();

    String getId();

    String getSubscription();

    String getType();

    Integer getComponentId();

    Integer getPublicationId();

    default Integer getPageId() {
        return -1;
    };
}
