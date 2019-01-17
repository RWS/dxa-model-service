package com.sdl.dxa.tridion.linking;

import com.tridion.linking.BinaryLink;
import com.tridion.linking.ComponentLink;
import com.tridion.linking.PageLink;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Function;

/**
 * BrokerTridionLinkResolver.
 */
@Component
public class BrokerTridionLinkResolver extends TridionLinkResolver{

    @Override
    protected Function<ResolvingData, Optional<String>> _componentResolver() {
        return (resolvingData) -> {
            return Optional.ofNullable((new ComponentLink(resolvingData.getPublicationId())).getLink(resolvingData.getItemId()).getURL());
        };
    }

    @Override
    protected Function<ResolvingData, Optional<String>> _pageResolver() {
        return (resolvingData) -> {
            return Optional.ofNullable((new PageLink(resolvingData.getPublicationId())).getLink(resolvingData.getItemId()).getURL());
        };
    }

    @Override
    protected Function<ResolvingData, Optional<String>> _binaryResolver() {
        return (resolvingData) -> {
            String uri = resolvingData.getUri();
            String componentURI = uri.startsWith("tcm:") ? uri : "tcm:" + uri;
            return Optional.ofNullable((new BinaryLink(resolvingData.getPublicationId())).getLink(componentURI, (String)null, (String)null, (String)null, false).getURL());
        };
    }
}
