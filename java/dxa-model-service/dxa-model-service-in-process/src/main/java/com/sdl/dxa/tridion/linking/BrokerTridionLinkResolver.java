package com.sdl.dxa.tridion.linking;

import com.tridion.linking.BinaryLink;
import com.tridion.linking.ComponentLink;
import com.tridion.linking.PageLink;
import org.springframework.stereotype.Component;

/**
 * BrokerTridionLinkResolver.
 */
@Component
public class BrokerTridionLinkResolver extends TridionLinkResolver {

    @Override
    protected String resolveComponent(ResolvingData resolvingData) {
        return (new ComponentLink(resolvingData.getPublicationId())).getLink(resolvingData.getItemId()).getURL();
    }

    @Override
    protected String resolvePage(ResolvingData resolvingData) {
        return (new PageLink(resolvingData.getPublicationId())).getLink(resolvingData.getItemId()).getURL();
    }

    @Override
    protected String resolveBinary(ResolvingData resolvingData) {
        String uri = resolvingData.getUri();
        String componentURI = uri.startsWith("tcm:") ? uri : "tcm:" + uri;
        return (new BinaryLink(resolvingData.getPublicationId())).getLink(componentURI, (String)null, (String)null, (String)null, false).getURL();
    }
}
