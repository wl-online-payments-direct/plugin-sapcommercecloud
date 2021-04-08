/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.ingenico.ogone.direct.constants;

/**
 * Global class for all Ingenicoogonedirectb2ccheckoutaddon constants. You can add global constants for your extension into this class.
 */
@SuppressWarnings("deprecation")
public final class Ingenicoogonedirectb2ccheckoutaddonConstants extends GeneratedIngenicoogonedirectb2ccheckoutaddonConstants {
    public static final String EXTENSIONNAME = "ingenicoogonedirectb2ccheckoutaddon";

    private Ingenicoogonedirectb2ccheckoutaddonConstants() {
        //empty to avoid instantiating this constant class
    }

    public static final String ADDON_PREFIX = "addon:/" + EXTENSIONNAME + "/";

    /**
     * Class with view name constants
     */
    public interface Views {

        interface Pages {

            interface MultiStepCheckout {
                String ingenicoPaymentMethod = ADDON_PREFIX + "pages/checkout/multi/ingenicoPaymentMethodPage";
                String ingenicoCheckoutSummaryPage = ADDON_PREFIX + "pages/checkout/multi/ingenicoCheckoutSummaryPage";
                String ingenicoDoPaymentPage = ADDON_PREFIX + "pages/checkout/multi/ingenicoDoPaymentPage";
            }
        }

        interface Fragments {
            interface Checkout {
                String BillingAddressForm = ADDON_PREFIX + "fragments/billingAddressForm";
            }
        }

    }


}
