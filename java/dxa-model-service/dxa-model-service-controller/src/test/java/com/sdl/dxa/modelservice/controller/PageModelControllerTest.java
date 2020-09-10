package com.sdl.dxa.modelservice.controller;

import com.sdl.dxa.DxaModelServiceApplication;
import com.sdl.dxa.common.dto.ContentType;
import com.sdl.dxa.common.dto.DataModelType;
import com.sdl.dxa.common.dto.PageRequestDto;
import com.sdl.dxa.modelservice.service.ContentService;
import com.sdl.dxa.modelservice.service.LegacyPageModelService;
import com.sdl.dxa.modelservice.service.PageModelService;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.mockito.BDDMockito.argThat;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Mockito.atLeastOnce;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = DxaModelServiceApplication.class)
@AutoConfigureMockMvc(addFilters = false)
public class PageModelControllerTest {

    @MockBean
    private PageModelService pageModelService;

    @MockBean
    private LegacyPageModelService legacyPageModelService;

    @MockBean
    private ContentService contentService;

    @Autowired
    private MockMvc mvc;

    @Test
    public void shouldReturnExpectedPathForSimpleCases() throws Exception {
        expectForUrl("/example/to/site", "/PageModel/tcm/42/example/to/site");
        expectForUrl("/example/to/site", "/PageModel/tcm/42//example/to/site");
        expectForUrl("/example/42/to/site", "/PageModel/tcm/42//example/42/to/site");
        expectForUrl("/example/42/to/site", "/PageModel/tcm/42//example/42/to/site");
    }

    @Test
    public void shouldReturnExpectedPathForAllCases_Regardless_LastSlash() throws Exception {
        expectForUrl("/example/to/site/", "/PageModel/tcm/42/example/to/site/");
        expectForUrl("/example/to/site/", "/PageModel/tcm/42//example/to/site/");
        expectForUrl("/example/42/to/site/", "/PageModel/tcm/42/example/42/to/site/");
        expectForUrl("/example/42/to/site/", "/PageModel/tcm/42//example/42/to/site/");
    }

    @Test
    public void shouldReturnExpectedPathForAllCases_Regardless_ContextPath() throws Exception {
        expectForUrl("/example/to/site", "/test/PageModel/tcm/42/example/to/site", "/test");
        expectForUrl("/example/to/site", "/test/PageModel/tcm/42//example/to/site", "/test");
        expectForUrl("/example/to/site", "/model.svc/PageModel/tcm/42//example/to/site", "/model.svc");
    }

    @Test
    public void shouldReturnBadRequest_IfPageUrlIsNotPresent() throws Exception {
        mvc.perform(get("/PageModel/tcm/42")).andExpect(status().is(400));
    }

    @Test
    public void shouldWork_IfPageUrlIsJustEmpty() throws Exception {
        mvc.perform(get("/PageModel/tcm/42/")).andExpect(status().isOk());
    }

    @Test
    public void shouldAcceptModelType_AndPassItTpRequestBuilder() throws Exception {
        //given 

        //when
        mvc.perform(get("/PageModel/tcm/42//?modelType=DD4T")).andExpect(status().isOk());

        //then
        verify(this.legacyPageModelService, atLeastOnce()).loadLegacyPageModel(matcherFor(DataModelType.DD4T, ContentType.MODEL, "/"));
    }

    @Test
    public void shouldAcceptModelType_AndPassItTpRequestBuilder_Raw() throws Exception {
        //given

        //when
        mvc.perform(get("/PageModel/tcm/42//?modelType=DD4T&raw=true")).andExpect(status().isOk());

        //then
        verify(this.contentService, atLeastOnce()).loadPageContent(matcherFor(DataModelType.DD4T, ContentType.RAW, "/"),
                true);
    }

    @Test
    public void shouldUseDefaultModelType_AndPassItTpRequestBuilder() throws Exception {
        //given

        //when
        mvc.perform(get("/PageModel/tcm/42//")).andExpect(status().isOk());

        //then
        verify(this.pageModelService, atLeastOnce()).loadPageModel(matcherFor(DataModelType.R2, ContentType.MODEL, "/"));
    }

    @Test
    public void shouldUseDefaultModelType_AndPassItTpRequestBuilder_Raw() throws Exception {
        //given

        //when
        mvc.perform(get("/PageModel/tcm/42//?raw=true")).andExpect(status().isOk());

        //then
        verify(this.contentService, atLeastOnce()).loadPageContent(matcherFor(DataModelType.R2, ContentType.RAW, "/"),
                true);
    }


    private void expectForUrl(String expected, String url, String contextPath) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(url);
        if (contextPath != null) {
            requestBuilder.contextPath(contextPath);
        }
        mvc.perform(requestBuilder).andExpect(status().isOk());

        verify(this.pageModelService, atLeastOnce()).loadPageModel(matcherFor(expected));
    }

    private void expectForUrl(String expected, String url) throws Exception {
        expectForUrl(expected, url, null);
    }

    private PageRequestDto matcherFor(String expected) {
        return matcherFor(DataModelType.R2, ContentType.MODEL, expected);
    }

    private PageRequestDto matcherFor(DataModelType dataModelType, ContentType contentType, String expected) {
        return argThat(new BaseMatcher<PageRequestDto>() {
            @Override
            public boolean matches(Object item) {
                PageRequestDto requestDto = (PageRequestDto) item;
                return requestDto.getDataModelType() == dataModelType
                        && requestDto.getContentType() == contentType
                        && requestDto.getPath().equals(expected);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Page request is built with url = " + expected)
                        .appendText("\nPage request is built with content type = " + contentType)
                        .appendText("\nPage request is built with model type = " + dataModelType);
            }
        });
    }
}
