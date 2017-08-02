package com.sdl.dxa.modelservice.service;

import com.sdl.dxa.common.dto.PageRequestDto;
import com.sdl.webapp.common.api.content.ContentProviderException;
import org.dd4t.contentmodel.Page;
import org.jetbrains.annotations.NotNull;

/**
 * Artificial interface to be able to inject {@code Model Service} since it is a JDK dynamic proxy and can only be injected referenced as an interface.
 */
public interface ModelService extends PageModelService, EntityModelService {

    /**
     * Loads DD4T page model based on request.
     *
     * @param pageRequest current page request
     * @return DD4T model
     * @throws ContentProviderException if something goes wrong
     */
    @NotNull
    Page loadLegacyPageModel(PageRequestDto pageRequest) throws ContentProviderException;
}
