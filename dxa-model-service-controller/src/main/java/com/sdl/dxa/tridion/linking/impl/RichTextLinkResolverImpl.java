package com.sdl.dxa.tridion.linking.impl;

import com.google.common.base.Strings;
import com.sdl.dxa.modelservice.service.ConfigService;
import com.sdl.dxa.tridion.linking.RichTextLinkResolver;
import com.sdl.webapp.common.api.content.LinkResolver;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Accepts a a String fragment and resolves possible links from it.
 */
@Component
@Slf4j
public class RichTextLinkResolverImpl implements RichTextLinkResolver {

    /**
     * Matches {@code xmlns:xlink} TDD and {@code xlink:} and namespace text fragment.
     */
    private static final Pattern XMLNS_FOR_REMOVAL =
            Pattern.compile("(xlink:|xmlns:?[^\"]*\"[^\"]*\".*?)",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

    private static final Pattern SPACES_FOR_REMOVAL =
            Pattern.compile("\\s+(\\s)|\\s(>)",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

    private static final Pattern XLINK_XLMNS_FOR_GENERATING_HREF =
            Pattern.compile("(?<before><a[^>]*?\\s)(?<prefix>(xlink|xmlns):)(?<tag>href=)(?<value>\"[^\"]*?\")(?<after>[^>]*?>)",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

    private static final Pattern COLLECT_LINK =
            // <p>Text <a data="1" href="tcm:1-2" data2="2">link text</a><!--CompLink tcm:1-2--> after text</p>
            // tcmUri: tcm:1-2
            Pattern.compile("href=\"(?<tcmUri>tcm:\\d++-\\d++)\"",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

    private static final Pattern FULL_LINK =
            // <p>Text <a data="1" href="tcm:1-2" data2="2">link text</a><!--CompLink tcm:1-2--> after text</p>
            // beforeWithLink: <p>Text <a data="1" href=
            // before: <p>Text
            // tcmUri: tcm:1-2
            // afterWithLink: " data2="2">link text</a><!--CompLink tcm:1-2--> after text</p>
            // after: link text</a><!--CompLink tcm:1-2--> after text</p>
            //                                       <a           href= "           tcm:1    -3                       "      >          link2                </a>
            Pattern.compile("(?<openingTagStart><a[^>]*?\\s++href=\")(?<tcmUri>tcm:\\d++-\\d++)(?<openingTagEnd>\"[^>]*?>)(?<linkText>.*?)(?<closingTag></a>)(<!--CompLink\\s++\\2-->)?",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

    public static final int BUFFER_CAPACITY = 1024;

    private final LinkResolver linkResolver;

    private final ConfigService configService;

    private interface ResolveOrGetLink {
        String getByTcmUri(String tcmUrl, int localizationId);
    }

    @Autowired
    public RichTextLinkResolverImpl(@Qualifier("dxaLinkResolver") LinkResolver linkResolver, ConfigService configService) {
        this.linkResolver = linkResolver;
        this.configService = configService;
    }

    /**
     * Processes a rich text fragment trying to resolve links from it. In case of non-resolvable link, puts it into buffer.
     * <p>Reuse the same buffer if you have multiple fragments with possible same links parts in different fragments:</p>
     * <pre><code>
     *     RichTextLinkResolver resolver = new RichTextLinkResolverImpl();
     *     Set&lt;String&gt; buffer = new HashSet&lt;&gt;();
     *     String[] fragments = new String[]{"&lt;a href="tcm:1-2"&gt;text", "&lt;/a&gt;&lt;!--CompLink tcm:1-2--&gt;"};
     *     String[] resolved = new String[2];
     *     for (int i = 0; i &lt; fragments.length; i++) {
     *          resolved[i] = resolver.processFragment(fragments[i], buffer);
     *     }
     *     // here
     *     //   resolved = {"&lt;a href="resolved-link"&gt;text", "&lt;/a&gt;"};
     *     // or if link if unresolvable
     *     //   resolved = {"text", ""};</code></pre>
     *
     * @param fragment          fragment of a rich text to process
     * @param notResolvedBuffer buffer to put non resolvable links to, make sure it's modifiable
     * @return modified fragment
     */
    public String processFragment(@NotNull String fragment, @NotNull Map<String, String> batchOfLinks, @NotNull Set<String> notResolvedBuffer) {

        log.trace("RichTextResolver, resolve = {}, remove = {}, input fragment: '{}'",
                configService.getDefaults().isRichTextResolve(), configService.getDefaults().isRichTextXmlnsRemove(), fragment);

        if (!configService.getDefaults().isRichTextResolve()) {
            log.debug("RichText link resolving is turned off, don't do anything");
            return fragment;
        }

        String fragmentToProcess = configService.getDefaults().isRichTextXmlnsRemove()
                ? dropXlmns(fragment)
                : generateHref(fragment);
        String result = processLinks(fragmentToProcess, batchOfLinks, notResolvedBuffer);
        Matcher withoutExcessiveSpaces = SPACES_FOR_REMOVAL.matcher(result);
        return withoutExcessiveSpaces.replaceAll("$1");
    }

    /**
     * Cleans up HTML fragment removing attributes from the {@code xlink:} namespace which may be found e.g. in DD4T representation.
     *
     * @param fragment rich text fragment to clean up
     * @return the same fragment with removed attributes
     */
    String dropXlmns(String fragment) {
        Matcher matcher = XMLNS_FOR_REMOVAL.matcher(fragment);
        return matcher
                .replaceAll("")
                .replaceAll(SPACES_FOR_REMOVAL.pattern(), "$1$2");
    }

    /**
     * Generates HREF based on xlmns:href.
     *
     * @param fragment rich text fragment to process
     * @return the same fragment with href added
     */
    String generateHref(String fragment) {
        Matcher matcher = XLINK_XLMNS_FOR_GENERATING_HREF.matcher(fragment);

        StringBuffer result = new StringBuffer(BUFFER_CAPACITY);
        while (matcher.find()) {
            String replacement = matcher.group("before") + matcher.group("prefix") + matcher.group("tag") + matcher.group("value") + " " + matcher.group("tag") + matcher.group("value") + matcher.group("after");
            if (matcher.group(0).contains(" href=" + matcher.group("value"))) {
                //already has 'xmlns:href' and 'ref', do not need to append 'href' at all
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
            } else {
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Generates HREF based on xlmns:href.
     *
     * @param fragmentString rich text fragment to process
     * @return Parses all the links out of the rich text fragment and returns them as a list
     */
    @NotNull
    public List<String> retrieveAllLinksFromFragment(@NotNull String fragmentString) {
        String fragment;

        List<String> links = new ArrayList<>();

        if (!configService.getDefaults().isRichTextResolve()) {
            log.debug("RichText link resolving is turned off, don't do anything");
            return links;
        }

        if (!fragmentString.contains("href=\"tcm")) {
            log.debug("No tcms in here to process.");
            return links;
        }

        fragment = configService.getDefaults().isRichTextXmlnsRemove()
                ? dropXlmns(fragmentString)
                : generateHref(fragmentString);

        log.debug("Fragment is: {}", fragment);

        long start = System.currentTimeMillis();

        Matcher startMatcher = COLLECT_LINK.matcher(fragment);
        while (startMatcher.find()) {
            links.add(startMatcher.group("tcmUri"));
        }

        log.debug(">>> matching took: {} ms.", (System.currentTimeMillis() - start));
        log.debug(">>> Found {} links", links.size());
        return links;
    }

    @NotNull
    String processLinks(@NotNull String fragment, @NotNull Map<String, String> batchOfLinks, @NotNull Set<String> linksNotResolved) {
        if (fragment.isEmpty()) return "";
        fragment += "</a>";
        StringBuffer result = new StringBuffer(BUFFER_CAPACITY);
        Matcher startMatcher = FULL_LINK.matcher(fragment);
        while (startMatcher.find()) {
            String tcmUri = startMatcher.group("tcmUri");
            String link = batchOfLinks.get(tcmUri);
            if (Strings.isNullOrEmpty(link)) {
                log.info("Cannot resolve link to {}, suppressing link", tcmUri);
                startMatcher.appendReplacement(result, Matcher.quoteReplacement(startMatcher.group("linkText") + "</a><!--CompLink " + tcmUri + "-->"));
                linksNotResolved.add(tcmUri);
            } else {
                log.debug("Resolved link to {} as {}", tcmUri, link);
                String replacement = startMatcher.group("openingTagStart") + link + startMatcher.group("openingTagEnd") + startMatcher.group("linkText") + startMatcher.group("closingTag") + "</a><!--CompLink " + tcmUri + "-->";
                startMatcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            }
        }
        startMatcher.appendTail(result);
        String finalResult = result.toString().replaceAll("</a>$","");
        return finalResult.replaceAll("</a><!--CompLink\\s++tcm:\\d++-\\d++-->", "");
    }
}
