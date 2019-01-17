package com.sdl.dxa;

import com.sdl.dxa.caching.TridionCacheConfiguration;
import com.sdl.web.ambient.client.AmbientClientFilter;
import com.sdl.web.api.dynamic.taxonomies.WebTaxonomyFactory;
import com.sdl.web.api.taxonomies.WebTaxonomyFactoryImpl;
import com.tridion.ambientdata.web.AmbientDataServletFilter;
import com.tridion.taxonomies.TaxonomyRelationManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@PropertySource("classpath:dxa.properties")
@Import({TridionCacheConfiguration.class, Dd4tSpringConfiguration.class})
public class TestDxaModelServiceApplication {

    /**
     * The main method stays here only because it is needed for development. Should not affect the application.
     */
    public static void main(String[] args) {
        SpringApplication.run(TestDxaModelServiceApplication.class, args);
        AnsiOutput.setEnabled(AnsiOutput.Enabled.DETECT);
    }

    @Bean
    public AmbientClientFilter ambientClientFilter() {
        return new AmbientClientFilter();
    }

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
}
