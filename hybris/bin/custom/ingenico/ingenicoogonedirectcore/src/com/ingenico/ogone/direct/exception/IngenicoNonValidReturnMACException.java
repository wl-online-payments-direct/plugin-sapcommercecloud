package com.ingenico.ogone.direct.exception;

public class IngenicoNonValidReturnMACException extends Exception {

    private final static String message = "non-valid returnMAC: ";

    public IngenicoNonValidReturnMACException(String returnMAC) {
        super(message + returnMAC);
    }
}
