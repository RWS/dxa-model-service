package com.sdl.dxa.modelservice.service;

/**
 * Exception may happen during data model expansion.
 */
public class DataModelExpansionException extends RuntimeException {

    public DataModelExpansionException() {
    }

    public DataModelExpansionException(String message) {
        super(message);
    }

    public DataModelExpansionException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataModelExpansionException(Throwable cause) {
        super(cause);
    }

    public DataModelExpansionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
