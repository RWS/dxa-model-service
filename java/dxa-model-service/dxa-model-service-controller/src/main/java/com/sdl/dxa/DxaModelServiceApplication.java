package com.sdl.dxa;

import com.sdl.web.ambient.client.AmbientClientFilter;
import com.tridion.ambientdata.web.AmbientDataServletFilter;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;

@EnableCaching
public class DxaModelServiceApplication {

    @Bean
    public AmbientClientFilter ambientClientFilter() {
        return new AmbientClientFilter();
    }

    @Bean
    public AmbientDataServletFilter ambientDataServletFilter() {
        return new AmbientDataServletFilter();
    }
}
