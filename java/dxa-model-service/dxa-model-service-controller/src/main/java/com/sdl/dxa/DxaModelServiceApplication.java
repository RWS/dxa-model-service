package com.sdl.dxa;

import com.sdl.web.ambient.client.AmbientClientFilter;
import com.sdl.web.api.dynamic.taxonomies.WebTaxonomyFactory;
import com.sdl.web.api.taxonomies.WebTaxonomyFactoryImpl;
import com.tridion.ambientdata.web.AmbientDataServletFilter;
import com.tridion.taxonomies.TaxonomyRelationManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;

@EnableCaching
@SpringBootApplication
@PropertySource("classpath:application.properties")
public class DxaModelServiceApplication {

    /**
     * The main method stays here only because it is needed for development. Should not affect the application.
     */
    public static void main(String[] args) {
        SpringApplication.run(DxaModelServiceApplication.class, args);
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
