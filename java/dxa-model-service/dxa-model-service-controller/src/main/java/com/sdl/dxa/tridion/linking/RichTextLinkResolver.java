package com.sdl.dxa.tridion.linking;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RichTextLinkResolver {
    /**
     * Processes the fragment as {@link #processFragment(String, int, Set)} just for single fragment.
     *
     * @param fragment       fragment of a rich text to process
     * @param localizationId current localization ID
     * @return modified fragment
     */
    String processFragment(@NotNull String fragment, int localizationId);

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
    String processFragment(@NotNull String fragment, int localizationId, @NotNull Set<String> notResolvedBuffer);
    List<String> retrieveAllLinksFromFragment(@NotNull String fragmentString);
    String applyBatchOfLinksStart(@NotNull String stringFragment, @NotNull Map<String, String> batchOfLinks, @NotNull Set<String> linksNotResolved);
}
