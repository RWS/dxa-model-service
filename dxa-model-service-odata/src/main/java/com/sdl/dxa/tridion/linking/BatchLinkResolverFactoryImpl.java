package com.sdl.dxa.tridion.linking;


import com.sdl.dxa.tridion.linking.api.BatchLinkResolver;
import com.sdl.dxa.tridion.linking.api.BatchLinkResolverFactory;
import com.sdl.web.api.linking.BatchLinkRetriever;
import com.sdl.web.api.linking.BatchLinkRetrieverImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BatchLinkResolverFactoryImpl implements BatchLinkResolverFactory {

    @Value("${dxa.web.link-resolver.remove-extension:#{true}}")
    private boolean shouldRemoveExtension;

    @Value("${dxa.web.link-resolver.strip-index-path:#{true}}")
    private boolean shouldStripIndexPath;

    @Value("${dxa.web.link-resolver.keep-trailing-slash:#{false}}")
    private boolean shouldKeepTrailingSlash;

    @Override
    public BatchLinkResolver getBatchLinkResolver() {
        return new BatchLinkResolverImpl(shouldRemoveExtension, shouldStripIndexPath, shouldKeepTrailingSlash, new BatchLinkRetrieverImpl());
    }
}
