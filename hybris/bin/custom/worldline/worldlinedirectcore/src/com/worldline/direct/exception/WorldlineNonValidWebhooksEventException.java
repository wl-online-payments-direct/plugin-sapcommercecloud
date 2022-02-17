package com.worldline.direct.exception;

public class WorldlineNonValidWebhooksEventException extends RuntimeException {

    public WorldlineNonValidWebhooksEventException(String reason) {
        super(reason);
    }

}
