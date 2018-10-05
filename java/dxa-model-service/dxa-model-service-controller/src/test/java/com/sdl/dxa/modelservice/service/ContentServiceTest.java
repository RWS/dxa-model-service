package com.sdl.dxa.modelservice.service;

import com.sdl.dxa.common.dto.DataModelType;
import com.sdl.dxa.common.dto.EntityRequestDto;
import com.sdl.dxa.common.dto.PageRequestDto;
import com.sdl.web.api.broker.querying.filter.BrokerResultFilter;
import com.sdl.web.api.broker.querying.filter.LimitFilter;
import com.sdl.webapp.common.api.content.ContentProviderException;
import com.sdl.webapp.common.api.content.PageNotFoundException;
import com.sdl.webapp.common.exceptions.DxaItemNotFoundException;
import com.tridion.broker.StorageException;
import com.tridion.broker.querying.Query;
import com.tridion.broker.querying.criteria.content.PageURLCriteria;
import com.tridion.content.PageContentFactory;
import com.tridion.data.CharacterData;
import com.tridion.dcp.ComponentPresentation;
import com.tridion.dcp.ComponentPresentationFactory;
import org.apache.commons.io.IOUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

import static com.sdl.dxa.modelservice.service.ContentService.getModelType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ContentService.class)
public class ContentServiceTest {

    @Mock
    private PageContentFactory pageContentFactory;

    @Mock
    private ComponentPresentationFactory componentPresentationFactory;

    @Mock
    private CharacterData pageContentMock;

    @Mock
    private ConfigService configService;

    @Mock
    private ConfigService.Defaults defaults;

    @Mock
    private ComponentPresentation componentPresentation;

    @Mock
    private Query query;

    @InjectMocks
    private ContentService contentService;

    @Before
    public void init() throws Exception {
        when(pageContentFactory.getPageContent(1, 2)).thenReturn(pageContentMock);
        PowerMockito.whenNew(PageContentFactory.class).withAnyArguments().thenReturn(pageContentFactory);

        PowerMockito.whenNew(ComponentPresentationFactory.class).withAnyArguments().thenReturn(componentPresentationFactory);
        when(componentPresentationFactory.getComponentPresentationWithHighestPriority(anyString())).thenReturn(componentPresentation);
        when(componentPresentationFactory.getComponentPresentation(anyString(), anyString())).thenReturn(componentPresentation);

        PowerMockito.whenNew(Query.class).withAnyArguments().thenReturn(query);

        when(configService.getDefaults()).thenReturn(defaults);

        when(defaults.getDynamicTemplateId(eq(42))).thenReturn(10247);
    }


    @Test
    public void shouldRequestTwoPaths_AfterNormalizingInitial_IfPathWithoutExtension() throws Exception {
        //given
        PageRequestDto pageRequestDto = PageRequestDto.builder(1, "/path").build();

        String expected = "page content";

        mockPageURLCriteria("/page.html");
        mockPageURLCriteria("/page/index.html");

        doReturn(new String[]{"tcm:1-2"}).when(query).executeQuery();
        doReturn(expected).when(pageContentMock).getString();

        //when
        String pageContent = contentService.loadPageContent(pageRequestDto);

        //then
        assertEquals(expected, pageContent);
        PowerMockito.verifyNew(PageURLCriteria.class).withArguments("/path.html");
        PowerMockito.verifyNew(PageURLCriteria.class).withArguments("/path/index.html");
        verify(query).setResultFilter(argThat(new BaseMatcher<BrokerResultFilter>() {
            @Override
            public boolean matches(Object item) {
                return ((LimitFilter) item).getMaximumResults() == 1;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Should add a limit filter for a single entry");
            }
        }));
    }


    //@Test
    public void shouldRequestSinglePath_AfterNormalizingInitial_IfPathHasExtension() throws Exception {
        //given
        PageRequestDto pageRequestDto = PageRequestDto.builder(1, "/path.html").build();

        mockPageURLCriteria("/page.html");
        doReturn(new String[]{"tcm:1-2"}).when(query).executeQuery();

        //when
        contentService.loadPageContent(pageRequestDto);

        //then
        PowerMockito.verifyNew(PageURLCriteria.class).withArguments("/path.html");
        PowerMockito.verifyNew(PageURLCriteria.class, never()).withArguments("/path/index.html");
    }

    @Test(expected = PageNotFoundException.class)
    public void shouldThrow404Exception_WhenNoResultFound_ForRequest() throws StorageException, ContentProviderException {
        //given
        PageRequestDto pageRequestDto = PageRequestDto.builder(1, "/path").build();

        doReturn(new String[0]).when(query).executeQuery();

        //when
        contentService.loadPageContent(pageRequestDto);

        //then
        //exception
    }

    @Test
    public void shouldDetectCorrectModel_OfJsonContent() throws IOException {
        //given
        String dd4tSource = IOUtils.toString(new ClassPathResource("models/dd4t.json").getInputStream(), "UTF-8");
        String dd4tSourceCP = IOUtils.toString(new ClassPathResource("models/dd4t_cp.json").getInputStream(), "UTF-8");
        String r2Source = IOUtils.toString(new ClassPathResource("models/r2.json").getInputStream(), "UTF-8");
        String r2SourceEntity = IOUtils.toString(new ClassPathResource("models/r2_entity.json").getInputStream(), "UTF-8");
        String r2SourceEntity2 = IOUtils.toString(new ClassPathResource("models/r2_entity_2.json").getInputStream(), "UTF-8");


        //when
        DataModelType dd4t = getModelType(dd4tSource);
        DataModelType dd4tCP = getModelType(dd4tSourceCP);
        DataModelType r2 = getModelType(r2Source);
        DataModelType r2Entity = getModelType(r2SourceEntity);
        DataModelType r2Entity2 = getModelType(r2SourceEntity2);

        //then
        assertEquals(DataModelType.DD4T, dd4t);
        assertEquals(DataModelType.DD4T, dd4tCP);
        assertEquals(DataModelType.R2, r2);
        assertEquals(DataModelType.R2, r2Entity);
        assertEquals(DataModelType.R2, r2Entity2);
    }

    @Test
    public void shouldLoadHighestPriority_WhenRequestedHighestPriority() throws DxaItemNotFoundException {
        //given
        EntityRequestDto entityRequest = EntityRequestDto.builder(42, 1)
                .dcpType(EntityRequestDto.DcpType.HIGHEST_PRIORITY)
                .build();

        //when
        ComponentPresentation presentation = contentService.loadComponentPresentation(entityRequest);

        //then
        verify(componentPresentationFactory).getComponentPresentationWithHighestPriority(eq("tcm:42-1"));
        assertSame(componentPresentation, presentation);
    }

    @Test
    public void shouldLoadHighestPriority_WhenRequestedHighestPriority_OnlyIfTemplateIsNotSet() throws DxaItemNotFoundException {
        //given
        EntityRequestDto entityRequest = EntityRequestDto.builder(42, 1, 2)
                .dcpType(EntityRequestDto.DcpType.HIGHEST_PRIORITY)
                .build();

        //when
        ComponentPresentation presentation = contentService.loadComponentPresentation(entityRequest);

        //then
        verify(componentPresentationFactory).getComponentPresentation(eq("tcm:42-1"), eq("tcm:42-2-32"));
        assertSame(componentPresentation, presentation);
    }

    @Test
    public void shouldLoadCP_WhenRequestedDefault() throws DxaItemNotFoundException {
        //given
        EntityRequestDto entityRequest = EntityRequestDto.builder(42, 1, 2)
                .build();

        //when
        ComponentPresentation presentation = contentService.loadComponentPresentation(entityRequest);

        //then
        verify(componentPresentationFactory).getComponentPresentation(eq("tcm:42-1"), eq("tcm:42-2-32"));
        assertSame(componentPresentation, presentation);
    }


    @Test
    public void shouldGetDefaultDynamicTemplate_ForDCP_WhenNoTemplateSet() throws ContentProviderException {
        //given
        EntityRequestDto entityRequest = EntityRequestDto.builder(42, 1).build();

        //when
        ComponentPresentation presentation = contentService.loadComponentPresentation(entityRequest);

        //then
        verify(componentPresentationFactory).getComponentPresentation(eq("tcm:42-1"), eq("tcm:42-10247-32"));
        assertSame(componentPresentation, presentation);
    }

    @Test(expected = DxaItemNotFoundException.class)
    public void shouldThrow404Exception_IfCPIsNotFound() throws DxaItemNotFoundException {
        //given
        when(componentPresentationFactory.getComponentPresentation(anyString(), anyString())).thenReturn(null);

        //when
        contentService.loadComponentPresentation(EntityRequestDto.builder(42, 1).build());

        //then
        //exception
    }

    private void mockPageURLCriteria(String path) throws Exception {
        PageURLCriteria pathHtmlCriteria = mock(PageURLCriteria.class);
        when(pathHtmlCriteria.getURL()).thenReturn(path);
        PowerMockito.whenNew(PageURLCriteria.class).withArguments(path).thenReturn(pathHtmlCriteria);
    }

}
