package com.sdl.dxa.tridion.compatible;

import com.sdl.dxa.common.dto.PageRequestDto;
import com.sdl.dxa.tridion.compatibility.TridionQueryLoader;
import com.sdl.web.api.broker.querying.QueryImpl;
import com.sdl.web.api.broker.querying.criteria.BrokerCriteria;
import com.sdl.web.api.broker.querying.filter.LimitFilter;
import com.sdl.web.api.broker.querying.sorting.SortParameter;
import com.tridion.broker.StorageException;
import com.tridion.broker.querying.criteria.Criteria;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * PageContentQueryLoader.
 *
 * Needed for compatibility and separation between CIL and the in process API.
 */
@Component
@Slf4j
public class CilTridionPageContentQuery implements TridionQueryLoader {

    @Override
    public String[] constructQueryAndSetResultFilter(final Criteria criteria, final PageRequestDto pageRequest) throws StorageException {

        final QueryImpl query = new QueryImpl((BrokerCriteria) criteria);
        query.setResultFilter(new LimitFilter(1));
        query.addSorting(new SortParameter(SortParameter.ITEMS_URL, SortParameter.ASCENDING));
        log.debug("Query {} for {}", query, pageRequest);
        return query.executeQuery();
    }
}
