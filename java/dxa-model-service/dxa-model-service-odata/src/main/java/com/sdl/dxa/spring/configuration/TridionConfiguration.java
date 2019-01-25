package com.sdl.dxa.spring.configuration;

import com.sdl.dxa.tridion.linking.TridionLinkResolver;
import com.sdl.web.ambient.client.AmbientClientFilter;
import com.tridion.taxonomies.TaxonomyFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TridionConfiguration.
 */
@Configuration
public class TridionConfiguration {

    @Bean
    public AmbientClientFilter ambientClientFilter() {
        return new AmbientClientFilter();
    }

    @Bean
    public TaxonomyFactory taxonomyFactory() {
        return new TaxonomyFactory();
    }

    @Bean(name = "dxaLinkResolver")
    public TridionLinkResolver linkResolver() {
        return new TridionLinkResolver();
    }
}
