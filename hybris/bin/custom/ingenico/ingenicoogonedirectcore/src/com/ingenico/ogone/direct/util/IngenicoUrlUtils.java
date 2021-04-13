package com.ingenico.ogone.direct.util;

public class IngenicoUrlUtils {

    private static final String PREFIX_URL = "https://payment";
    private static final String POINT = ".";
    private static final String HOSTED_CHECKOUT_RETURN_URL = "checkout/multi/ingenico/hosted-checkout/response";
    private static final String SLASH = "/";

    public static String buildFullURL(final String partialRedirectUrl) {
        return String.join(POINT, PREFIX_URL, partialRedirectUrl);
    }

    public static String buildHostedCheckoutReturnUrl(final String domain) {
        return String.join(SLASH, domain, HOSTED_CHECKOUT_RETURN_URL);
    }

}
