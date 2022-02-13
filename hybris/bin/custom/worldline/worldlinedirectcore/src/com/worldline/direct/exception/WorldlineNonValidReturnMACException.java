package com.worldline.direct.exception;

public class WorldlineNonValidReturnMACException extends Exception {

    private final static String message = "non-valid returnMAC: ";

    public WorldlineNonValidReturnMACException(String returnMAC) {
        super(message + returnMAC);
    }
}
