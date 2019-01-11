package com.sdl.dxa;

import com.sdl.dxa.caching.LocalizationIdProvider;
import com.sdl.dxa.modelservice.ModelServiceLocalizationIdProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * KeyGeneratorConfiguration.
 */
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class IdProviderConfiguration {

    @Bean
    public LocalizationIdProvider localizationIdProvider () {
        return new ModelServiceLocalizationIdProvider();
    }
}
