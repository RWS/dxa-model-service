package com.sdl.dxa.tridion.linking.processors;

import com.sdl.dxa.tridion.linking.RichTextLinkResolver;
import com.sdl.dxa.tridion.linking.api.processors.LinkListProcessor;

import java.util.HashSet;
import java.util.Map;

public class FragmentLinkListProcessor implements LinkListProcessor {

    private RichTextLinkResolver resolver;

    private Map<String, String> model;
    private String key;
    private String value;

    public FragmentLinkListProcessor(Map<String, String> map, String key, String value, RichTextLinkResolver resolver) {
        this.model = map;

        this.key = key;
        this.value = value;

        this.resolver = resolver;
    }

    @Override
    public void update(Map<String, String> links) {
        this.model.replace(this.key, this.resolver.applyBatchOfLinksStart(this.value, links, new HashSet<>()));
    }
}
