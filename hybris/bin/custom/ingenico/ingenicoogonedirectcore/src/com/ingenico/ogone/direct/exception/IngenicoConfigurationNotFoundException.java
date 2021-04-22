package com.ingenico.ogone.direct.exception;

public class IngenicoConfigurationNotFoundException extends RuntimeException {
    private final static String message = "Ingenico Configuration not found for [BaseStore] :";

    public IngenicoConfigurationNotFoundException(String baseStore) {
        super(message + baseStore);
    }
}
