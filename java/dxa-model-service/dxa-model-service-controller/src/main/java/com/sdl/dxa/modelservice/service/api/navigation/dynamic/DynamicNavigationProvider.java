package com.sdl.dxa.modelservice.service.api.navigation.dynamic;

import com.sdl.dxa.api.datamodel.model.SitemapItemModelData;
import com.sdl.dxa.api.datamodel.model.TaxonomyNodeModelData;
import com.sdl.dxa.common.dto.DepthCounter;
import com.sdl.dxa.common.dto.SitemapRequestDto;
import com.sdl.dxa.common.util.PathUtils;
import com.sdl.web.api.dynamic.taxonomies.WebTaxonomyFactory;
import com.sdl.webapp.common.api.navigation.NavigationFilter;
import com.sdl.webapp.common.util.TcmUtils;
import com.tridion.broker.StorageException;
import com.tridion.meta.PageMeta;
import com.tridion.meta.PageMetaFactory;
import com.tridion.taxonomies.Keyword;
import com.tridion.taxonomies.filters.DepthFilter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.sdl.dxa.common.util.PathUtils.isIndexPath;
import static com.sdl.dxa.common.util.PathUtils.isWithSequenceDigits;
import static com.sdl.dxa.common.util.PathUtils.removeSequenceFromPageTitle;
import static com.sdl.dxa.common.util.PathUtils.stripDefaultExtension;
import static com.sdl.webapp.common.util.TcmUtils.Taxonomies.SitemapItemType.KEYWORD;
import static com.sdl.webapp.common.util.TcmUtils.Taxonomies.SitemapItemType.PAGE;
import static com.sdl.webapp.common.util.TcmUtils.Taxonomies.getTaxonomySitemapIdentifier;

@Slf4j
@Service
public class DynamicNavigationProvider {

    @Value("${dxa.tridion.navigation.taxonomy.marker}")
    protected String taxonomyNavigationMarker;

    @Value("${dxa.tridion.navigation.taxonomy.type.taxonomyNode}")
    protected String sitemapItemTypeTaxonomyNode;

    @Value("${dxa.tridion.navigation.taxonomy.type.structureGroup}")
    protected String sitemapItemTypeStructureGroup;

    @Value("${dxa.tridion.navigation.taxonomy.type.page}")
    protected String sitemapItemTypePage;

    @Autowired
    private WebTaxonomyFactory taxonomyFactory;

    public Optional<SitemapItemModelData> getNavigationModel(@NotNull SitemapRequestDto requestDto) {
        Optional<String> rootTaxonomy = getRootNavigationTaxonomyId(requestDto.getLocalizationId());
        if (!rootTaxonomy.isPresent()) {
            log.error("No Navigation Taxonomy Found in Localization [{}]. Ensure a Taxonomy with '{}}' in its title is published",
                    requestDto.getLocalizationId(), taxonomyNavigationMarker);
            return Optional.empty();
        }

        log.debug("Resolved Navigation Taxonomy {} for request {}", rootTaxonomy, requestDto);

        Keyword taxonomy = taxonomyFactory.getTaxonomyKeywords(rootTaxonomy.get(),
                new DepthFilter(DepthFilter.UNLIMITED_DEPTH, DepthFilter.FILTER_DOWN));

        return Optional.of(
                createTaxonomyNode(taxonomy,
                        requestDto.toBuilder()
                                .navigationFilter(NavigationFilter.DEFAULT)
                                .expandLevels(DepthCounter.UNLIMITED_DEPTH)
                                .build()));
    }

    private Optional<String> getRootNavigationTaxonomyId(int publicationId) {
        String[] taxonomies = taxonomyFactory.getTaxonomies(TcmUtils.buildPublicationTcmUri(publicationId));

        return Stream.of(taxonomies)
                .filter(taxonomy -> taxonomyFactory.getTaxonomyKeyword(taxonomy).getKeywordName().contains(taxonomyNavigationMarker))
                .findFirst();
    }

    private TaxonomyNodeModelData createTaxonomyNode(@NotNull Keyword keyword, @NotNull SitemapRequestDto requestDto) {
        log.debug("Creating taxonomy node for keyword {} and request {}", keyword.getTaxonomyURI(), requestDto);
        //todo replace with TcmUtils?
        String taxonomyId = keyword.getTaxonomyURI().split("-")[1];

        String taxonomyNodeUrl = null;

        List<SitemapItemModelData> children = new ArrayList<>();

        if (requestDto.getExpandLevels().isNotTooDeep()) {
            keyword.getKeywordChildren().forEach(child -> children.add(createTaxonomyNode(child, requestDto.nextExpandLevel())));

            if (keyword.getReferencedContentCount() > 0 && requestDto.getNavigationFilter().getDescendantLevels() != 0) {
                List<SitemapItemModelData> pageSitemapItems = getChildrenPages(keyword, taxonomyId, requestDto);

                taxonomyNodeUrl = findIndexPageUrl(pageSitemapItems).orElse(null);
                log.trace("taxonomyNodeUrl = {}", taxonomyNodeUrl);

                children.addAll(pageSitemapItems);
            }
        }

        children.forEach(child -> child.setTitle(removeSequenceFromPageTitle(child.getTitle())));

        return createTaxonomyNodeFromKeyword(keyword, taxonomyId, taxonomyNodeUrl, new LinkedHashSet<>(children));
    }

    private List<SitemapItemModelData> getChildrenPages(@NotNull Keyword keyword, @NotNull String taxonomyId, @NotNull SitemapRequestDto requestDto) {
        log.trace("Getting SitemapItems for all classified Pages (ordered by Page Title, including sequence prefix if any), " +
                "keyword {}, taxonomyId {}, localization {}", keyword, taxonomyId, requestDto.getLocalizationId());

        List<SitemapItemModelData> items = new ArrayList<>();

        try {
            PageMetaFactory pageMetaFactory = new PageMetaFactory(requestDto.getLocalizationId());
            PageMeta[] taxonomyPages = pageMetaFactory.getTaxonomyPages(keyword, false);
            items = Arrays.stream(taxonomyPages)
                    .map(page -> createSitemapItemFromPage(page, taxonomyId))
                    .collect(Collectors.toList());
        } catch (StorageException e) {
            log.error("Error loading taxonomy pages for taxonomyId = {}, localizationId = {} and keyword {}", taxonomyId, requestDto.getLocalizationId(), keyword, e);
        }

        return items;
    }

    private Optional<String> findIndexPageUrl(@NonNull List<SitemapItemModelData> pageSitemapItems) {
        return pageSitemapItems.stream()
                .filter(input -> isIndexPath(input.getUrl()))
                .findFirst()
                .map(SitemapItemModelData::getUrl)
                .map(PathUtils::stripIndexPath);
    }

    private SitemapItemModelData createSitemapItemFromPage(PageMeta page, String taxonomyId) {
        return new SitemapItemModelData()
                .setId(getTaxonomySitemapIdentifier(taxonomyId, PAGE, String.valueOf(page.getId())))
                .setType(sitemapItemTypePage)
                .setTitle(page.getTitle())
                .setUrl(stripDefaultExtension(page.getURLPath()))
                .setVisible(isVisibleItem(page.getTitle(), page.getURLPath()))
                .setPublishedDate(new DateTime(page.getLastPublicationDate()));
    }

    private TaxonomyNodeModelData createTaxonomyNodeFromKeyword(@NotNull Keyword keyword, String taxonomyId, String taxonomyNodeUrl, Set<SitemapItemModelData> children) {
        boolean isRoot = Objects.equals(keyword.getTaxonomyURI(), keyword.getKeywordURI());
        //todo check if cannot use TcmUtils
        String keywordId = keyword.getKeywordURI().split("-")[1];

        return (TaxonomyNodeModelData) new TaxonomyNodeModelData()
                .setWithChildren(keyword.hasKeywordChildren() || keyword.getReferencedContentCount() > 0)
                .setDescription(keyword.getKeywordDescription())
                .setTaxonomyAbstract(keyword.isKeywordAbstract())
                .setClassifiedItemsCount(keyword.getReferencedContentCount())
                .setKey(keyword.getKeywordKey())
                .setId(isRoot ? getTaxonomySitemapIdentifier(taxonomyId) : getTaxonomySitemapIdentifier(taxonomyId, KEYWORD, keywordId))
                .setType(sitemapItemTypeTaxonomyNode)
                .setUrl(stripDefaultExtension(taxonomyNodeUrl))
                .setTitle(keyword.getKeywordName())
                .setVisible(isVisibleItem(keyword.getKeywordName(), taxonomyNodeUrl))
                .setItems(children);
    }

    private boolean isVisibleItem(String pageName, String pageUrl) {
        return isWithSequenceDigits(pageName) && !isNullOrEmpty(pageUrl);
    }

}
