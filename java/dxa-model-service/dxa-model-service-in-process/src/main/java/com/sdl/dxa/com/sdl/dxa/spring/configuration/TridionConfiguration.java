package com.sdl.dxa.com.sdl.dxa.spring.configuration;

import com.sdl.dxa.tridion.linking.BrokerTridionLinkResolver;
import com.sdl.dxa.tridion.linking.TridionBatchLinkResolver;
import com.sdl.dxa.tridion.linking.TridionLinkResolver;
import com.sdl.dxa.tridion.linking.api.BatchLinkResolver;
import com.tridion.ambientdata.web.AmbientDataServletFilter;
import com.tridion.taxonomies.TaxonomyFactory;
import com.tridion.taxonomies.TaxonomyRelationManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * TridionConfiguration.
 */
@Configuration
public class TridionConfiguration {

    @Bean
    public AmbientDataServletFilter ambientDataServletFilter() {
        return new AmbientDataServletFilter();
    }

    @Bean
    @Primary
    public TaxonomyFactory taxonomyFactory() {
        return new TaxonomyFactory();
    }

    @Bean
    @Primary
    public BatchLinkResolver batchLinkResolver() {
        return new TridionBatchLinkResolver();
    }

    @Bean(name = "dxaLinkResolver")
    public TridionLinkResolver linkResolver() {
        return new BrokerTridionLinkResolver();
    }

    @Bean
    public TaxonomyRelationManager taxonomyRelationManager() {
        return new TaxonomyRelationManager();
    }
}
