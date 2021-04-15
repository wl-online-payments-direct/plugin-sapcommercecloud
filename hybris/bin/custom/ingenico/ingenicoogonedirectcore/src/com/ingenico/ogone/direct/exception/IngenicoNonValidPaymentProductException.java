package com.ingenico.ogone.direct.exception;

public class IngenicoNonValidPaymentProductException extends Exception {

    private final static String message = "non-valid payment product : ";
    public IngenicoNonValidPaymentProductException(int paymentId) {
        super(message + paymentId);
    }
}
