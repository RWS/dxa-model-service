package com.sdl.dxa.tridion.linking.processors;

import com.sdl.dxa.api.datamodel.model.RichTextData;
import com.sdl.dxa.tridion.linking.RichTextLinkResolver;
import com.sdl.dxa.tridion.linking.api.processors.LinkListProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FragmentListProcessor implements LinkListProcessor {

    private RichTextLinkResolver resolver;

    private RichTextData model;

    public FragmentListProcessor(RichTextData model,
                                 RichTextLinkResolver resolver) {
        this.model = model;
        this.resolver = resolver;
    }

    @Override
    public void update(Map<String, String> links, Set<String> notResolvedLinks) {
        List<Object> resolvedFragments = new ArrayList<>();
        for (Object fragment : model.getFragments()) {
            if (fragment instanceof String) {
                String fragmentString = (String) fragment;
                String resolvedFragment = this.resolver.processFragment(fragmentString, links, notResolvedLinks);
                resolvedFragments.add(resolvedFragment);
            } else {
                resolvedFragments.add(fragment);
            }
        }
        this.model.setFragments(resolvedFragments);
    }
}
