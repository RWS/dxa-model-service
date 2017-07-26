package com.sdl.dxa.modelservice.service;

import com.sdl.dxa.api.datamodel.model.PageModelData;
import org.dd4t.contentmodel.Page;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

/**
 * Converter service is capable to convert R2 to DD4T data models both ways.
 */
@Service
public class ConverterService {

    /**
     * Converts the given R2 data model to DD4T data model.
     *
     * @param r2model R2 page model to convert
     * @return equal DD4T model, {@code null} in case parameter is {@code null}
     */
    @Contract("!null -> !null; null -> null")
    public Page convertToDd4t(@Nullable PageModelData r2model) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Converts the given DD4T data model to R2 data model.
     *
     * @param dd4tModel DD4T page model to convert
     * @return equal R2 model, {@code null} in case parameter is {@code null}
     */
    @Contract("!null -> !null; null -> null")
    public PageModelData convertToR2(@Nullable Page dd4tModel) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
