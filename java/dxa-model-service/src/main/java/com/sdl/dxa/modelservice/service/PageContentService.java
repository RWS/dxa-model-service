package com.sdl.dxa.modelservice.service;

import com.sdl.dxa.api.datamodel.model.PageModelData;
import com.sdl.dxa.common.dto.PageRequestDto;
import com.sdl.webapp.common.api.content.ContentProviderException;
import com.sdl.webapp.common.api.content.PageNotFoundException;
import org.jetbrains.annotations.NotNull;

public interface PageContentService {

    /**
     * Loads a page from CD and converts it to the {@link PageModelData}.
     * See {@link #loadPageContent(PageRequestDto)} for details.
     */
    @NotNull
    PageModelData loadPageModel(PageRequestDto pageRequest) throws ContentProviderException;

    /**
     * Loads a page from CD.
     *
     * @param pageRequest page request data
     * @return a page model data, never null
     * @throws PageNotFoundException    if the page doesn't exist
     * @throws ContentProviderException if couldn't load or parse the page content
     */
    @NotNull
    String loadPageContent(PageRequestDto pageRequest) throws ContentProviderException;
}
