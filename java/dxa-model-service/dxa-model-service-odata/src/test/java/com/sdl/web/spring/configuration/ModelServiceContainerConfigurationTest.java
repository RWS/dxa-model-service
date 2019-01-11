package com.sdl.web.spring.configuration;

import org.junit.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ModelServiceContainerConfigurationTest {

    @Test
    public void shouldHaveServiceContainerConfiguration_InRightPackageByConvention() {
        //then
        assertEquals("com.sdl.web.spring.configuration", ModelServiceContainerConfiguration.class.getPackage().getName());
        assertTrue(ModelServiceContainerConfiguration.class.isAnnotationPresent(Configuration.class));
    }

    @Test
    public void serviceContainerConfiguration_ImportsModelServiceConfiguration() {
        //given
        Class<?> value = ModelServiceContainerConfiguration.class.getAnnotation(Import.class).value()[0];

        //then
        assertEquals(DxaModelServiceApplication.class, value);
    }
}