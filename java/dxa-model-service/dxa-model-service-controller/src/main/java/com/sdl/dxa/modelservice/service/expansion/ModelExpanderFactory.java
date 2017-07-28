package com.sdl.dxa.modelservice.service.expansion;

import com.sdl.dxa.common.dto.PageRequestDto;
import com.sdl.dxa.modelservice.service.ConfigService;
import com.sdl.dxa.modelservice.service.ModelService;
import com.sdl.dxa.tridion.linking.RichTextLinkResolver;
import com.sdl.webapp.common.api.content.LinkResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Instantiates a {@link PageModelExpander} with all the dependencies injected.
 */
@Component
public class ModelExpanderFactory {

    private final ModelService modelService;

    private final RichTextLinkResolver richTextLinkResolver;

    private final LinkResolver linkResolver;

    private final ConfigService configService;

    @Autowired
    public ModelExpanderFactory(ModelService modelService,
                                RichTextLinkResolver richTextLinkResolver,
                                LinkResolver linkResolver,
                                ConfigService configService) {
        this.modelService = modelService;
        this.richTextLinkResolver = richTextLinkResolver;
        this.linkResolver = linkResolver;
        this.configService = configService;
    }

    /**
     * Instantiates a {@link PageModelExpander}.
     *
     * @param pageRequestDto current page request
     * @return an instance of {@link PageModelExpander}
     */
    public PageModelExpander getFor(PageRequestDto pageRequestDto) {
        return new PageModelExpander(pageRequestDto, modelService, richTextLinkResolver, linkResolver, configService);
    }
}
