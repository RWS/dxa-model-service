package com.sdl.dxa.spring.configuration;

import com.sdl.dxa.tridion.linking.BatchLinkResolverFactoryImpl;
import com.sdl.dxa.tridion.linking.TridionLinkResolver;
import com.sdl.dxa.tridion.linking.api.BatchLinkResolverFactory;
import com.sdl.web.ambient.client.AmbientClientFilter;
import com.sdl.webapp.common.api.content.LinkResolver;
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
    @Primary
    public BatchLinkResolverFactory batchLinkResolverFactory() {
        return new BatchLinkResolverFactoryImpl();
    }

    @Bean
    public AmbientClientFilter ambientClientFilter() {
        return new AmbientClientFilter();
    }

    @Bean
    public TaxonomyRelationManager taxonomyRelationManager() {
        return new TaxonomyRelationManager();
    }

    @Bean(name = "dxaLinkResolver")
    public LinkResolver linkResolver() {
        return new TridionLinkResolver();
    }

}
