package com.sdl.dxa.tridion.linking;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IRichTextLinkResolver {
    String processFragment(@NotNull String fragment, int localizationId);
    String processFragment(@NotNull String fragment, int localizationId, @NotNull Set<String> notResolvedBuffer);
    List<String> retrieveAllLinksFromFragment(@NotNull String fragmentString);
    String applyBatchOfLinksStart(@NotNull String stringFragment, @NotNull Map<String, String> batchOfLinks, @NotNull Set<String> linksNotResolved);
}
