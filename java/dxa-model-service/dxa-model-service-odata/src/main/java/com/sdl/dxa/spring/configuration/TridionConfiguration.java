package com.sdl.dxa.spring.configuration;

import com.sdl.dxa.tridion.content.StaticContentResolver;
import com.sdl.dxa.tridion.linking.TridionLinkResolver;
import com.sdl.web.ambient.client.AmbientClientFilter;
import com.sdl.web.api.dynamic.taxonomies.WebTaxonomyFactory;
import com.sdl.web.api.taxonomies.WebTaxonomyFactoryImpl;
import com.tridion.taxonomies.TaxonomyRelationManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.annotation.Resource;

/**
 * TridionConfiguration.
 */
@Configuration
public class TridionConfiguration {

    @Resource
    private ApplicationContext applicationContext;

    @Autowired
    Environment environment;

    @Bean
    public AmbientClientFilter ambientClientFilter() {
        return new AmbientClientFilter();
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
    @Primary
    public StaticContentResolver staticContentResolver() {

        String[] profiles = environment.getActiveProfiles();

        boolean isGraphQL = true;

        if (profiles == null) {
            isGraphQL = false;
        } else {
            for (String profile : profiles) {
                if (profile.equalsIgnoreCase("cil.providers.active")) {
                    isGraphQL = false;
                }
            }
        }

        if (isGraphQL) {
            return (StaticContentResolver) applicationContext.getBean("graphQLStaticContentResolver");
        }
        return (StaticContentResolver) applicationContext.getBean("cilStaticContentResolver");
    }
}
