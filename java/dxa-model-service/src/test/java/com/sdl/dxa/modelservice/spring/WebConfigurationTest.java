package com.sdl.dxa.modelservice.spring;

import org.junit.Test;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class WebConfigurationTest {

    @Test
    public void shouldDisable_SpringsDefaults_FavorByPath() {
        //given 
        ContentNegotiationConfigurer configurer = mock(ContentNegotiationConfigurer.class);

        //when
        new WebConfiguration().configureContentNegotiation(configurer);

        //then
        verify(configurer).favorPathExtension(eq(false));
    }
}