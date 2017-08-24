package com.sdl.dxa.modelservice.service;

import com.sdl.dxa.common.dto.PageRequestDto;
import com.sdl.web.api.broker.querying.filter.BrokerResultFilter;
import com.sdl.web.api.broker.querying.filter.LimitFilter;
import com.sdl.webapp.common.api.content.ContentProviderException;
import com.sdl.webapp.common.api.content.PageNotFoundException;
import com.tridion.broker.StorageException;
import com.tridion.broker.querying.Query;
import com.tridion.broker.querying.criteria.content.PageURLCriteria;
import com.tridion.content.PageContentFactory;
import com.tridion.data.CharacterData;
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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.argThat;
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
    private CharacterData pageContentMock;

    @Mock
    private Query query;

    @InjectMocks
    private ContentService contentService;

    @Before
    public void init() throws Exception {
        PowerMockito.whenNew(PageContentFactory.class).withAnyArguments().thenReturn(pageContentFactory);
        when(pageContentFactory.getPageContent(1, 2)).thenReturn(pageContentMock);

        PowerMockito.whenNew(Query.class).withAnyArguments().thenReturn(query);
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


    @Test
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


    private void mockPageURLCriteria(String path) throws Exception {
        PageURLCriteria pathHtmlCriteria = mock(PageURLCriteria.class);
        when(pathHtmlCriteria.getURL()).thenReturn(path);
        PowerMockito.whenNew(PageURLCriteria.class).withArguments(path).thenReturn(pathHtmlCriteria);
    }

}
