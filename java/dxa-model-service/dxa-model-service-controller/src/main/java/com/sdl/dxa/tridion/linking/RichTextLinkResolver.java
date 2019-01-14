package com.sdl.dxa.tridion.linking;

import com.google.common.base.Strings;
import com.sdl.dxa.modelservice.service.ConfigService;
import com.sdl.dxa.tridion.linking.api.BatchLinkResolver;
import com.sdl.webapp.common.api.content.LinkResolver;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
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
public class RichTextLinkResolver {

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
            Pattern.compile("(?<before>.*(xlink|xmlns):(?<tag>href=)(?<value>\"[^\"]*?\"))(?<after>.*)",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

    private static final Pattern COLLECT_LINK =
            // <p>Text <a data="1" href="tcm:1-2" data2="2">link text</a><!--CompLink tcm:1-2--> after text</p>
            // tcmUri: tcm:1-2
            // Original, slow: ".*?<a[^>]*\\shref=\"(?<tcmUri>tcm:\\d+-\\d+)\"[^>]*>"
            Pattern.compile("href=\"(?<tcmUri>tcm:\\d+-\\d+)\"",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

    private static final Pattern START_LINK =
            // <p>Text <a data="1" href="tcm:1-2" data2="2">link text</a><!--CompLink tcm:1-2--> after text</p>
            // beforeWithLink: <p>Text <a data="1" href=
            // before: <p>Text
            // tcmUri: tcm:1-2
            // afterWithLink: " data2="2">link text</a><!--CompLink tcm:1-2--> after text</p>
            // after: link text</a><!--CompLink tcm:1-2--> after text</p>
            Pattern.compile("(?<beforeWithLink>(?<before>.*?)<a[^>]*\\shref=\")(?<tcmUri>tcm:\\d+-\\d+)(?<afterWithLink>\"[^>]*>(?<after>.*))",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

    private static final Pattern END_LINK =
            // <p>Text <a data="1" href="resolved-link" data2="2">link text</a><!--CompLink tcm:1-2--> after text</p>
            // beforeWithLink: <p>Text <a data="1" href="resolved-link" data2="2">link text</a>
            // before: <p>Text <a data="1" href="resolved-link" data2="2">link text
            // tcmUri: tcm:1-2
            // after: after text</p>
            Pattern.compile("(?<beforeWithLink>(?<before>.*?)</a>)<!--CompLink\\s(?<tcmUri>tcm:\\d+-\\d+)-->(?<after>.*)",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

    private final LinkResolver linkResolver;

    private final ConfigService configService;

    private final BatchLinkResolver batchLinkResolver;

    @Autowired
    public RichTextLinkResolver(@Qualifier("dxaLinkResolver") LinkResolver linkResolver, ConfigService configService,
                                BatchLinkResolver batchLinkResolver) {
        this.linkResolver = linkResolver;
        this.configService = configService;
        this.batchLinkResolver = batchLinkResolver;
    }

    /**
     * Processes the fragment as {@link #processFragment(String, int, Set)} just for single fragment.
     *
     * @param fragment       fragment of a rich text to process
     * @param localizationId current localization ID
     * @return modified fragment
     */
    public String processFragment(@NotNull String fragment, int localizationId) {
        return processFragment(fragment, localizationId, new HashSet<>());
    }

    /**
     * Processes a rich text fragment trying to resolve links from it. In case of non-resolvable link, puts it into buffer.
     * <p>Reuse the same buffer if you have multiple fragments with possible links start/end parts in different fragments:</p>
     * <pre><code>
     *     RichTextLinkResolver resolver = new RichTextLinkResolver();
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
     * @param localizationId    current localization ID
     * @param notResolvedBuffer buffer to put non resolvable links to, make sure it's modifiable
     * @return modified fragment
     */
    public String processFragment(@NotNull String fragment, int localizationId, @NotNull Set<String> notResolvedBuffer) {

        log.trace("RichTextResolver, resolve = {}, remove = {}, input fragment: '{}'",
                configService.getDefaults().isRichTextResolve(), configService.getDefaults().isRichTextXmlnsRemove(), fragment);

        if (!configService.getDefaults().isRichTextResolve()) {
            log.debug("RichText link resolving is turned off, don't do anything");
            return fragment;
        }

        final String _fragment = configService.getDefaults().isRichTextXmlnsRemove() ? dropXlmns(fragment) : generateHref(fragment);

        return processEndLinks(
                processStartLinks(_fragment, localizationId, notResolvedBuffer),
                notResolvedBuffer);
    }

    /**
     * Cleans up HTML fragment removing attributes from the {@code xlink:} namespace which may be found e.g. in DD4T representation.
     *
     * @param fragment rich text fragment to clean up
     * @return the same fragment with removed attributes
     */
    private String dropXlmns(String fragment) {
        Matcher matcher = XMLNS_FOR_REMOVAL.matcher(fragment);

        if (matcher.find()) {
            return matcher
                    .replaceAll("")
                    .replaceAll(SPACES_FOR_REMOVAL.pattern(), "$1$2");
        }

        return fragment;
    }

    /**
     * Generates HREF based on xlmns:href.
     *
     * @param fragment rich text fragment to process
     * @return the same fragment with href added
     */
    private String generateHref(String fragment) {
        String _fragment = fragment;
        Matcher matcher = XLINK_XLMNS_FOR_GENERATING_HREF.matcher(_fragment);

        if (matcher.matches()) {
            Matcher hrefAlreadyThere = START_LINK.matcher(_fragment);
            if (!hrefAlreadyThere.matches()) {
                _fragment = matcher.group("before") + " " + matcher.group("tag") + matcher.group("value") + matcher.group("after");
            }
        }

        return _fragment;
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

        fragment = configService.getDefaults().isRichTextXmlnsRemove() ? dropXlmns(fragmentString) : generateHref(fragmentString);

        log.debug("Fragment is: {}", fragment);

        long start = System.currentTimeMillis();

        Matcher startMatcher = COLLECT_LINK.matcher(fragment);
        while (startMatcher.find()) {
            links.add(startMatcher.group("tcmUri"));
        }

        log.debug(">>> matching took: {} ms.", ((System.currentTimeMillis() - start)));
        log.debug(">>> Found {} links", links.size());
        return links;
    }

    @NotNull
    private String processStartLinks(@NotNull String stringFragment, int localizationId, @NotNull Set<String> linksNotResolved) {
        String fragment = stringFragment;
        Matcher startMatcher = START_LINK.matcher(fragment);

        while (startMatcher.matches()) {
            String tcmUri = startMatcher.group("tcmUri");
            String link = linkResolver.resolveLink(tcmUri, String.valueOf(localizationId), true);
            if (Strings.isNullOrEmpty(link)) {
                log.info("Cannot resolve link to {}, suppressing link", tcmUri);
                fragment = startMatcher.group("before") + startMatcher.group("after");
                linksNotResolved.add(tcmUri);
            } else {
                log.debug("Resolved link to {} as {}", tcmUri, link);
                fragment = startMatcher.group("beforeWithLink") + link + startMatcher.group("afterWithLink");
            }

            startMatcher = START_LINK.matcher(fragment);
        }

        return fragment;
    }

    @NotNull
    public String applyBatchOfLinksStart(@NotNull String stringFragment, @NotNull Map<String, String> batchOfLinks, @NotNull Set<String> linksNotResolved) {
        String fragment = stringFragment;
        Matcher startMatcher = START_LINK.matcher(fragment);

        if (!configService.getDefaults().isRichTextResolve()) {
            log.debug("RichText link resolving is turned off, don't do anything");
            return fragment;
        }

        fragment = configService.getDefaults().isRichTextXmlnsRemove() ? dropXlmns(stringFragment) : generateHref(stringFragment);

        while (startMatcher.matches()) {
            String tcmUri = startMatcher.group("tcmUri");
            String link = batchOfLinks.get(tcmUri);
            if (Strings.isNullOrEmpty(link)) {
                log.info("Link to {} has not been resolved, suppressing link", tcmUri);
                fragment = startMatcher.group("before") + startMatcher.group("after");
                linksNotResolved.add(tcmUri);
            } else {
                log.debug("Link to {} has been resolved as {}", tcmUri, link);
                fragment = startMatcher.group("beforeWithLink") + link + startMatcher.group("afterWithLink");
            }

            startMatcher = START_LINK.matcher(fragment);
        }

        return processEndLinks(fragment, linksNotResolved);
    }

    @NotNull
    private String processEndLinks(@NotNull String stringFragment, @NotNull Set<String> linksNotResolved) {
        String fragment = stringFragment;
        Matcher endMatcher = END_LINK.matcher(fragment);
        while (endMatcher.matches()) {
            String tcmUri = endMatcher.group("tcmUri");
            if (linksNotResolved.contains(tcmUri)) {
                log.trace("Tcm URI {} was not resolved, removing end </a> with marker", tcmUri);
                fragment = endMatcher.group("before") + endMatcher.group("after");
            } else {
                log.trace("Tcm URI {} was resolved, removing only marker, leaving </a>", tcmUri);
                fragment = endMatcher.group("beforeWithLink") + endMatcher.group("after");
            }

            endMatcher = END_LINK.matcher(fragment);
        }

        return fragment;
    }
}
