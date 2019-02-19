package com.sdl.dxa.modelservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdl.dxa.api.datamodel.model.PageModelData;
import com.sdl.dxa.api.datamodel.model.RegionModelData;
import com.sdl.dxa.api.datamodel.model.ViewModelData;
import com.sdl.dxa.common.dto.DataModelType;
import com.sdl.dxa.common.dto.PageRequestDto;
import com.sdl.dxa.modelservice.service.processing.conversion.ToDd4tConverter;
import com.sdl.dxa.modelservice.service.processing.conversion.ToR2Converter;
import com.sdl.dxa.modelservice.service.processing.expansion.PageModelExpander;
import com.sdl.dxa.tridion.linking.api.BatchLinkResolver;
import com.sdl.dxa.tridion.linking.RichTextLinkResolver;
import com.sdl.webapp.common.api.content.ContentProviderException;
import com.sdl.webapp.common.api.content.LinkResolver;
import com.tridion.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.dd4t.contentmodel.Page;
import org.dd4t.contentmodel.impl.PageImpl;
import org.dd4t.core.databind.DataBinder;
import org.dd4t.core.exceptions.ProcessorException;
import org.dd4t.core.exceptions.SerializationException;
import org.dd4t.core.processors.impl.RichTextResolver;
import org.dd4t.core.util.HttpRequestContext;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Iterator;

import static com.sdl.dxa.modelservice.service.ContentService.getModelType;

/**
 * Service capable to load content and construct {@code models} out of it.
 *
 * Note: for in process the LinkResolver is overridden to BrokerTridionLinkResolver.
 */
@Slf4j
@Service
public class DefaultPageModelService implements PageModelService, LegacyPageModelService {

    private final ObjectMapper objectMapper;

    private final LinkResolver linkResolver;

    private final ConfigService configService;

    private final ContentService contentService;

    private final ToDd4tConverter toDd4tConverter;

    private final ToR2Converter toR2Converter;

    private final RichTextLinkResolver richTextLinkResolver;

    private final DataBinder dd4tDataBinder;

    private final RichTextResolver dd4tRichTextResolver;

    private final EntityModelService entityModelService;

    @Autowired
    public DefaultPageModelService(@Qualifier("dxaR2ObjectMapper") ObjectMapper objectMapper,
                                   @Qualifier("dxaLinkResolver") LinkResolver linkResolver,
                                   ConfigService configService,
                                   EntityModelService entityModelService,
                                   ContentService contentService,
                                   ToDd4tConverter toDd4tConverter,
                                   ToR2Converter toR2Converter,
                                   RichTextLinkResolver richTextLinkResolver,
                                   DataBinder dd4tDataBinder,
                                   RichTextResolver dd4tRichTextResolver) {
        this.objectMapper = objectMapper;
        this.linkResolver = linkResolver;
        this.configService = configService;
        this.entityModelService = entityModelService;
        this.contentService = contentService;
        this.toDd4tConverter = toDd4tConverter;
        this.toR2Converter = toR2Converter;
        this.richTextLinkResolver = richTextLinkResolver;
        this.dd4tDataBinder = dd4tDataBinder;
        this.dd4tRichTextResolver = dd4tRichTextResolver;
    }

    @Override
    @NotNull
    @Cacheable(value = "pageModels", key = "{ #root.methodName, #pageRequest }")
    public Page loadLegacyPageModel(PageRequestDto pageRequest) throws ContentProviderException {
        String pageContent = contentService.loadPageContent(pageRequest);
        log.trace("Loaded page content for {}", pageRequest);
        return _processDd4tPageModel(pageContent, pageRequest);
    }

    @Override
    @NotNull
    @Cacheable(value = "pageModels", key = "{ #root.methodName, #pageRequest }")
    public PageModelData loadPageModel(PageRequestDto pageRequest) throws ContentProviderException {

        String pageContent = contentService.loadPageContent(pageRequest);
        log.trace("Loaded page content for {}", pageRequest);

        return _processR2PageModel(pageContent, pageRequest);
    }

    @Contract("!null, _ -> !null")
    private Page _processDd4tPageModel(String pageContent, PageRequestDto pageRequest) throws ContentProviderException {
        Page page;
        DataModelType publishedModelType = getModelType(pageContent);
        if (publishedModelType == DataModelType.R2) {
            log.info("Found R2 model while requested DD4T, need to process R2 and convert, request {}", pageRequest);
            PageModelData r2page = _processR2PageModel(pageContent, pageRequest);
            page = toDd4tConverter.convertToDd4t(r2page, pageRequest);
        } else {
            try {
                page = dd4tDataBinder.buildPage(pageContent, PageImpl.class);

                // we only resolve links using DD4T if we request content also in DD4T, otherwise our resolver is used
                if (pageRequest.getDataModelType() == DataModelType.DD4T) {
                    dd4tRichTextResolver.execute(page, new HttpRequestContext());
                }

                log.trace("Parsed page content to page model {}", page);
                return page;
            } catch (SerializationException e) {
                throw new ContentProviderException("Couldn't deserialize DD4T content for request " + pageRequest, e);
            } catch (ProcessorException e) {
                throw new ContentProviderException("Couldn't process DD4T content for request " + pageRequest, e);
            }
        }
        return page;
    }

    @Contract("!null, _ -> !null")
    private PageModelData _processR2PageModel(String pageContent, PageRequestDto pageRequest) throws ContentProviderException {

        DataModelType publishedModelType = getModelType(pageContent);
        PageModelData pageModel;
        if (publishedModelType == DataModelType.DD4T) {
            log.info("Found DD4T model while requested R2, need to convert, no expansion needed, request {}", pageRequest);
            Page page = _processDd4tPageModel(pageContent, pageRequest);
            pageModel = toR2Converter.convertToR2(page, pageRequest);
        } else {
            pageModel = _parseR2Content(pageContent, PageModelData.class);
        }

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
        return new PageModelExpander(pageRequestDto, entityModelService, richTextLinkResolver, linkResolver, configService, getBatchLinkResolver());
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
                        try {
                            String includePageContent = contentService.loadPageContent(pageRequest.getPublicationId(), Integer.parseInt(region.getIncludePageId()));
                            if (StringUtils.isNotEmpty(includePageContent)) {
                                // maybe it has inner regions which we need to include?
                                PageModelData includePage = _expandIncludePages(_processR2PageModel(includePageContent, pageRequest), pageRequest);

                                if (includePage.getRegions() != null) {
                                    includePage.getRegions().forEach(region::addRegion);
                                }
                            }
                        } catch (ContentProviderException e){
                            log.error("Include Page '{0}' not found.", region.getIncludePageId());
                        }
                }
            }
        }
        return pageModel;
    }

    private <T extends ViewModelData> T _parseR2Content(String content, Class<T> expectedClass) throws ContentProviderException {
        try {
            return objectMapper.readValue(content, expectedClass);
        } catch (IOException e) {
            throw new ContentProviderException("Couldn't deserialize content '" + content + "' for " + expectedClass, e);
        }
    }

    @Lookup
    public BatchLinkResolver getBatchLinkResolver() {
        return null;
    }
}
