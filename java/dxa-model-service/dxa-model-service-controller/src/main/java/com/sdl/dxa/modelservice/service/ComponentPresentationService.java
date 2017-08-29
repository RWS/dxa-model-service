package com.sdl.dxa.modelservice.service;

import com.sdl.dxa.common.dto.EntityRequestDto;
import com.sdl.webapp.common.exceptions.DxaItemNotFoundException;
import com.tridion.dcp.ComponentPresentation;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ComponentPresentationService {

    @NotNull
    ComponentPresentation loadComponentPresentation(EntityRequestDto entityRequest) throws DxaItemNotFoundException;
}
