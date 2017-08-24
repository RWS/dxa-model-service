package com.sdl.dxa.modelservice.controller;

import com.sdl.dxa.common.dto.EntityRequestDto;
import com.sdl.dxa.modelservice.service.EntityModelService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.mockito.Matchers.eq;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
public class EntityModelControllerTest {

    @MockBean
    private EntityModelService modelService;

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
}