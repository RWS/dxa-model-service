package com.sdl.dxa.spring.configuration;

import com.sdl.dxa.tridion.linking.BatchLinkResolver;
import com.sdl.dxa.tridion.linking.TridionLinkResolver;
import com.sdl.dxa.tridion.linking.descriptors.api.MultipleLinksDescriptor;
import com.sdl.dxa.tridion.linking.descriptors.api.SingleLinkDescriptor;
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

//    @Bean
//    public AmbientClientFilter ambientClientFilter() {
//        return new AmbientClientFilter();
//    }

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
    public BatchLinkResolver batchLinkResolver() {
        return new BatchLinkResolver() {
            @Override
            public void dispatchLinkResolution(final SingleLinkDescriptor descriptor) {

            }

            @Override
            public void dispatchMultipleLinksResolution(final MultipleLinksDescriptor descriptor) {

            }

            @Override
            public void resolveAndFlush() {

            }
        };
    }
}
