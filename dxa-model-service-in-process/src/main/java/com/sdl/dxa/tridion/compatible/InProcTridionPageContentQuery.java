package com.sdl.dxa.tridion.compatible;

import com.sdl.dxa.common.dto.PageRequestDto;
import com.sdl.dxa.tridion.compatibility.TridionQueryLoader;
import com.tridion.broker.StorageException;
import com.tridion.broker.querying.Query;
import com.tridion.broker.querying.criteria.Criteria;
import com.tridion.broker.querying.filter.LimitFilter;
import com.tridion.broker.querying.sorting.SortParameter;
import org.springframework.stereotype.Component;

/**
 * InProcTridionPageContentQuery.
 *
 * Needed for compatibility and separation between CIL and the in process API.
 */
@Component
public class InProcTridionPageContentQuery implements TridionQueryLoader {

    @Override
    public String[] constructQueryAndSetResultFilter(final Criteria criteria, final PageRequestDto pageRequest) throws StorageException {
        final Query query = new Query(criteria);
        query.setResultFilter(new LimitFilter(1));
        query.addSorting(new SortParameter(SortParameter.ITEMS_URL, SortParameter.ASCENDING));
        return query.executeQuery();
    }
}
