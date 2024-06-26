package com.worldline.direct.b2bcheckoutaddon.constants;

public interface WorldlineCheckoutConstants {
    String EXTENSIONNAME = "worldlinedirectb2bcheckoutaddon";
    String ADDON_PREFIX = "addon:/" + EXTENSIONNAME + "/";

    interface Views {

        interface Pages {

            interface MultiStepCheckout {
                String worldlineDeliveryAddressPage = ADDON_PREFIX + "pages/checkout/multi/worldlineDeliveryAddressPage";

                String worldlinePaymentMethod = ADDON_PREFIX + "pages/checkout/multi/worldlinePaymentMethodPage";
                String worldlineCheckoutSummaryPage = ADDON_PREFIX + "pages/checkout/multi/worldlineCheckoutSummaryPage";
                String worldlineOrderConfirmationPage = ADDON_PREFIX + "pages/checkout/multi/worldlineOrderConfirmationPage";
            }

            interface Cart {
                String CartPage = "pages/cart/cartPage";
            }

        }

        interface Fragments {
            interface Checkout {
                String BillingAddressForm = ADDON_PREFIX + "fragments/billingAddressForm";
                String CountryAddressForm = ADDON_PREFIX + "fragments/worldlineCountryAddressForm";
                String TermsAndConditionsPopup = "fragments/checkout/termsAndConditionsPopup";
                String ReadOnlyExpandedOrderForm = "fragments/checkout/readOnlyExpandedOrderForm";

            }
            interface Cart {
                 String ExpandGridInCart = "fragments/cart/expandGridInCart";
            }

        }

    }


}
