package com.ingenico.ogone.direct.util;

public class IngenicoUrlUtils {

    private static final String PREFIX_URL = "https://payment";
    private static final String POINT = ".";

    public static String buildFullURL(final String partialRedirectUrl) {
        return String.join(POINT, PREFIX_URL, partialRedirectUrl);
    }

}
