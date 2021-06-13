package com.ingenico.ogone.direct.exception;

public class IngenicoNonValidWebhooksEventException extends RuntimeException {

    public IngenicoNonValidWebhooksEventException(String reason) {
        super(reason);
    }

}
