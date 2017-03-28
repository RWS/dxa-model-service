package com.sdl.web.spring.configuration;

import com.sdl.web.ambient.client.AmbientClientFilter;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("com.sdl.dxa")
@EnableCaching
public class ModelServiceConfiguration {

    @Bean
    public AmbientClientFilter ambientClientFilter() {
        return new AmbientClientFilter();
    }
}
