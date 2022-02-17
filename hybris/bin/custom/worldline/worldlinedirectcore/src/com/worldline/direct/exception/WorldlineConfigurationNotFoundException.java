package com.worldline.direct.exception;

public class WorldlineConfigurationNotFoundException extends RuntimeException {
    private final static String message = "Worldline Configuration not found for [BaseStore] :";

    public WorldlineConfigurationNotFoundException(String baseStore) {
        super(message + baseStore);
    }
}
