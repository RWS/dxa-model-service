package com.sdl.dxa.tridion.linking.processors;

import com.sdl.dxa.api.datamodel.model.RichTextData;
import com.sdl.dxa.tridion.linking.RichTextLinkResolver;
import com.sdl.dxa.tridion.linking.api.processors.LinkListProcessor;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FragmentListProcessor implements LinkListProcessor {

    private RichTextLinkResolver resolver;

    private RichTextData model;

    private String uuid;

    private String fragment;

    public FragmentListProcessor(RichTextData model,
                                 String fragment,
                                 RichTextLinkResolver resolver) {
        this.model = model;
        this.fragment = fragment;
        this.resolver = resolver;
    }

    @Override
    public void update(Map<String, String> links) {
        List<Object> fragmentList = this.model
                .getValues().stream()
                .map(fragment -> this.resolver.applyBatchOfLinksStart(this.fragment, links, new HashSet<>())).collect(Collectors.toList());

        this.model.setFragments(fragmentList);
    }
}
