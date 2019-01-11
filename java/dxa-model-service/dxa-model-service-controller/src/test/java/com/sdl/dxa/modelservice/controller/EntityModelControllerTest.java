package com.sdl.dxa.modelservice.controller;

import com.sdl.dxa.common.dto.ContentType;
import com.sdl.dxa.common.dto.DataModelType;
import com.sdl.dxa.common.dto.EntityRequestDto;
import com.sdl.dxa.modelservice.service.ContentService;
import com.sdl.dxa.modelservice.service.EntityModelService;
import com.sdl.dxa.modelservice.service.LegacyPageModelService;
import com.sdl.dxa.modelservice.service.PageModelService;
import com.sdl.dxa.modelservice.service.processing.conversion.ToDd4tConverter;
import com.sdl.dxa.modelservice.service.processing.conversion.ToR2Converter;
import com.sdl.web.spring.configuration.DxaModelServiceApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.mockito.Matchers.eq;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = DxaModelServiceApplication.class)
@AutoConfigureMockMvc(addFilters = false)
@MockBean(classes = {PageModelService.class, LegacyPageModelService.class, ToDd4tConverter.class, ToR2Converter.class})
public class EntityModelControllerTest {

    @MockBean
    private EntityModelService modelService;

    @MockBean
    private ContentService contentService;

    @Autowired
    private MockMvc mvc;

    @Test
    public void shouldCallModelService_WithTemplateId() throws Exception {
        //given 

        //when
        mvc.perform(MockMvcRequestBuilders.get("/EntityModel/tcm/42/123-345")).andExpect(MockMvcResultMatchers.status().isOk());

        //then
        Mockito.verify(modelService).loadEntity(eq(EntityRequestDto.builder(42, 123, 345).build()));
    }

    @Test
    public void shouldCallModelService_WithoutTemplateId() throws Exception {
        //given

        //when
        mvc.perform(MockMvcRequestBuilders.get("/EntityModel/tcm/42/123")).andExpect(MockMvcResultMatchers.status().isOk());

        //then
        Mockito.verify(modelService).loadEntity(eq(EntityRequestDto.builder(42, 123, 0).build()));
    }

    @Test
    public void shouldCallModelService_WithoutTemplateId_WithHighestPriority() throws Exception {
        //given

        //when
        mvc.perform(MockMvcRequestBuilders.get("/EntityModel/tcm/42/123?dcpType=HIGHEST_PRIORITY")).andExpect(MockMvcResultMatchers.status().isOk());

        //then
        Mockito.verify(modelService).loadEntity(eq(
                EntityRequestDto.builder(42, 123, 0).dcpType(EntityRequestDto.DcpType.HIGHEST_PRIORITY).build()));
    }

    @Test
    public void shouldCallModelService_WithoutTemplateId_WithRawContent() throws Exception {
        //given

        //when
        mvc.perform(MockMvcRequestBuilders.get("/EntityModel/tcm/42/123?raw=true")).andExpect(MockMvcResultMatchers.status().isOk());

        //then
        Mockito.verify(contentService).loadComponentPresentation(
                eq(EntityRequestDto.builder(42, 123, 0).contentType(ContentType.RAW).build()));
    }

    @Test
    public void shouldCallModelService_WithoutTemplateId_WithDD4TContent() throws Exception {
        //given

        //when
        mvc.perform(MockMvcRequestBuilders.get("/EntityModel/tcm/42/123?modelType=DD4T")).andExpect(MockMvcResultMatchers.status().isOk());

        //then
        Mockito.verify(modelService).loadEntity(
                eq(EntityRequestDto.builder(42, 123, 0).dataModelType(DataModelType.DD4T).build()));
    }
}