package com.sdl.dxa.tridion.linking;

import com.google.common.base.Strings;
import com.sdl.webapp.common.api.content.LinkResolver;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Accepts a a String fragment and resolves possible links from it.
 */
@Component
@Slf4j
public class RichTextLinkResolver {

    private static final Pattern START_LINK =
            // <p>Text <a data="1" href="tcm:1-2" data2="2">link text</a><!--CompLink tcm:1-2--> after text</p>
            // beforeWithLink: <p>Text <a data="1" href=
            // before: <p>Text
            // tcmUri: tcm:1-2
            // afterWithLink: " data2="2">link text</a><!--CompLink tcm:1-2--> after text</p>
            // after: link text</a><!--CompLink tcm:1-2--> after text</p>
            Pattern.compile("(?<beforeWithLink>(?<before>.*?)<a[^>]+href=\")(?<tcmUri>tcm:\\d+-\\d+)(?<afterWithLink>\"[^>]*>(?<after>.*))",
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

    @Autowired
    public RichTextLinkResolver(LinkResolver linkResolver) {
        this.linkResolver = linkResolver;
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
        return processEndLinks(
                processStartLinks(fragment, localizationId, notResolvedBuffer),
                notResolvedBuffer);
    }

    @NotNull
    private String processEndLinks(@NotNull String stringFragment, @NotNull Set<String> linksNotResolved) {
        String fragment = stringFragment;
        Matcher endMatcher = END_LINK.matcher(fragment);
        while (endMatcher.matches()) {
            String tcmUri = endMatcher.group("tcmUri");
            if (linksNotResolved.contains(tcmUri)) {
                log.trace("Tcm Uri {} was not resolved, removing end </a> with marker");
                fragment = endMatcher.group("before") + endMatcher.group("after");
            } else {
                log.trace("Tcm Uri {} was resolved, removing only marker, leaving </a>");
                fragment = endMatcher.group("beforeWithLink") + endMatcher.group("after");
            }

            endMatcher = END_LINK.matcher(fragment);
        }
        return fragment;
    }

    @NotNull
    private String processStartLinks(@NotNull String stringFragment, int localizationId, @NotNull Set<String> linksNotResolved) {
        String fragment = stringFragment;
        Matcher startMatcher = START_LINK.matcher(fragment);

        while (startMatcher.matches()) {
            String tcmUri = startMatcher.group("tcmUri");
            String link = linkResolver.resolveLink(tcmUri, String.valueOf(localizationId));
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
}
