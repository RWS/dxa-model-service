package com.sdl.dxa.utils;

import com.sdl.dxa.api.datamodel.model.RichTextData;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.List;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

/**
 * FragmentUtils.
 */
public class FragmentUtils {

    private FragmentUtils() {

    }

    public static List<Object> assignUUIDsToRichTextFragments(RichTextData richTextData) {
        return richTextData.getValues()
                .stream()
                .map(fragment -> {
                    if (fragment instanceof String) {
                        String uuid = UUID.randomUUID().toString();
                        String fragmentString = String.valueOf(fragment);
                        return new ImmutablePair<>(uuid, fragmentString);
                    } else {
                        return fragment;
                    }
                }).collect(toList());
    }
}
