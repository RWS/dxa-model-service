package com.sdl.dxa.spring.configuration;

import com.sdl.dxa.tridion.linking.BrokerTridionLinkResolver;
import com.sdl.dxa.tridion.linking.TridionBatchLinkResolver;
import com.sdl.dxa.tridion.linking.api.BatchLinkResolver;
import com.sdl.webapp.common.api.content.LinkResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * TridionConfiguration.
 */
@Configuration
public class TridionConfiguration {

    @Bean
    @Primary
    public BatchLinkResolver batchLinkResolver() {
        return new TridionBatchLinkResolver();
    }

    @Bean(name = "dxaLinkResolver")
    public LinkResolver linkResolver() {
        return new BrokerTridionLinkResolver();
    }

}
