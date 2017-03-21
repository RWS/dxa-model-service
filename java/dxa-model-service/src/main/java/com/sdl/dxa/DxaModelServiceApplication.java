package com.sdl.dxa;

import com.sdl.web.ambient.client.AmbientClientFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;

@EnableCaching
@SpringBootApplication
public class DxaModelServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DxaModelServiceApplication.class, args);
        AnsiOutput.setEnabled(AnsiOutput.Enabled.ALWAYS);
    }


    @Bean
    public AmbientClientFilter ambientClientFilter() {
        return new AmbientClientFilter();
    }
}
