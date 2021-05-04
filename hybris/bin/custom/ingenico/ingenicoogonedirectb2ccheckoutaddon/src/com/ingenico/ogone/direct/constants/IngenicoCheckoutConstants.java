package com.ingenico.ogone.direct.constants;

public interface IngenicoCheckoutConstants {
    String EXTENSIONNAME = "ingenicoogonedirectb2ccheckoutaddon";
    String ADDON_PREFIX = "addon:/" + EXTENSIONNAME + "/";

    interface Views {

        interface Pages {

            interface MultiStepCheckout {
                String ingenicoPaymentMethod = ADDON_PREFIX + "pages/checkout/multi/ingenicoPaymentMethodPage";
                String ingenicoCheckoutSummaryPage = ADDON_PREFIX + "pages/checkout/multi/ingenicoCheckoutSummaryPage";
                String ingenicoDoPaymentPage = ADDON_PREFIX + "pages/checkout/multi/ingenicoDoPaymentPage";
                String ingenicoOrderConfirmationPage = ADDON_PREFIX + "pages/checkout/multi/ingenicoOrderConfirmationPage";
            }
        }

        interface Fragments {
            interface Checkout {
                String BillingAddressForm = ADDON_PREFIX + "fragments/billingAddressForm";
            }
        }

    }


}
