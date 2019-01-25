package com.sdl.dxa;

import com.sdl.dxa.caching.LocalizationIdProvider;
import com.sdl.dxa.modelservice.ModelServiceLocalizationIdProvider;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

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
}
