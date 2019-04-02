package com.sdl.dxa.modelservice.service.processing.conversion.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.dd4t.contentmodel.Component;
import org.dd4t.contentmodel.Field;
import org.dd4t.contentmodel.impl.XhtmlField;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * To be removed in TSI-2698.
 */
@Deprecated
public class AdoptedRichTextField extends XhtmlField implements Field {

    @JsonProperty("RichTextValues")
    private List<Object> richTextValues;

    public List<Object> getRichTextValues() {
        return richTextValues != null ? richTextValues : Collections.emptyList();
    }

    @SuppressWarnings("Duplicates")
    public void setRichTextValues(List<Object> richTextValues) {
        List<Component> components = new ArrayList<>();
        List<String> strings = new ArrayList<>();
        for (Object richTextValue : richTextValues) {
            if (richTextValue instanceof Component) {
                components.add((Component) richTextValue);
            } else if (richTextValue instanceof String) {
                strings.add((String) richTextValue);
            } else {
                throw new IllegalArgumentException("RichText Field may only contain Strings of Components, found " + richTextValue.getClass());
            }
        }

        setLinkedComponentValues(components);
        setTextValues(strings);
        this.richTextValues = richTextValues;
    }

    @Override
    @JsonProperty("RichTextValues")
    public List<Object> getValues() {
        List<Object> values = new ArrayList<>();
        values.addAll(getRichTextValues());
        return values;
    }
}
