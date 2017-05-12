package com.sdl.dxa.modelservice.controller.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
public class DynamicNavigationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void shouldBeCompatible_WithOnDemandNavigationSpec_WithoutSiteMapId() throws Exception {
        //given
        String url = "/api/navigation/subtree";

        //when, then
        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].Id").value("subtree/null-false-1"));

        mockMvc.perform(
                get(url)
                        .param("includeAncestors", "false")
                        .param("descendantLevels", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].Id").value("subtree/null-false-1"));

        mockMvc.perform(
                get(url)
                        .param("includeAncestors", "true")
                        .param("descendantLevels", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].Id").value("subtree/null-true-2"));

    }

    @Test
    public void shouldBeCompatible_WithOnDemandNavigationSpec_WithSiteMapId() throws Exception {
        //given 
        String url = "/api/navigation/subtree/123";

        //when, then
        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].Id").value("subtree/123-false-1"));

        mockMvc.perform(
                get(url)
                        .param("includeAncestors", "false")
                        .param("descendantLevels", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].Id").value("subtree/123-false-1"));

        mockMvc.perform(
                get(url)
                        .param("includeAncestors", "true")
                        .param("descendantLevels", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].Id").value("subtree/123-true-2"));

    }

    @Test
    public void shouldAcceptLocalizationId_AndReturnNavigationModel() throws Exception {
        //given 
        String url = "/api/navigation";

        //when, then
        mockMvc.perform(get(url).param("localizationId", "123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Id").value("model-123"));

        mockMvc.perform(get(url + "/").param("localizationId", "123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Id").value("model-123"));
    }

    @Test
    public void shouldFailOnInvalidRequest() throws Exception {
        String url = "/api/navigation";

        //when, then
        mockMvc.perform(get(url).param("localizationId", "string instead of integer"))
                .andExpect(status().is(400));

        mockMvc.perform(get(url)).andExpect(status().is(400));
    }
}