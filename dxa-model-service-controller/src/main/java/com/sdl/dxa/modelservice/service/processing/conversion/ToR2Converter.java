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

/**
 * Converts DD4T content to R2 content.
 * <p><strong>Notice that implementors cannot use DXA-specific content</strong> (e.g. {@code schemas.json} to load any data
 * because pure DD4T applications will not have it.</p>
 */
public interface ToR2Converter {

    /**
     * Converts the given DD4T page data model to R2 page data model.
     *
     * @param toConvert      DD4T page model to convert
     * @param pageRequestDto current page request
     * @return equal R2 model, {@code null} in case parameter is {@code null}
     */
    @Contract("!null, _ -> !null; null, _ -> null")
    PageModelData convertToR2(@Nullable Page toConvert, @NotNull PageRequestDto pageRequestDto) throws ContentProviderException;

    /**
     * Converts the given DD4T CP data model to R2 entity data model.
     *
     * @param toConvert        DD4T entity model to convert
     * @param entityRequestDto current entity request
     * @return equal R2 model, {@code null} in case parameter is {@code null}
     */
    @Contract("!null, _ -> !null; null, _ -> null")
    EntityModelData convertToR2(@Nullable ComponentPresentation toConvert, @NotNull EntityRequestDto entityRequestDto) throws ContentProviderException;
}
