package com.sdl.dxa.tridion.linking.descriptors;

import com.sdl.dxa.tridion.linking.api.processors.LinkProcessor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sdl.web.util.ContentServiceQueryConstants.LINK_TYPE_DYNAMIC_COMPONENT;

public class DynamicComponentLinkDescriptor extends BaseLinkDescriptor {

    private Integer templateId;
    private Integer componentId;

    private static final Pattern SEPARATE_IDS =
            // <p>Text <a data="1" href="tcm:1-2" data2="2">link text</a><!--CompLink tcm:1-2--> after text</p>
            // tcmUri: tcm:1-2
            Pattern.compile("(?<componentId>\\d+)-(?<templateId>\\d+)",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);


    public DynamicComponentLinkDescriptor(Integer publicationId, Integer sourcePageId, LinkProcessor linkProcessor) {
        super(publicationId, sourcePageId, linkProcessor, LINK_TYPE_DYNAMIC_COMPONENT);

        String dynamicId = linkProcessor.getId();

        this.templateId = extractGroupFromId(dynamicId, "templateId");
        this.componentId = extractGroupFromId(dynamicId, "componentId");
    }

    @Override
    public Integer getComponentId() {
        return this.componentId;
    }

    @Override
    public Integer getTemplateId() {
        return this.templateId;
    }

    @Override
    public String getLinkId() {
        return String.format("%s-%s-%s-%s", this.getPublicationId(), this.getPageId(),
                this.getComponentId(), this.getTemplateId());
    }

    private int extractGroupFromId(String id, String group) {
        int failed = -1;
        if (id == null) {
            return failed;
        }

        Matcher matcher = SEPARATE_IDS.matcher(id);
        if (matcher.matches()) {
            String match = matcher.group(group);
            return match != null ? Integer.parseInt(match) : -2;
        }
        return failed;
    }
}

