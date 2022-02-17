package com.worldline.direct.exception;

public class WorldlineNonValidPaymentProductException extends Exception {

    private final static String message = "non-valid payment product : ";
    public WorldlineNonValidPaymentProductException(int paymentId) {
        super(message + paymentId);
    }
}
