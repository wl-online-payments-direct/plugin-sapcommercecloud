package com.worldline.gopay.exception;

public class WorldlineNonValidWebhooksEventException extends RuntimeException {

    public WorldlineNonValidWebhooksEventException(String reason) {
        super(reason);
    }

}
