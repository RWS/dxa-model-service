package com.sdl.dxa.tridion.linking.processors;

import com.sdl.dxa.tridion.linking.api.processors.LinkListProcessor;
import com.sdl.dxa.tridion.linking.RichTextLinkResolver;

import java.util.HashSet;
import java.util.Map;

public class FragmentLinkListProcessor implements LinkListProcessor {

    private RichTextLinkResolver resolver;

    private Map<String, String> fragments;
    private String key;
    private String value;

    public FragmentLinkListProcessor(Map<String, String> fragments,
                                     String key,
                                     String value,
                                     RichTextLinkResolver resolver) {
        this.fragments = fragments;

        this.key = key;
        this.value = value;

        this.resolver = resolver;
    }

    @Override
    public void update(Map<String, String> links) {
        String value = resolver.applyBatchOfLinksStart(this.value, links, new HashSet<>());
        fragments.replace(key, value);
    }
}
