package com.worldline.direct.constants;

public interface WorldlineCheckoutConstants {
    String EXTENSIONNAME = "worldlinedirectb2ccheckoutaddon";
    String ADDON_PREFIX = "addon:/" + EXTENSIONNAME + "/";

    interface Views {

        interface Pages {

            interface MultiStepCheckout {
                String worldlinePaymentMethod = ADDON_PREFIX + "pages/checkout/multi/worldlinePaymentMethodPage";
                String worldlineCheckoutSummaryPage = ADDON_PREFIX + "pages/checkout/multi/worldlineCheckoutSummaryPage";
                String worldlineOrderConfirmationPage = ADDON_PREFIX + "pages/checkout/multi/worldlineOrderConfirmationPage";
            }
        }

        interface Fragments {
            interface Checkout {
                String BillingAddressForm = ADDON_PREFIX + "fragments/billingAddressForm";
            }
        }

    }


}
