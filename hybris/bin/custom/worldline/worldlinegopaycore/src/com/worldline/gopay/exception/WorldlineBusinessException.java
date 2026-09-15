package com.worldline.gopay.exception;

public class WorldlineBusinessException extends Exception {
    public WorldlineBusinessException(final String message, final Throwable throwable) {
        super(message, throwable);
    }
}
