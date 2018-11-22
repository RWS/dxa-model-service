package com.sdl.dxa.modelservice.service.processing.links;

public interface LinkDescriptor {
    void subscribe(String subscriptionId);

    String getSubscription();

    boolean couldBeResolved();

    String getId();

    Integer getComponentId();

    int getPublicationId();

    void setLinkUrl(String url);

    String getType();

    default Integer getPageId() {
        return -1;
    };
}
