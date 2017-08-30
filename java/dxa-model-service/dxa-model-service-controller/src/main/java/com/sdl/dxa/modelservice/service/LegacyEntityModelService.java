package com.sdl.dxa.modelservice.service;

import com.sdl.dxa.common.dto.EntityRequestDto;
import com.sdl.webapp.common.api.content.ContentProviderException;
import org.dd4t.contentmodel.ComponentPresentation;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface LegacyEntityModelService {

    /**
     * Loads DD4T component presentation model based on request.
     *
     * @param entityRequest current entity request
     * @return DD4T model
     * @throws ContentProviderException if something goes wrong
     */
    @NotNull
    ComponentPresentation loadLegacyEntityModel(EntityRequestDto entityRequest) throws ContentProviderException;
}
