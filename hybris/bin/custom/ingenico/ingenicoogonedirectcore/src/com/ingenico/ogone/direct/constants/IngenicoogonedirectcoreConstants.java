package com.ingenico.ogone.direct.constants;

/**
 * Global class for all Ingenicoogonedirectcore constants. You can add global constants for your extension into this class.
 */
public final class IngenicoogonedirectcoreConstants extends GeneratedIngenicoogonedirectcoreConstants {
    public static final String EXTENSIONNAME = "ingenicoogonedirectcore";

    // Payment methods

    public static final int PAYMENT_METHOD_IDEAL = 809;
    public static final int PAYMENT_METHOD_PAYPAL = 840;
    public static final int PAYMENT_METHOD_ILLICADO = 3112;
    public static final int PAYMENT_METHOD_BCC = 3012;


    public static final String PAYMENT_METHOD_IDEAL_COUNTRY = "NL";


    public enum PAYMENT_METHOD_TYPE {
        CARD("card"),
        REDIRECT("redirect"),
        MOBILE("mobile");

        private final String value;

        PAYMENT_METHOD_TYPE(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum UNAUTHORIZED_REASON {NEED_3DS, IN_PROGRESS, CANCELLED, REJECTED}


    public enum HOSTED_CHECKOUT_STATUS_ENUM {
        CANCELLED_BY_CONSUMER("CANCELLED_BY_CONSUMER"),
        CLIENT_NOT_ELIGIBLE_FOR_SELECTED_PAYMENT_PRODUCT("CLIENT_NOT_ELIGIBLE_FOR_SELECTED_PAYMENT_PRODUCT"),
        IN_PROGRESS("IN_PROGRESS"),
        PAYMENT_CREATED("PAYMENT_CREATED");
        private final String value;

        HOSTED_CHECKOUT_STATUS_ENUM(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum ORDER_AMOUNT_BREAKDOWN_TYPES_ENUM {
        DISCOUNT("DISCOUNT"),
        SHIPPING("SHIPPING"),
        VAT("VAT"),
        BASE_AMOUNT("BASE_AMOUNT");

        private final String value;

        ORDER_AMOUNT_BREAKDOWN_TYPES_ENUM(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum PAYMENT_STATUS_ENUM {
        CREATED("PAYMENT_CREATED"),
        CANCELLED("CANCELLED"),
        REJECTED("REJECTED"),
        REJECTED_CAPTURE("REJECTED_CAPTURE"),
        REDIRECTED("REDIRECTED"),
        PENDING_PAYMENT("PENDING_PAYMENT"),
        PENDING_COMPLETION("PENDING_COMPLETION"),
        PENDING_CAPTURE("PENDING_CAPTURE"),
        AUTHORIZATION_REQUESTED("AUTHORIZATION_REQUESTED"),
        CAPTURE_REQUESTED("CAPTURE_REQUESTED"),
        CAPTURED("CAPTURED"),
        REVERSED("REVERSED"),
        REFUND_REQUESTED("REFUND_REQUESTED"),
        REFUNDED("REFUNDED");

        private final String value;

        PAYMENT_STATUS_ENUM(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum PAYMENT_STATUS_CATEGORY_ENUM {
        SUCCESSFUL("SUCCESSFUL"),
        REJECTED("REJECTED"),
        STATUS_UNKNOWN("STATUS_UNKNOWN");

        private final String value;

        PAYMENT_STATUS_CATEGORY_ENUM(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private IngenicoogonedirectcoreConstants() {
        //empty to avoid instantiating this constant class
    }


}
