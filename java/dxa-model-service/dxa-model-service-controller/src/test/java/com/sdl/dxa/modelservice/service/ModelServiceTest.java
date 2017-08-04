package com.sdl.dxa.modelservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdl.dxa.api.datamodel.DataModelSpringConfiguration;
import com.sdl.dxa.api.datamodel.model.EntityModelData;
import com.sdl.dxa.common.dto.EntityRequestDto;
import com.sdl.dxa.common.dto.PageRequestDto;
import com.sdl.dxa.tridion.linking.RichTextLinkResolver;
import com.sdl.web.api.broker.querying.filter.BrokerResultFilter;
import com.sdl.web.api.broker.querying.filter.LimitFilter;
import com.sdl.webapp.common.api.content.ContentProviderException;
import com.sdl.webapp.common.api.content.LinkResolver;
import com.sdl.webapp.common.api.content.PageNotFoundException;
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
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

import static com.sdl.dxa.modelservice.service.ModelServiceImpl.getModelType;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ModelServiceImpl.class)
public class ModelServiceTest {

    @Mock
    private Query query;

    @Spy
    private ObjectMapper objectMapper = new DataModelSpringConfiguration().dxaR2ObjectMapper();

    @Mock
    private LinkResolver linkResolver;

    @Mock
    private RichTextLinkResolver richTextLinkResolver;

    @Mock
    private PageContentFactory pageContentFactory;

    @Mock
    private CharacterData pageContentMock;

    @Mock
    private ComponentPresentationFactory componentPresentationFactory;

    @Mock
    private ComponentPresentation dcp;

    @Mock
    private ConfigService configService;

    @Mock
    private ConfigService.Defaults defaults;

    @InjectMocks
    private ModelServiceImpl modelService;

    @Before
    public void init() throws Exception {
        PowerMockito.whenNew(PageContentFactory.class).withAnyArguments().thenReturn(pageContentFactory);
        when(pageContentFactory.getPageContent(1, 2)).thenReturn(pageContentMock);

        PowerMockito.whenNew(ComponentPresentationFactory.class).withAnyArguments().thenReturn(componentPresentationFactory);
        doReturn(dcp).when(componentPresentationFactory).getComponentPresentation(eq("tcm:42-1"), eq("tcm:42-10247-32"));

        String value = objectMapper.writeValueAsString(objectMapper.readTree(new ClassPathResource("dcp.json").getFile()));
        when(dcp.getContent()).thenReturn(value);

        PowerMockito.whenNew(Query.class).withAnyArguments().thenReturn(query);

        when(configService.getDefaults()).thenReturn(defaults);

        when(defaults.getDynamicTemplateId(eq(42))).thenReturn(10247);
    }


    @Test
    public void shouldDetectCorrectModel_OfJsonContent() throws IOException {
        //given
        String dd4tSource = IOUtils.toString(new ClassPathResource("models/dd4t.json").getInputStream(), "UTF-8");
        String r2Source = IOUtils.toString(new ClassPathResource("models/r2.json").getInputStream(), "UTF-8");


        //when
        PageRequestDto.DataModelType dd4t = getModelType(dd4tSource);
        PageRequestDto.DataModelType r2 = getModelType(r2Source);

        //then
        assertEquals(PageRequestDto.DataModelType.DD4T, dd4t);
        assertEquals(PageRequestDto.DataModelType.R2, r2);
    }

    @Test
    public void shouldRequestTwoPaths_AfterNormalizingInitial_IfPathWithoutExtension() throws Exception {
        //given 
        PageRequestDto pageRequestDto = PageRequestDto.builder()
                .publicationId(1)
                .path("/path")
                .build();

        String expected = "page content";

        mockPageURLCriteria("/page.html");
        mockPageURLCriteria("/page/index.html");

        doReturn(new String[]{"tcm:1-2"}).when(query).executeQuery();
        doReturn(expected).when(pageContentMock).getString();

        //when
        String pageContent = modelService.loadPageContent(pageRequestDto);

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

    private void mockPageURLCriteria(String path) throws Exception {
        PageURLCriteria pathHtmlCriteria = mock(PageURLCriteria.class);
        when(pathHtmlCriteria.getURL()).thenReturn(path);
        PowerMockito.whenNew(PageURLCriteria.class).withArguments(path).thenReturn(pathHtmlCriteria);
    }

    @Test
    public void shouldRequestSinglePath_AfterNormalizingInitial_IfPathHasExtension() throws Exception {
        //given
        PageRequestDto pageRequestDto = PageRequestDto.builder()
                .publicationId(1)
                .path("/path.html")
                .build();

        mockPageURLCriteria("/page.html");
        doReturn(new String[]{"tcm:1-2"}).when(query).executeQuery();

        //when
        modelService.loadPageContent(pageRequestDto);

        //then
        PowerMockito.verifyNew(PageURLCriteria.class).withArguments("/path.html");
        PowerMockito.verifyNew(PageURLCriteria.class, never()).withArguments("/path/index.html");
    }

    @Test(expected = PageNotFoundException.class)
    public void shouldThrow404Exception_WhenNoResultFound_ForRequest() throws StorageException, ContentProviderException {
        //given
        PageRequestDto pageRequestDto = PageRequestDto.builder()
                .publicationId(1)
                .path("/path")
                .build();

        doReturn(new String[0]).when(query).executeQuery();

        //when
        modelService.loadPageContent(pageRequestDto);

        //then
        //exception
    }

    @Test
    public void shouldGetDefaultDynamicTemplate_ForDCP_WhenNoTemplateSet() throws ContentProviderException {
        //given 
        EntityRequestDto entityRequest = EntityRequestDto.builder().publicationId(42).componentId(1).build();

        //when
        EntityModelData entity = modelService.loadEntity(entityRequest);

        //then
        assertEquals("1468", entity.getId());
    }

}