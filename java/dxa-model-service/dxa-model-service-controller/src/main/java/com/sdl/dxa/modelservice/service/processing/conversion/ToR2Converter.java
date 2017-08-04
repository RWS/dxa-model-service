package com.sdl.dxa.modelservice.service.processing.conversion;

import com.sdl.dxa.api.datamodel.model.PageModelData;
import com.sdl.dxa.common.dto.PageRequestDto;
import org.dd4t.contentmodel.Page;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ToR2Converter {

    /**
     * Converts the given DD4T data model to R2 data model.
     *
     * @param toConvert      DD4T page model to convert
     * @param pageRequestDto current page request
     * @return equal R2 model, {@code null} in case parameter is {@code null}
     */
    @Contract("!null, _ -> !null; null, _ -> null")
    PageModelData convertToR2(@Nullable Page toConvert, @NotNull PageRequestDto pageRequestDto);
}
