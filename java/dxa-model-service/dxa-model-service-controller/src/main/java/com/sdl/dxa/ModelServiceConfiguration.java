package com.sdl.dxa;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.sdl.dxa.caching.LocalizationIdProvider;
import com.sdl.dxa.modelservice.ModelServiceLocalizationIdProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

/**
 * KeyGeneratorConfiguration.
 */
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableCaching
public class ModelServiceConfiguration {

    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    public LocalizationIdProvider localizationIdProvider () {
        return new ModelServiceLocalizationIdProvider();
    }

    @Bean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        ObjectMapper objectMapper = applicationContext.getBean(ObjectMapper.class);

        objectMapper.configure(MapperFeature.IGNORE_DUPLICATE_MODULE_REGISTRATIONS, true);
        objectMapper.registerModule(new JodaModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        jsonConverter.setObjectMapper(objectMapper);
        return jsonConverter;
    }
}
