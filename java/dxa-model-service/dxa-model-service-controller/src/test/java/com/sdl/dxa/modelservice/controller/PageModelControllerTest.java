package com.sdl.dxa.modelservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdl.dxa.api.datamodel.model.PageModelData;
import com.sdl.dxa.common.dto.PageRequestDto;
import com.sdl.dxa.modelservice.service.ModelService;
import com.sdl.dxa.tridion.linking.RichTextLinkResolver;
import com.sdl.webapp.common.api.content.ContentProviderException;
import com.sdl.webapp.common.api.content.LinkResolver;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.argThat;
import static org.mockito.BDDMockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
public class PageModelControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean ModelService modelService;

    @Test
    public void shouldReturnCorrectPath_withOnlyOneSlashInUrl() throws Exception {
        // given
        String expected = "/autotest-parent/";
        String url = "/PageModel/tcm/1065/autotest-parent/";

        // when
        mvc.perform(get(url).accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
                // then
                .andExpect(status().isOk());

        verify(this.modelService).loadPageModel(argThat(new BaseMatcher<PageRequestDto>() {
            @Override
            public boolean matches(Object item) {
                PageRequestDto prd = (PageRequestDto) item;
                return prd.getPath().equals(expected);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Should be valid url");
            }
        }));

    }

    @Test
    public void shouldReturnCorrectPath_withTwoSlashesInUrl() throws Exception {
        // given
        String expected = "/autotest-parent/";
        String url = "/PageModel/tcm/1065//autotest-parent/";

        // when
        mvc.perform(get(url).accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
                // then
                .andExpect(status().isOk());

        verify(this.modelService).loadPageModel(argThat(new BaseMatcher<PageRequestDto>() {
            @Override
            public boolean matches(Object item) {
                PageRequestDto prd = (PageRequestDto) item;
                return prd.getPath().equals(expected);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Should be valid url");
            }
        }));

    }

}
