package com.sdl.dxa.tridion.linking;

import com.google.common.collect.Lists;
import com.sdl.webapp.common.api.content.LinkResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RichTextLinkResolverTest {

    @Mock
    private LinkResolver linkResolver;

    @InjectMocks
    private RichTextLinkResolver richTextLinkResolver;

    @Before
    public void init() {
        when(linkResolver.resolveLink(eq("tcm:1-2"), eq("1"))).thenReturn("");
        when(linkResolver.resolveLink(eq("tcm:1-3"), eq("1"))).thenReturn("");
        when(linkResolver.resolveLink(eq("tcm:1-11"), eq("1"))).thenReturn("resolved-link");
    }

    @Test
    public void shouldSuppressBrokenLinks() {
        //given
        List<String> values = Lists.newArrayList("<p>Text <a href=\"tcm:1-2\">link text</a><!--CompLink tcm:1-2--> text ",
                "<a href=\"tcm:1-3\">link2",
                " text2",
                "</a><!--CompLink tcm:1-3--></p>",
                "<a href=\"tcm:1-2\">text3</a><!--CompLink tcm:1-2-->");

        //when
        Set<String> notResolvedBuffer = new HashSet<>();
        String result = values.stream()
                .map(String.class::cast)
                .map(fragment -> richTextLinkResolver.processFragment(fragment, 1, notResolvedBuffer))
                .collect(Collectors.joining());

        //then
        assertEquals("<p>Text link text text link2 text2</p>text3", result);
    }

    @Test
    public void shouldResolveLinks_IfSameTcmUri_IsDoubledInSameFragment() {
        //given
        String string = "<p>Text <a href=\"tcm:1-2\">link text</a><!--CompLink tcm:1-2--> " +
                "<a href=\"tcm:1-2\">link text</a><!--CompLink tcm:1-2--> text </p>";

        //when
        String result = richTextLinkResolver.processFragment(string, 1);

        //then
        assertEquals("<p>Text link text link text text </p>", result);
    }

    @Test
    public void shouldResolveLinks_AndRemoveMarkers() {
        //given 
        String fragment = "<p>Text <a href=\"tcm:1-11\">link text</a><!--CompLink tcm:1-11--></p>";

        //when
        String result = richTextLinkResolver.processFragment(fragment, 1);

        //then
        assertEquals("<p>Text <a href=\"resolved-link\">link text</a></p>", result);
    }

    @Test
    public void shouldResolveLinks_InFragmentWithLineBreaks() {
        //given 
        String fragment = "<p>\nText <a href=\"tcm:1-11\">\nlink text</a><!--CompLink tcm:1-11-->\n</p>";

        //when
        String result = richTextLinkResolver.processFragment(fragment, 1);

        //then
        assertEquals("<p>\nText <a href=\"resolved-link\">\nlink text</a>\n</p>", result);
    }

    @Test
    public void shouldResolveLinks_WhenLinkHasManyAttrs() {
        //given 
        String fragment = "<p>\nText <a data-first=\"1\" href=\"tcm:1-11\" data-second=\"2\">\nlink text</a><!--CompLink tcm:1-11-->\n</p>";

        //when
        String result = richTextLinkResolver.processFragment(fragment, 1);

        //then
        assertEquals("<p>\nText <a data-first=\"1\" href=\"resolved-link\" data-second=\"2\">\nlink text</a>\n</p>", result);
    }

}