package com.sdl.dxa.modelservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdl.dxa.common.dto.StaticContentRequestDto;
import com.sdl.dxa.tridion.content.StaticContentResolver;
import com.sdl.webapp.common.api.content.ContentProviderException;
import com.sdl.webapp.common.api.content.StaticContentItem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefaultsTest {

    @Mock
    private StaticContentResolver contentResolver;

    private ConfigService.Defaults defaults;

    @Before
    public void init() throws ContentProviderException, IOException {

        ClassPathResource classPathResource = new ClassPathResource("_all.json");
        File file = classPathResource.getFile();

        defaults = new ConfigService.Defaults(contentResolver, new ObjectMapper());
        defaults.setConfigBootstrapPath("/system/config/_all.json");
        defaults.setConfigDcpUriField("dataPresentationTemplateUri");

        when(contentResolver.getStaticContent(eq(StaticContentRequestDto.builder("/system/config/_all.json", "42").build())))
                .thenReturn(new StaticContentItem("content_type", file, true));
    }

    @Test
    public void shouldLoadAllJsonFile_AndParseTemplateIDFromIt() {
        //given 

        //when
        int dynamicTemplateId = defaults.getDynamicTemplateId(42);

        //then
        assertEquals(666, dynamicTemplateId);
    }

    @Test
    public void shouldLoadAllJsonFile_AndReturnMinusOne_IfFieldNotFound() {
        //given
        defaults.setConfigDcpUriField("something");

        //when
        int dynamicTemplateId = defaults.getDynamicTemplateId(42);

        //then
        assertEquals(-1, dynamicTemplateId);
    }
}