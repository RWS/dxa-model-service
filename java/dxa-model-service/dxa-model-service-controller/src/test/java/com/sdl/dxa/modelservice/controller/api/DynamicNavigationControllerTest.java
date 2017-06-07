package com.sdl.dxa.modelservice.controller.api;

import com.sdl.dxa.api.datamodel.model.SitemapItemModelData;
import com.sdl.dxa.api.datamodel.model.TaxonomyNodeModelData;
import com.sdl.dxa.common.dto.DepthCounter;
import com.sdl.dxa.common.dto.SitemapRequestDto;
import com.sdl.dxa.tridion.navigation.dynamic.NavigationModelProvider;
import com.sdl.dxa.tridion.navigation.dynamic.OnDemandNavigationModelProvider;
import com.sdl.webapp.common.api.navigation.NavigationFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
public class DynamicNavigationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NavigationModelProvider navigationModelProvider;

    @MockBean
    private OnDemandNavigationModelProvider onDemandNavigationModelProvider;

    @Test
    public void shouldBeCompatible_WithOnDemandNavigationSpec_WithoutSiteMapId() throws Exception {
        //given
        String url = "/api/navigation/42/subtree";
        doReturn(Optional.of(Collections.emptySet())).when(onDemandNavigationModelProvider).getNavigationSubtree(any());

        //when, then
        mockMvc.perform(get(url))
                .andExpect(status().isOk());

        mockMvc.perform(
                get(url)
                        .param("includeAncestors", "false")
                        .param("descendantLevels", "1"))
                .andExpect(status().isOk());

        mockMvc.perform(
                get(url)
                        .param("includeAncestors", "true")
                        .param("descendantLevels", "2"))
                .andExpect(status().isOk());

        verify(onDemandNavigationModelProvider, times(2)).getNavigationSubtree(argThat(getArgumentMatcher(42, false, 1, 1, null)));
        verify(onDemandNavigationModelProvider).getNavigationSubtree(argThat(getArgumentMatcher(42, true, 2, 2, null)));
    }

    @Test
    public void shouldBeCompatible_WithOnDemandNavigationSpec_WithSiteMapId() throws Exception {
        //given 
        String url = "/api/navigation/42/subtree/t1-k2";
        doReturn(Optional.of(Collections.emptySet())).when(onDemandNavigationModelProvider).getNavigationSubtree(any());

        //when, then
        mockMvc.perform(get(url))
                .andExpect(status().isOk());

        mockMvc.perform(
                get(url)
                        .param("includeAncestors", "false")
                        .param("descendantLevels", "1"))
                .andExpect(status().isOk());

        mockMvc.perform(
                get(url)
                        .param("includeAncestors", "true")
                        .param("descendantLevels", "2"))
                .andExpect(status().isOk());

        verify(onDemandNavigationModelProvider, times(2)).getNavigationSubtree(argThat(getArgumentMatcher(42, false, 1, 1, "t1-k2")));
        verify(onDemandNavigationModelProvider).getNavigationSubtree(argThat(getArgumentMatcher(42, true, 2, 2, "t1-k2")));
    }

    private ArgumentMatcher<SitemapRequestDto> getArgumentMatcher(int localizationId, boolean includeAncestors, int descendantLevels, int expandlevels, String sitemapId) {
        return new ArgumentMatcher<SitemapRequestDto>() {
            @Override
            public boolean matches(Object argument) {
                SitemapRequestDto dto = (SitemapRequestDto) argument;
                NavigationFilter navigationFilter = new NavigationFilter();
                navigationFilter.setDescendantLevels(descendantLevels);
                navigationFilter.setWithAncestors(includeAncestors);
                return dto.getLocalizationId() == localizationId
                        && Objects.equals(navigationFilter, dto.getNavigationFilter())
                        && Objects.equals(sitemapId, dto.getSitemapId())
                        && Objects.equals(new DepthCounter(expandlevels), dto.getExpandLevels());
            }
        };
    }

    @Test
    public void shouldAcceptLocalizationId_AndReturnNavigationModel() throws Exception {
        //given 
        String url = "/api/navigation/42";
        Optional<SitemapItemModelData> modelData = Optional.of(new TaxonomyNodeModelData());
        doReturn(modelData).when(navigationModelProvider).getNavigationModel(any());

        //when, then
        mockMvc.perform(get(url)).andExpect(status().isOk());

        mockMvc.perform(get(url + "/")).andExpect(status().isOk());

        verify(navigationModelProvider, times(2)).getNavigationModel(argThat(getArgumentMatcher(42, false, -1, Integer.MAX_VALUE, null)));
    }

    @Test
    public void shouldSend404_IfThereIsNoNavigationModel() throws Exception {
        //given
        String url = "/api/navigation/42";
        doReturn(Optional.empty()).when(navigationModelProvider).getNavigationModel(any());

        //when, then
        mockMvc.perform(get(url)).andExpect(status().is(404));
    }

    @Test
    public void shouldFailOnInvalidRequest() throws Exception {
        String url = "/api/navigation";

        //when, then
        mockMvc.perform(get(url + "/string"))
                .andExpect(status().is(400));

        mockMvc.perform(get(url)).andExpect(status().isNotFound());
    }
}