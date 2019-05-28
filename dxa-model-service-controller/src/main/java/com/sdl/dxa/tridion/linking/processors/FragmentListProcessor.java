package com.sdl.dxa.tridion.linking.processors;

import com.sdl.dxa.api.datamodel.model.RichTextData;
import com.sdl.dxa.tridion.linking.RichTextLinkResolver;
import com.sdl.dxa.tridion.linking.api.processors.LinkListProcessor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class FragmentListProcessor implements LinkListProcessor {

    private RichTextLinkResolver resolver;

    private RichTextData model;

    public FragmentListProcessor(RichTextData model,
                                 RichTextLinkResolver resolver) {
        this.model = model;
        this.resolver = resolver;
    }

    @Override
    public void update(Map<String, String> links) {
        List<Object> resolvedFragments = new ArrayList<>();
        for (Object fragment : model.getFragments()) {
            if (fragment instanceof String) {
                String fragmentString = (String) fragment;
                String resolvedFragment = this.resolver.processFragment(fragmentString, links, new HashSet<>());
                resolvedFragments.add(resolvedFragment);
            } else {
                resolvedFragments.add(fragment);
            }
        }
        this.model.setFragments(resolvedFragments);
    }
}
