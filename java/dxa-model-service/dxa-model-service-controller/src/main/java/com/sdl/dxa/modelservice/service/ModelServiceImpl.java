package com.sdl.dxa.modelservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdl.dxa.api.datamodel.model.EntityModelData;
import com.sdl.dxa.api.datamodel.model.PageModelData;
import com.sdl.dxa.api.datamodel.model.RegionModelData;
import com.sdl.dxa.api.datamodel.model.ViewModelData;
import com.sdl.dxa.common.dto.EntityRequestDto;
import com.sdl.dxa.common.dto.PageRequestDto;
import com.sdl.dxa.modelservice.service.processing.conversion.ToDd4tConverter;
import com.sdl.dxa.modelservice.service.processing.conversion.ToR2Converter;
import com.sdl.dxa.modelservice.service.processing.expansion.PageModelExpander;
import com.sdl.dxa.tridion.linking.RichTextLinkResolver;
import com.sdl.webapp.common.api.content.ContentProviderException;
import com.sdl.webapp.common.api.content.LinkResolver;
import com.sdl.webapp.common.exceptions.DxaItemNotFoundException;
import com.sdl.webapp.common.util.TcmUtils;
import com.tridion.dcp.ComponentPresentation;
import com.tridion.dcp.ComponentPresentationFactory;
import lombok.extern.slf4j.Slf4j;
import org.dd4t.contentmodel.Page;
import org.dd4t.contentmodel.impl.PageImpl;
import org.dd4t.core.exceptions.SerializationException;
import org.dd4t.databind.DataBindFactory;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Iterator;

/**
 * Service capable to load content and construct {@code models} out of it.
 */
@Slf4j
@Service
public class ModelServiceImpl implements ModelService {

    private final ObjectMapper objectMapper;

    private final LinkResolver linkResolver;

    private final ConfigService configService;

    private final ContentService contentService;

    private final ToDd4tConverter toDd4tConverter;

    private final ToR2Converter toR2Converter;

    private final RichTextLinkResolver richTextLinkResolver;

    @Autowired
    public ModelServiceImpl(@Qualifier("dxaR2ObjectMapper") ObjectMapper objectMapper,
                            LinkResolver linkResolver,
                            ConfigService configService,
                            ContentService contentService,
                            ToDd4tConverter toDd4tConverter,
                            ToR2Converter toR2Converter,
                            RichTextLinkResolver richTextLinkResolver) {
        this.objectMapper = objectMapper;
        this.linkResolver = linkResolver;
        this.configService = configService;
        this.contentService = contentService;
        this.toDd4tConverter = toDd4tConverter;
        this.toR2Converter = toR2Converter;
        this.richTextLinkResolver = richTextLinkResolver;
    }

    /**
     * Detects model type from json content string.
     *
     * @param jsonContent json content of a page
     * @return type of the model
     */
    public static PageRequestDto.DataModelType getModelType(String jsonContent) {
        // todo implement normally
        return jsonContent.contains("UrlPath") ? PageRequestDto.DataModelType.R2 : PageRequestDto.DataModelType.DD4T;
    }

    @Override
    @NotNull
    @Cacheable(value = "pageModels", key = "{ #root.methodName, #pageRequest }")
    public Page loadLegacyPageModel(PageRequestDto pageRequest) throws ContentProviderException {
        String pageContent = contentService.loadPageContent(pageRequest);
        log.trace("Loaded page content for {}", pageRequest);

        PageRequestDto.DataModelType publishedModelType = getModelType(pageContent);
        if (publishedModelType == PageRequestDto.DataModelType.R2) {
            log.info("Found R2 model while requested DD4T, need to process R2 and convert, request {}", pageRequest);
            PageModelData page = _processR2PageModel(pageContent, pageRequest);
            return toDd4tConverter.convertToDd4t(page, pageRequest);
        }

        return _processDd4tPageModel(pageContent, pageRequest);
    }

    @Override
    @NotNull
    @Cacheable(value = "pageModels", key = "{ #root.methodName, #pageRequest }")
    public PageModelData loadPageModel(PageRequestDto pageRequest) throws ContentProviderException {
        String pageContent = contentService.loadPageContent(pageRequest);
        log.trace("Loaded page content for {}", pageRequest);

        PageRequestDto.DataModelType publishedModelType = getModelType(pageContent);
        if (publishedModelType == PageRequestDto.DataModelType.DD4T) {
            log.info("Found DD4T model while requested R2, need to convert, no expansion needed, request {}", pageRequest);
            Page page = _processDd4tPageModel(pageContent, pageRequest);
            return toR2Converter.convertToR2(page, pageRequest);
        }

        return _processR2PageModel(pageContent, pageRequest);
    }

    @Contract("!null, _ -> !null")
    private Page _processDd4tPageModel(String pageContent, PageRequestDto pageRequest) throws ContentProviderException {
        try {
            Page page = DataBindFactory.buildPage(pageContent, PageImpl.class);
            log.trace("Parsed page content to page model {}", page);
            return page;
        } catch (SerializationException e) {
            throw new ContentProviderException("Couldn't deserialize DD4T content for request " + pageRequest, e);
        }
    }

    @Contract("!null, _ -> !null")
    private PageModelData _processR2PageModel(String pageContent, PageRequestDto pageRequest) throws ContentProviderException {
        PageModelData pageModel = _parseR2Content(pageContent, PageModelData.class);
        log.trace("Parsed page content to page model {}", pageModel);

        log.trace("processing page model {} for page request {}", pageModel, pageRequest);

        PageModelData pageModelData = _expandIncludePages(pageModel, pageRequest);
        log.trace("expanded include pages for {}", pageRequest);

        // let's check every leaf here if we need to expand it
        _getModelExpander(pageRequest).expandPage(pageModelData);
        log.trace("expanded the whole model for {}", pageRequest);

        return pageModelData;
    }

    @NotNull
    private PageModelExpander _getModelExpander(PageRequestDto pageRequestDto) {
        return new PageModelExpander(pageRequestDto, this, richTextLinkResolver, linkResolver, configService);
    }

    @Contract("!null, _ -> !null")
    private PageModelData _expandIncludePages(PageModelData pageModel, PageRequestDto pageRequest) throws ContentProviderException {
        if (pageModel.getRegions() != null) {
            Iterator<RegionModelData> iterator = pageModel.getRegions().iterator();
            while (iterator.hasNext()) {
                RegionModelData region = iterator.next();
                if (region.getIncludePageId() == null) {
                    continue;
                }

                log.trace("Found include region include id = {}, we {} this page", region.getIncludePageId(), pageRequest.getIncludePages());

                switch (pageRequest.getIncludePages()) {
                    case EXCLUDE:
                        iterator.remove();
                        break;
                    case INCLUDE:
                    default:
                        String includePageContent = contentService.loadPageContent(pageRequest.getPublicationId(), Integer.parseInt(region.getIncludePageId()));
                        // maybe it has inner regions which we need to include?
                        PageModelData includePage = _expandIncludePages(_parseR2Content(includePageContent, PageModelData.class), pageRequest);

                        if (includePage.getRegions() != null) {
                            includePage.getRegions().forEach(region::addRegion);
                        }
                }
            }
        }
        return pageModel;
    }

    @Override
    @NotNull
    @Cacheable(value = "entityModels", key = "{ #root.methodName, #entityRequest }")
    public EntityModelData loadEntity(EntityRequestDto entityRequest) throws ContentProviderException {
        int publicationId = entityRequest.getPublicationId();
        int componentId = entityRequest.getComponentId();
        int templateId = entityRequest.getTemplateId() <= 0 ?
                configService.getDefaults().getDynamicTemplateId(publicationId) : entityRequest.getTemplateId();

        String componentUri = TcmUtils.buildTcmUri(publicationId, componentId);
        String templateUri = TcmUtils.buildTemplateTcmUri(publicationId, templateId);

        ComponentPresentationFactory componentPresentationFactory = new ComponentPresentationFactory(componentUri);
        ComponentPresentation componentPresentation = componentPresentationFactory.getComponentPresentation(componentUri, templateUri);

        if (componentPresentation == null) {
            throw new DxaItemNotFoundException("Cannot find a CP for componentUri" + componentUri + ", templateUri" + templateUri);
        }

        EntityModelData modelData = _parseR2Content(componentPresentation.getContent(), EntityModelData.class);
        if (entityRequest.isResolveLink()) {
            modelData.setLinkUrl(linkResolver.resolveLink(componentUri, String.valueOf(publicationId)));
        }

        return modelData;
    }

    private <T extends ViewModelData> T _parseR2Content(String content, Class<T> expectedClass) throws ContentProviderException {
        try {
            return objectMapper.readValue(content, expectedClass);
        } catch (IOException e) {
            throw new ContentProviderException("Couldn't deserialize content '" + content + "' for " + expectedClass, e);
        }
    }
}
