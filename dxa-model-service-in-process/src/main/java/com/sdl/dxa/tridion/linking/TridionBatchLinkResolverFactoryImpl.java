package com.sdl.dxa.tridion.linking;


import com.sdl.dxa.tridion.linking.api.BatchLinkResolver;
import com.sdl.dxa.tridion.linking.api.BatchLinkResolverFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TridionBatchLinkResolverFactoryImpl implements BatchLinkResolverFactory {

    @Autowired
    BatchLinkResolver tridionBatchLinkResolver;

    @Override
    public BatchLinkResolver getBatchLinkResolver() {
        //The in-process version of the tridionBatchLinkResolver doesn't actually resolve
        //anything in batches and is therefor stateless and can be reused across requests and threads.
        return tridionBatchLinkResolver;
    }
}
