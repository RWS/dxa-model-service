package com.sdl.dxa.modelservice.service.processing.conversion.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Helper class to parse {@code navigation.json} in order to get Structure Group during R2 to DD4T conversion.
 */
@Value
public class LightSitemapItem {

    @JsonProperty("Id")
    private String id;

    @JsonProperty("Title")
    private String title;

    @JsonProperty("Items")
    @JsonDeserialize(as = LinkedHashSet.class)
    private Set<LightSitemapItem> items;

    public LightSitemapItem() {
        //Default constructor, so this class can be deserialized by Jackson.
        id = null;
        title = null;
        items = null;
    }

    @NotNull
    public Optional<LightSitemapItem> findWithId(@NotNull String id) {
        if (id.matches(this.id + "/?")) {
            return Optional.of(this);
        }

        for (LightSitemapItem item : items) {
            Optional<LightSitemapItem> subItem = item.findWithId(id);
            if (subItem.isPresent()) {
                return subItem;
            }
        }
        return Optional.empty();
    }
}
