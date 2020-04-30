package com.sdl.dxa.tridion.linking.impl;

import com.google.common.collect.Lists;
import com.sdl.dxa.modelservice.service.ConfigService;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class RichTextLinkResolverTest {

    @Mock
    private ConfigService configService;

    @InjectMocks
    private RichTextLinkResolverImpl richTextLinkResolver;

    private HashMap<String, String> batchOfLinks;

    @Before
    public void init() {
        batchOfLinks = new HashMap<>();
        batchOfLinks.put("tcm:1-2", "");
        batchOfLinks.put("tcm:1-3", "");
        batchOfLinks.put("tcm:1-11", "resolved-link");
        batchOfLinks.put("tcm:1-12", "resolved-link.html");
        batchOfLinks.put("tcm:15-980", "<link1/>");
        batchOfLinks.put("tcm:17-982", "<link3/>");

        ConfigService.Defaults defaults = new ConfigService.Defaults(null, null);
        defaults.setRichTextXmlnsRemove(true);
        defaults.setRichTextResolve(true);
        Mockito.when(configService.getDefaults()).thenReturn(defaults);
    }

    @Test
    public void shouldSuppressBrokenLinks() {
        batchOfLinks.put("tcm:1-3", "resolved_link_url");

        //given
        List<String> values = Lists.newArrayList("<p>Text1-2 <a href=\"tcm:1-2\">(link text)1-2</a><!--CompLink tcm:1-2--> text1-2 ",
                "<a href =  \"tcm:1-3\">",
                "(link text)3 bad</a><!--CompLink tcm:1-3-->",
                "</p>",
                "<a href=\"tcm:1-3\">(link text)3</a><!--CompLink tcm:1-3-->",
                " text2");

        //when
        Set<String> notResolvedBuffer = new HashSet<>();
        String result = values.stream()
                .map(String.class::cast)
                .map(fragment -> richTextLinkResolver.processFragment(fragment, batchOfLinks, notResolvedBuffer))
                .collect(Collectors.joining());

        //then
        assertEquals("<p>Text1-2 (link text)1-2 text1-2 <a href = \"resolved_link_url\">(link text)3 bad</a></p><a href=\"resolved_link_url\">(link text)3</a> text2", result);
    }

    @Test
    public void shouldResolveLinks_IfSameTcmUri_IsDoubledInSameFragment() {
        //given
        String string = "<p>Text_before <a href=\"tcm:1-2\">(link text 1)</a><!--CompLink tcm:1-2--> text_between " +
                "<a href=\"tcm:1-2\">(link text 2)</a><!--CompLink tcm:1-2--> text_after </p>";

        //when
        String result = richTextLinkResolver.processFragment(string, batchOfLinks, new HashSet<>());

        //then
        assertEquals("<p>Text_before (link text 1) text_between (link text 2) text_after </p>", result);
    }

    @Test
    public void shouldResolveLinks_AndRemoveMarkers() {
        //given
        String fragment = "<p>Text <a href=\"tcm:1-11\">link text</a><!--CompLink tcm:1-11--></p>";

        //when
        String result = richTextLinkResolver.processFragment(fragment, batchOfLinks, new HashSet<>());

        //then
        assertEquals("<p>Text <a href=\"resolved-link\">link text</a></p>", result);
    }

    @Test
    public void shouldResolveLinks_AndLeaveExtension() {
        //given
        String fragment = "<p>Text <a href=\"tcm:1-12\">link text</a><!--CompLink tcm:1-12--></p>";

        //when
        String result = richTextLinkResolver.processFragment(fragment, batchOfLinks, new HashSet<>());

        //then
        assertEquals("<p>Text <a href=\"resolved-link.html\">link text</a></p>", result);
    }

    @Test
    public void shouldResolveLinks_InFragmentWithLineBreaks() {
        //given
        String fragment = "<p>\nText <a href=\"tcm:1-11\">\nlink text</a><!--CompLink tcm:1-11-->\n</p>";

        //when
        String result = richTextLinkResolver.processFragment(fragment, batchOfLinks, new HashSet<>());

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
        String result = richTextLinkResolver.processFragment(fragment, batchOfLinks, new HashSet<>());

        //then
        assertEquals("<p>\nText <a xlink=\"X link\" data-id=\"ID\" title=\"link title\" href=\"resolved-link\">\nlink text</a>\n</p>", result);
    }

    @Test
    public void shouldNotRemoveNamespaces_IfDisabled() {
        String fragment = "<p>\nText <a xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink=\"X link\" data-id=\"ID\" xlink:title=\"link title\" xlink:href=\"tcm:1-11\">\nlink text</a><!--CompLink tcm:1-11-->\n</p>";
        configService.getDefaults().setRichTextXmlnsRemove(false);

        //when
        String result = richTextLinkResolver.processFragment(fragment, batchOfLinks, new HashSet<>());

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
        String result = richTextLinkResolver.processFragment(fragment, batchOfLinks, new HashSet<>());

        //then
        assertEquals(fragment, result);
    }

    @Test
    public void shouldNotTouchFragment_IfDisabled_AndRemoveDisabled() { // not valid config case
        configService.getDefaults().setRichTextResolve(false);
        configService.getDefaults().setRichTextXmlnsRemove(false);
        String fragment = "<p>Text <a xlink:href=\"tcm:1-11\">link text</a><!--CompLink tcm:1-11--></p>";

        //when
        String result = richTextLinkResolver.processFragment(fragment, batchOfLinks, new HashSet<>());

        //then
        assertEquals(fragment, result);
    }

    @Test
    public void shouldNotRemoveNamespaces_IfDisabled_AndResolve() {
        configService.getDefaults().setRichTextXmlnsRemove(false);
        configService.getDefaults().setRichTextResolve(true);
        String fragment = "<p>Text <a xlink:href=\"tcm:1-11\" xmlns:title=\"title\">link text</a><!--CompLink tcm:1-11--></p>";

        //when
        String result = richTextLinkResolver.processFragment(fragment, batchOfLinks, new HashSet<>());

        //then
        assertEquals("<p>Text <a xlink:href=\"tcm:1-11\" href=\"resolved-link\" xmlns:title=\"title\">link text</a></p>", result);
    }

    @Test
    public void shouldRemoveNamespaces_IfDisabled_AndResolveOnlyHref() {
        configService.getDefaults().setRichTextXmlnsRemove(false);
        configService.getDefaults().setRichTextResolve(true);
        String fragment = "<p>Text <a xlink:href=\"tcm:1-11\" href=\"tcm:1-11\">link text</a><!--CompLink tcm:1-11--></p>";

        //when
        String result = richTextLinkResolver.processFragment(fragment, batchOfLinks, new HashSet<>());

        //then
        assertEquals("<p>Text <a xlink:href=\"tcm:1-11\" href=\"resolved-link\">link text</a></p>", result);
    }

    @Test
    public void shouldAlsoRemove_Xmlns_WithoutSpecificField() {
        //given
        String fragment = "<p xmlns=\"http://www.w3.org/1999/xhtml\">And some content with a <a href=\"tcm:1-11\" title=\"Copy of My article\">link</a> in it!</p>";

        String result = richTextLinkResolver.processFragment(fragment, batchOfLinks, new HashSet<>());

        //then
        assertEquals("<p>And some content with a <a href=\"resolved-link\" title=\"Copy of My article\">link</a> in it!</p>", result);
    }

    @Test
    public void shouldResolveLinks_WhenLinkHasManyAttrs() {
        //given
        String fragment = "<p>\nText <a data-first=\"1\" href=\"tcm:1-11\" data-second=\"2\">\nlink text</a><!--CompLink tcm:1-11-->\n</p>";

        //when
        String result = richTextLinkResolver.processFragment(fragment, batchOfLinks, new HashSet<>());

        //then
        assertEquals("<p>\nText <a data-first=\"1\" href=\"resolved-link\" data-second=\"2\">\nlink text</a>\n</p>", result);
    }

    @Test
    public void spaceBeforeClosingCharacterLeadsToDisappearingIt() {
        //given '     >'
        String fragment = "<p>\nText <a data-first=\"1\" href=\"tcm:1-11\"    \n>link text\n</a>\n</p>";

        //when
        String result = richTextLinkResolver.processFragment(fragment, batchOfLinks, new HashSet<>());

        //then
        assertEquals("<p>\nText <a data-first=\"1\" href=\"resolved-link\">link text\n</a>\n</p>", result);
    }

    @Test
    public void spaceBeforeClosingCharacterLeadsToDisappearingItWhenTwoLinks() {
        //given '     >'
        String fragment = "<p>\nXREF1 <a data-first=\"1\" href       =  \"tcm:1-11\"  >link text1\n</a>\n" +
                "XREF2 <a href=\"tcm:1-11\">link text2\n</a></p>";

        //when
        String result = richTextLinkResolver.processFragment(fragment, batchOfLinks, new HashSet<>());

        //then
        assertEquals("<p>\nXREF1 <a data-first=\"1\" href = \"resolved-link\">link text1\n</a>\n" +
                "XREF2 <a href=\"resolved-link\">link text2\n" +
                "</a></p>", result);
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

        System.out.println("1 Duration: " + (System.currentTimeMillis() - start) + " ms.");
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
        System.out.println("2 Duration: " + end + " ms.");
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
        Set<String> linksNotResolved = new LinkedHashSet<>();
        long start = System.currentTimeMillis();

        String fragments = richTextLinkResolver.processFragment(fragment, batchOfLinks, linksNotResolved);

        System.out.println("3 Duration: " + (System.currentTimeMillis() - start) + " ms.");
        String expected = "<p>Text <a data=\"1\" href=\"resolved-link\" data2=\"2\">link text</a> after text</p><p><a title=\"Unused Component\" href=\"<link1/>\">UNRESOLVED LINK1</a> </p><p>UNRESOLVED LINK2 </p><p><a title=\"Unused Component\" href=\"<link3/>\">UNRESOLVED LINK3</a> </p><p>UNRESOLVED LINK4 </p>";
        assertEquals(expected, fragments);
        assertEquals(2, linksNotResolved.size());
        assertEquals("tcm:16-981", linksNotResolved.toArray()[0]);
        assertEquals("tcm:18-983", linksNotResolved.toArray()[1]);
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

    @NotNull
    private List<String> getFragmentsWithSplittedLinks() {
        List<String> result = new ArrayList<>();
        result.add("<p>Link to <a title='entire link in a fragment' href=\"tcm:1-11\"> not published;</a><!--CompLink tcm:1-11--> (suppressed).</p>" +
                "<p>Link to <a title='entire link in a fragment' href=\"tcm:15-980\">   published;</a><!--CompLink tcm:15-980--> (resolved).</p>" +
                "<p>Link to <a title='splitted link in fragments' href=\"tcm:15-980\"> (image as a ");
        result.add(" link </a><!--CompLink tcm:17-982--> (resolved).</p>" +
                "<p><a title='splitted link in fragments' href=\"tcm:1-11\"> (image as a ");
        result.add(" link</a><!--CompLink tcm:1-11--> (suppressed).</p>");
        return result;
    }

    @Test
    public void testFragmentsSplitted() {

        Map<String, String> batchOfLinks = getResolvedLinksMap();
        Set<String> linksNotResolved = new LinkedHashSet<>();

        String resolvedFragment = richTextLinkResolver.processFragment(getFragmentsWithSplittedLinks().get(0), batchOfLinks, linksNotResolved);
        assertEquals("<p>Link to not published; (suppressed).</p>" +
                "<p>Link to <a title='entire link in a fragment' href=\"/resolved/link/1\"> published;</a> (resolved).</p>" +
                "<p>Link to <a title='splitted link in fragments' href=\"/resolved/link/1\"> (image as a ", resolvedFragment);

        resolvedFragment = richTextLinkResolver.processFragment(getFragmentsWithSplittedLinks().get(1), batchOfLinks, linksNotResolved);
        assertEquals(" link </a> (resolved).</p><p> (image as a ", resolvedFragment);

        resolvedFragment = richTextLinkResolver.processFragment(getFragmentsWithSplittedLinks().get(2), batchOfLinks, linksNotResolved);
        assertEquals(" link</a> (suppressed).</p>", resolvedFragment);

        //demonstrating issue CRQ-15566 (tag </a> following by 'link' left unremoved)
        resolvedFragment = richTextLinkResolver.processFragment(getFragmentsWithSplittedLinks().get(2), batchOfLinks, new HashSet<>());
        assertEquals(" link</a> (suppressed).</p>", resolvedFragment);
    }

    @Test
    public void shouldResolveLinks_SRQ_13454() {
        String[] fragments = new String[]{
                "<div><a href=\"tcm:15-980\">",
                "<img src=\"tcm:17-982\" alt=\"$$$ insurance costs?\"/>",
                " </a><!--CompLink tcm:15-980--></div>\n<h3><a href=\"tcm:1-11\">How much is insurance?</a></h3>"};

        Map<String, String> batchOfLinks = getResolvedLinksMap();
        Set<String> linksNotResolved = new LinkedHashSet<>();

        String resolvedFragment = richTextLinkResolver.processFragment(fragments[0], batchOfLinks, linksNotResolved);
        assertEquals("<div><a href=\"/resolved/link/1\">", resolvedFragment);
        resolvedFragment = richTextLinkResolver.processFragment(fragments[1], batchOfLinks, linksNotResolved);
        assertEquals("<img src=\"tcm:17-982\" alt=\"$$$ insurance costs?\"/>", resolvedFragment);
        resolvedFragment = richTextLinkResolver.processFragment(fragments[2], batchOfLinks, linksNotResolved);
        assertEquals(" </a></div>\n<h3>How much is insurance?</h3>", resolvedFragment);
    }

    @Test
    public void simpleTestEndLink() {
        Map<String, String> batchOfLinks = getResolvedLinksMap();
        Set<String> linksNotResolved = new LinkedHashSet<>();
        String fragment = " </a><!--CompLink tcm:15-980--></div>\n<h3><a href=\"tcm:1-11\">How much is insurance?</a></h3>";
        List<Integer> positionsWhereTagsRemoved = new ArrayList<>();
        //tag a here cannot be resolved, so it should disappear
        String text = richTextLinkResolver.processStartLinks(fragment,
                batchOfLinks,
                linksNotResolved,
                positionsWhereTagsRemoved);
        assertEquals(" </a><!--CompLink tcm:15-980--></div>\n<h3>How much is insurance?</a></h3>", text);
        assertEquals(Lists.newArrayList(42), positionsWhereTagsRemoved);
        //closing tag for removed '<a href' also has to disappear
        text = richTextLinkResolver.processEndLinks(text, positionsWhereTagsRemoved);
        assertEquals(" </a></div>\n<h3>How much is insurance?</h3>", text);
    }

    @Test
    public void simpleTestSeveralLinks() {
        Map<String, String> batchOfLinks = getResolvedLinksMap();
        Set<String> linksNotResolved = new LinkedHashSet<>();
        String fragment = " </a></div><h3><a href=\"tcm:1-11\">How much is insurance?</a></h3>text1" +
                "<a href=\"resolved.jpg\">map</a> text2[map] <a href=\"tcm:1-11\">text3</a> text4";
        List<Integer> positionsWhereTagsRemoved = new ArrayList<>();
        //tag a here cannot be resolved, so it should disappear
        String text = richTextLinkResolver.processStartLinks(fragment,
                batchOfLinks,
                linksNotResolved,
                positionsWhereTagsRemoved);
        assertEquals(" </a></div><h3>How much is insurance?</a></h3>" +
                "text1<a href=\"resolved.jpg\">map</a> text2[map] text3</a> text4", text);
        assertEquals(Lists.newArrayList(15, 93), positionsWhereTagsRemoved);
        //closing tag for removed '<a href' also has to disappear
        text = richTextLinkResolver.processEndLinks(text, positionsWhereTagsRemoved);
        assertEquals(" </a></div><h3>How much is insurance?</h3>" +
                "text1<a href=\"resolved.jpg\">map</a> text2[map] text3 text4", text);
    }

    private Map<String, String> getResolvedLinksMap() {
        Map<String, String> result = new HashMap<>();
        result.put("tcm:15-980", "/resolved/link/1");
        result.put("tcm:17-982", "/resolved/link/2");
        return result;
    }
}