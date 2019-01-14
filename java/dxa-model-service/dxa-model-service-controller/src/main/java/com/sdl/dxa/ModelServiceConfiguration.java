package com.sdl.dxa;

import com.sdl.dxa.caching.LocalizationIdProvider;
import com.sdl.dxa.modelservice.ModelServiceLocalizationIdProvider;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * KeyGeneratorConfiguration.
 */
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableCaching
public class ModelServiceConfiguration {

    @Bean
    public LocalizationIdProvider localizationIdProvider () {
        return new ModelServiceLocalizationIdProvider();
    }
//
//    @Bean
//    public LocalizationAwareKeyGenerator localizationAwareKeyGenerator() {
//        return new LocalizationAwareKeyGenerator();
//    }
//
//    @Bean
//    @Primary
//    public NamedCacheProvider namedCacheProvider() throws ConfigurationException {
//        return new DefaultNamedCacheProvider();
//    }


}
