package com.sdl.dxa.modelservice.service;

import com.sdl.dxa.common.dto.PageRequestDto;
import com.sdl.dxa.common.util.PathUtils;
import com.sdl.webapp.common.api.content.ContentProviderException;
import com.sdl.webapp.common.api.content.PageNotFoundException;
import com.sdl.webapp.common.util.TcmUtils;
import com.tridion.broker.StorageException;
import com.tridion.broker.querying.Query;
import com.tridion.broker.querying.criteria.content.PageURLCriteria;
import com.tridion.broker.querying.criteria.content.PublicationCriteria;
import com.tridion.broker.querying.criteria.operators.AndCriteria;
import com.tridion.broker.querying.criteria.operators.OrCriteria;
import com.tridion.broker.querying.filter.LimitFilter;
import com.tridion.broker.querying.sorting.SortParameter;
import com.tridion.content.PageContentFactory;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;

import static com.sdl.dxa.common.util.PathUtils.normalizePathToDefaults;

/**
 * The service to load raw content stored in Broker database completely without or with light processing.
 * See details in Javadoc of a concrete method.
 */
@Slf4j
@Service
public class ContentService {

    /**
     * Loads page content without any processing.
     *
     * @param pageRequest current page request
     * @return raw content of a page based on a request
     * @throws ContentProviderException in case there were issues communicating with CIS services
     */
    @NotNull
    @Cacheable(value = "pageModels", key = "{ #root.methodName, #pageRequest }")
    public String loadPageContent(PageRequestDto pageRequest) throws ContentProviderException {
        int publicationId = pageRequest.getPublicationId();
        String path = pageRequest.getPath();

        log.debug("Trying to request a page with localization id = '{}' and path = '{}'", publicationId, path);

        // cannot call OrCriteria#addCriteria(Criteria) due to SOException, https://jira.sdl.com/browse/CRQ-3850
        OrCriteria urlCriteria = PathUtils.hasExtension(path) ?
                new OrCriteria(new PageURLCriteria(normalizePathToDefaults(path))) :
                new OrCriteria(new PageURLCriteria(normalizePathToDefaults(path)), new PageURLCriteria(normalizePathToDefaults(path + "/")));
        Query query = new Query(new AndCriteria(urlCriteria, new PublicationCriteria(publicationId)));
        query.setResultFilter(new LimitFilter(1));
        query.addSorting(new SortParameter(SortParameter.ITEMS_URL, SortParameter.ASCENDING));

        log.trace("Query {} for {}", query, pageRequest);

        try {
            String[] result = query.executeQuery();
            log.debug("Requested publication '{}', path '{}', result is '{}'", publicationId, path, result);
            if (result.length == 0) {
                log.debug("Page not found for {}", pageRequest);
                throw new PageNotFoundException(publicationId, path);
            }

            return loadPageContent(publicationId, TcmUtils.getItemId(result[0]));
        } catch (StorageException e) {
            ContentProviderException exception = new ContentProviderException("Couldn't communicate to CD broker DB while loading a page " +
                    "with localization ID '" + publicationId + "' and page URL '" + path + "'", e);
            log.warn("Issues communicating with CD", exception);
            throw exception;
        }
    }

    String loadPageContent(int publicationId, int pageId) throws ContentProviderException {
        try {
            log.trace("requesting page content for publication {} page id and {}", publicationId, pageId);
            return new PageContentFactory().getPageContent(publicationId, pageId).getString();
        } catch (IOException e) {
            ContentProviderException exception = new ContentProviderException("Couldn't load a page with localization ID '" + publicationId + "' and page ID '" + pageId + "'", e);
            log.warn("Failed to load page content", exception);
            throw exception;
        }
    }
}
