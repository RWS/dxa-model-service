package com.sdl.web.spring.configuration;

import com.sdl.dxa.DxaModelServiceApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(DxaModelServiceApplication.class)
public class ModelServiceContainerConfiguration {

}
