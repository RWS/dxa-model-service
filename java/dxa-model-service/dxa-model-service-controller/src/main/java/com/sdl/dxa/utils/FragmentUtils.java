package com.sdl.dxa.utils;

import com.sdl.dxa.api.datamodel.model.RichTextData;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

/**
 * FragmentUtils.
 */
public class FragmentUtils {
    private static UUIDGenerator generator = new UUIDGenerator(new SecureRandom());

    private FragmentUtils() {
    }

    public static List<Object> assignUUIDsToRichTextFragments(RichTextData richTextData) {
        return richTextData.getValues().stream()
                .map(fragment -> {
                    if (fragment instanceof String) {
                        String uuid = generator.generate().toString();
                        String fragmentString = String.valueOf(fragment);
                        return new ImmutablePair<>(uuid, fragmentString);
                    }
                    return fragment;
                }).collect(toList());
    }
}
