package com.sdl.dxa.tridion.compatibility;

import com.sdl.dxa.common.dto.PageRequestDto;
import com.tridion.broker.StorageException;
import com.tridion.broker.querying.criteria.Criteria;

/**
 * TridionQueryLoader.
 */
public interface TridionQueryLoader {

    /**
     * Constructs and executes the required Query object just to get single page content
     * wrapping the actual implementation, of which the method signatures differ between
     * in-proc and CIL.
     *
     * The compiler basically casts the setResultFilter to either (BrokerResultFilter)
     * or (ResultFilter). The same is true for the SortParameters, so
     * we need to have two implementations which will cast differently for
     * each module.
     *
     * @param criteria the page criteria
     * @return a Query object.
     */

    String[] constructQueryAndSetResultFilter(Criteria criteria, PageRequestDto pageRequest) throws StorageException;
}
