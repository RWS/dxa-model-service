package com.sdl.dxa.modelservice.service.processing.links;

public interface BatchLinkResolver {

    void dispatchLinkResolution(LinkDescriptor descriptor);

    void dispatchLinkListResolution(LinkListDescriptor descriptor);

    void resolveAndFlush();
}
