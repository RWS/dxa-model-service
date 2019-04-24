package com.sdl.dxa.tridion.linking.impl;

import com.google.common.collect.Lists;
import com.sdl.dxa.modelservice.service.ConfigService;
import com.sdl.webapp.common.api.content.LinkResolver;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RichTextLinkResolverTest {

    @Mock
    private LinkResolver linkResolver;

    @Mock
    private ConfigService configService;

    @InjectMocks
    private RichTextLinkResolverImpl richTextLinkResolver;

    @Before
    public void init() {
        when(linkResolver.resolveLink(eq("tcm:1-2"), eq("1"), eq(true), eq("-1"))).thenReturn("");
        when(linkResolver.resolveLink(eq("tcm:1-3"), eq("1"), eq(true), eq("-1"))).thenReturn("");
        when(linkResolver.resolveLink(eq("tcm:1-11"), eq("1"), eq(true), eq("-1"))).thenReturn("resolved-link");
        when(linkResolver.resolveLink(eq("tcm:1-12"), eq("1"), eq(true), eq("-1"))).thenReturn("resolved-link.html");

        ConfigService.Defaults defaults = new ConfigService.Defaults(null, null);
        defaults.setRichTextXmlnsRemove(true);
        defaults.setRichTextResolve(true);
        Mockito.when(configService.getDefaults()).thenReturn(defaults);
        Mockito.when(linkResolver.resolveLink("tcm:15-980", "1", true)).thenReturn("<link1/>");
        Mockito.when(linkResolver.resolveLink("tcm:17-982", "1", true)).thenReturn("<link3/>");
    }

    @Test
    public void shouldSuppressBrokenLinks() {
        Mockito.when(linkResolver.resolveLink("tcm:1-3", "1", true)).thenReturn("resolved_link_url");
        //given
        List<String> values = Lists.newArrayList("<p>Text1 <a href=\"tcm:1-2\">(link text)1</a><!--CompLink tcm:1-2--> text2 ",
                "<a href=\"tcm:1-2\">(link",
                " text)2",
                "</a><!--CompLink tcm:1-2--></p>",
                "<a href=\"tcm:1-3\">(link text)3</a><!--CompLink tcm:1-3-->",
                " text2");

        //when
        Set<String> notResolvedBuffer = new HashSet<>();
        String result = values.stream()
                .map(String.class::cast)
                .map(fragment -> richTextLinkResolver.processFragment(fragment, 1, -1, notResolvedBuffer))
                .collect(Collectors.joining());

        //then
        assertEquals("<p>Text1 (link text)1 text2 (link text)2</p><a href=\"resolved_link_url\">(link text)3</a> text2", result);
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
    public void shouldResolveLinks_AndLeaveExtension() {
        //given
        String fragment = "<p>Text <a href=\"tcm:1-12\">link text</a><!--CompLink tcm:1-12--></p>";

        //when
        String result = richTextLinkResolver.processFragment(fragment, 1);

        //then
        assertEquals("<p>Text <a href=\"resolved-link.html\">link text</a></p>", result);
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
    public void shouldRemoveNamespaces_WhenAttributesHaveNamespacePrefixes() {
        String fragment = "<p>\nText <a " +
                "xmlns:xhtml=\"test\" " +
                "xmlns:xlink=\"http://www.w3.org/1999/xlink\" " +
                "xlink=\"X link\" " +
                "data-id=\"ID\" " +
                "xlink:title=\"link title\" " +
                "xlink:href=\"tcm:1-11\">\nlink text</a><!--CompLink tcm:1-11-->\n</p>";

        //when
        String result = richTextLinkResolver.processFragment(fragment, 1);

        //then
        assertEquals("<p>\nText <a xlink=\"X link\" data-id=\"ID\" title=\"link title\" href=\"resolved-link\">\nlink text</a>\n</p>", result);
    }

    @Test
    public void shouldNotRemoveNamespaces_IfDisabled() {
        String fragment = "<p>\nText <a xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink=\"X link\" data-id=\"ID\" xlink:title=\"link title\" xlink:href=\"tcm:1-11\">\nlink text</a><!--CompLink tcm:1-11-->\n</p>";
        configService.getDefaults().setRichTextXmlnsRemove(false);

        //when
        String result = richTextLinkResolver.processFragment(fragment, 1);

        //then
        assertEquals("<p>\n" +
                "Text <a xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink=\"X link\" data-id=\"ID\" xlink:title=\"link title\" xlink:href=\"tcm:1-11\" href=\"resolved-link\">\n" +
                "link text</a>\n" +
                "</p>", result);
    }

    @Test
    public void shouldNotTouchFragment_IfDisabled() {
        configService.getDefaults().setRichTextResolve(false);
        configService.getDefaults().setRichTextXmlnsRemove(true);
        String fragment = "<p>Text <a xlink:href=\"tcm:1-11\">link text</a><!--CompLink tcm:1-11--></p>";

        //when
        String result = richTextLinkResolver.processFragment(fragment, 1);

        //then
        assertEquals(fragment, result);
    }

    @Test
    public void shouldNotTouchFragment_IfDisabled_AndRemoveDisabled() { // not valid config case
        configService.getDefaults().setRichTextResolve(false);
        configService.getDefaults().setRichTextXmlnsRemove(false);
        String fragment = "<p>Text <a xlink:href=\"tcm:1-11\">link text</a><!--CompLink tcm:1-11--></p>";

        //when
        String result = richTextLinkResolver.processFragment(fragment, 1);

        //then
        assertEquals(fragment, result);
    }

    @Test
    public void shouldNotRemoveNamespaces_IfDisabled_AndResolve() {
        configService.getDefaults().setRichTextXmlnsRemove(false);
        configService.getDefaults().setRichTextResolve(true);
        String fragment = "<p>Text <a xlink:href=\"tcm:1-11\" xmlns:title=\"title\">link text</a><!--CompLink tcm:1-11--></p>";

        //when
        String result = richTextLinkResolver.processFragment(fragment, 1);

        //then
        assertEquals("<p>Text <a xlink:href=\"tcm:1-11\" href=\"resolved-link\" xmlns:title=\"title\">link text</a></p>", result);
    }

    @Test
    public void shouldRemoveNamespaces_IfDisabled_AndResolveOnlyHref() {
        configService.getDefaults().setRichTextXmlnsRemove(false);
        configService.getDefaults().setRichTextResolve(true);
        String fragment = "<p>Text <a xlink:href=\"tcm:1-11\" href=\"tcm:1-11\">link text</a><!--CompLink tcm:1-11--></p>";

        //when
        String result = richTextLinkResolver.processFragment(fragment, 1);

        //then
        assertEquals("<p>Text <a xlink:href=\"tcm:1-11\" href=\"resolved-link\">link text</a></p>", result);
    }

    @Test
    public void shouldAlsoRemove_Xmlns_WithoutSpecificField() {
        //given
        String fragment = "<p xmlns=\"http://www.w3.org/1999/xhtml\">And some content with a <a href=\"tcm:1-11\" title=\"Copy of My article\">link</a> in it!</p>";

        String result = richTextLinkResolver.processFragment(fragment, 1);

        //then
        assertEquals("<p>And some content with a <a href=\"resolved-link\" title=\"Copy of My article\">link</a> in it!</p>", result);
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

    @Test
    public void testGetAllFragmentsThroughRegex() {

        String fragment = "<p><a title=\"Unused Component\" href=\"tcm:15-980\">UNRESOLVED " +
                "LINK</a><!--CompLink tcm:15-980--> <span>ipsumis dolor sit amet, consectetur adipiscing elit. Ut " +
                "semper ex tortor, a ullamcorper sem venenatis sed. In interdum leo eu orci pharetra luctus. Nulla ut" +
                " blandit urna, ac maximus mauris. Cras sapien dolor, blandit eu nisi at, pretium facilisis quam" +
                ". </span></p>\n" +
                "<p>Donec ipsum ex, pellentesque id diam a, aliquam commodo nibh. Fusce lacinia arcu lorem, volutpat " +
                "pulvinar quam scelerisque vel. Etiam auctor pulvinar mi, eget pretium odio. Curabitur iaculis nisl " +
                "augue, fermentum porta arcu condimentum convallis. Ut sit amet nisi a enim blandit accumsan. Integer" +
                " scelerisque ac nibh a viverra.<a title=\"Latest News\" href=\"tcm:15-564\">INTERNA LRTF  LINK</a>" +
                " Ut sed nisi id velit egestas mollis.</p>";
        long start = System.currentTimeMillis();
        List<String> links = richTextLinkResolver.retrieveAllLinksFromFragment(fragment);

        System.out.println("Duration: " + (System.currentTimeMillis() - start) + " ms.");
        assertEquals(2, links.size());
        assertEquals("tcm:15-980", links.get(0));
        assertEquals("tcm:15-564", links.get(1));
    }

    @Test
    public void testNoLinksReturn() {
        String fragment = "<p>" +
                "LINK</a><!--CompLink tcm:15-980--> <span>ipsumis dolor sit amet, consectetur adipiscing elit. Ut " +
                "semper ex tortor, a ullamcorper sem venenatis sed. In interdum leo eu orci pharetra luctus. Nulla ut" +
                " blandit urna, ac maximus mauris. Cras sapien dolor, blandit eu nisi at, pretium facilisis quam" +
                ". </span></p>\n" +
                "<p>Donec ipsum ex, pellentesque id diam a, aliquam commodo nibh. Fusce lacinia arcu lorem, volutpat " +
                "pulvinar quam scelerisque vel. Etiam auctor pulvinar mi, eget pretium odio. Curabitur iaculis nisl " +
                "augue, fermentum porta arcu condimentum convallis. Ut sit amet nisi a enim blandit accumsan. Integer" +
                " scelerisque ac nibh a viverra." +
                " Ut sed nisi id velit egestas mollis.</p>";

        long start = System.currentTimeMillis();
        List<String> links = richTextLinkResolver.retrieveAllLinksFromFragment(fragment);
        long end = System.currentTimeMillis() - start;

        assertTrue(links.isEmpty());
        System.out.println("Duration: " + end + " ms.");
    }

    @Test
    public void testApplyBatchOfLinksStartVerifyLinks() {
        String fragment = getFragments();

        List<String> links = richTextLinkResolver.retrieveAllLinksFromFragment(fragment);

        assertEquals(4, links.size());
        assertEquals("tcm:18-983", links.get(3));
    }

    @Test
    public void testApplyBatchOfLinksStartVerifyXmlnsAndHrefs() {

        String droppedXmlns = richTextLinkResolver.dropXlmns("<a xmlns:href=\"tcm:4-29\">LINK1</a>"+
                "<a xmlns:href=\"tcm:2-21\">LINK2</a>");
        String generatedHref = richTextLinkResolver.generateHref("<a xmlns:href=\"tcm:4-29\">LINK1</a>"+
                "<a xmlns:href=\"tcm:2-21\">LINK2</a>" +
                "<a xmlns:href=\"tcm:3-2\" href=\"tcm:3-2\">LINK3</a>");

        assertEquals("<a>LINK1</a><a>LINK2</a>", droppedXmlns);
        assertEquals("<a xmlns:href=\"tcm:4-29\" href=\"tcm:4-29\">LINK1</a>" +
                "<a xmlns:href=\"tcm:2-21\" href=\"tcm:2-21\">LINK2</a>" +
                "<a xmlns:href=\"tcm:3-2\" href=\"tcm:3-2\">LINK3</a>", generatedHref);
    }

    @Test
    public void testApplyBatchOfLinksStartVerifyResolvedFrahments() {
        String fragment = getFragments();

        Map<String, String> batchOfLinks = getResolvedLinksMap();
        Set<String> linksNotResolved = new LinkedHashSet<>();

        long start = System.currentTimeMillis();

        String fragments = richTextLinkResolver.processFragment(fragment, 1, linksNotResolved);
        String resolvedFragments = richTextLinkResolver.applyBatchOfLinksStart(fragment, batchOfLinks, linksNotResolved);

        String expected = "<p>Text <a data=\"1\" href=\"resolved-link\" data2=\"2\">link text</a> after text</p><p><a title=\"Unused Component\" href=\"<link1/>\">UNRESOLVED LINK1</a> </p><p>UNRESOLVED LINK2 </p><p><a title=\"Unused Component\" href=\"<link3/>\">UNRESOLVED LINK3</a> </p><p>UNRESOLVED LINK4 </p>";
        assertEquals(expected, fragments);
        assertEquals(expected, resolvedFragments);
        assertEquals(2, linksNotResolved.size());
        assertEquals("tcm:16-981", linksNotResolved.toArray()[0]);
        assertEquals("tcm:18-983", linksNotResolved.toArray()[1]);

        System.out.println("Duration: " + (System.currentTimeMillis() - start) + " ms.");
    }

    @NotNull
    private Map<String, String> getResolvedLinksMap() {
        Map<String, String> batchOfLinks = new LinkedHashMap<>();
        batchOfLinks.put("tcm:15-980", "<link1/>");
        batchOfLinks.put("tcm:17-982", "<link3/>");
        return batchOfLinks;
    }

    @NotNull
    private String getFragments() {
        StringBuilder fragment = new StringBuilder("<p>Text <a data=\"1\" href=\"resolved-link\" data2=\"2\">link text</a> after text</p>");
        fragment.append("<p><a title=\"Unused Component\" href=\"tcm:15-980\">UNRESOLVED LINK1</a><!--CompLink tcm:15-980--> </p>" +
                "<p><a title=\"Unused Component\" href=\"tcm:16-981\">UNRESOLVED LINK2</a><!--CompLink tcm:16-981--> </p>" +
                "<p><a title=\"Unused Component\" href=\"tcm:17-982\">UNRESOLVED LINK3</a><!--CompLink tcm:17-982--> </p>" +
                "<p><a title=\"Unused Component\" href=\"tcm:18-983\">UNRESOLVED LINK4</a><!--CompLink tcm:18-983--> </p>");
        return fragment.toString();
    }
}