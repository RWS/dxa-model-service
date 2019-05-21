package com.sdl.dxa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdl.dxa.caching.LocalizationIdProvider;
import com.sdl.dxa.modelservice.ModelServiceLocalizationIdProvider;
import com.tridion.ambientdata.web.AmbientDataServletFilter;
import com.tridion.content.PageContentFactory;
import com.tridion.taxonomies.TaxonomyFactory;
import com.tridion.taxonomies.TaxonomyRelationManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

/**
 * KeyGeneratorConfiguration.
 */
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableCaching
@ComponentScan(basePackages = "com.sdl.dxa.spring.configuration")
public class ModelServiceConfiguration {

    @Bean
    public LocalizationIdProvider localizationIdProvider () {
        return new ModelServiceLocalizationIdProvider();
    }

    @Bean
    public TaxonomyRelationManager taxonomyRelationManager() {
        return new TaxonomyRelationManager();
    }

    @Bean
    public TaxonomyFactory taxonomyFactory() {
        return new TaxonomyFactory();
    }

    @Bean
    public AmbientDataServletFilter ambientDataServletFilter() {
        return new AmbientDataServletFilter();
    }

    @Bean
    public PageContentFactory pageContentFactory() {
        return new PageContentFactory();
    }

    @Bean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter(ObjectMapper objectMapper) {
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();

        jsonConverter.setObjectMapper(objectMapper);
        return jsonConverter;
    }
}
