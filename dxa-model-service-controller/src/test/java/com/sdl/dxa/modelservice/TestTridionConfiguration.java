package com.sdl.dxa.modelservice;

import com.sdl.dxa.tridion.linking.TridionLinkResolver;
import com.sdl.dxa.tridion.linking.api.BatchLinkResolverFactory;
import com.sdl.web.api.dynamic.taxonomies.WebTaxonomyFactory;
import com.sdl.web.api.taxonomies.WebTaxonomyFactoryImpl;
import com.tridion.ambientdata.web.AmbientDataServletFilter;
import com.tridion.taxonomies.TaxonomyRelationManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TridionConfiguration.
 */
@Configuration
public class TestTridionConfiguration {

    @Bean
    public AmbientDataServletFilter ambientDataServletFilter() {
        return new AmbientDataServletFilter();
    }

    @Bean
    public WebTaxonomyFactory webTaxonomyFactory() {
        return new WebTaxonomyFactoryImpl();
    }

    @Bean
    public TaxonomyRelationManager taxonomyRelationManager() {
        return new TaxonomyRelationManager();
    }

    @Bean(name = "dxaLinkResolver")
    public TridionLinkResolver linkResolver() {
        return new TridionLinkResolver();
    }

    @Bean
    public BatchLinkResolverFactory batchLinkResolverFactory() {
        return () -> null;
    }
}
