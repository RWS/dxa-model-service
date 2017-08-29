package com.sdl.dxa.modelservice.service.processing.conversion;

import com.sdl.dxa.api.datamodel.model.EntityModelData;
import com.sdl.dxa.api.datamodel.model.PageModelData;
import com.sdl.dxa.common.dto.EntityRequestDto;
import com.sdl.dxa.common.dto.PageRequestDto;
import com.sdl.webapp.common.api.content.ContentProviderException;
import org.dd4t.contentmodel.ComponentPresentation;
import org.dd4t.contentmodel.Page;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ToDd4tConverter {

    /**
     * Converts the given R2 data model to DD4T data model.
     *
     * @param toConvert   R2 page model to convert
     * @param pageRequest current page request
     * @return equal DD4T model, {@code null} in case parameter is {@code null}
     */
    @Contract("!null, _ -> !null; null, _ -> null")
    Page convertToDd4t(@Nullable PageModelData toConvert, @NotNull PageRequestDto pageRequest) throws ContentProviderException;

    @Contract("!null, _ -> !null; null, _ -> null")
    ComponentPresentation convertToDd4t(@Nullable EntityModelData toConvert, @NotNull EntityRequestDto entityRequest) throws ContentProviderException;
}
