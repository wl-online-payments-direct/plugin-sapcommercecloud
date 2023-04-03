package com.worldline.direct.constants;


/**
 * Global class for all Worldlinedirectcore constants. You can add global constants for your extension into this class.
 */
public final class WorldlinedirectcoreConstants extends GeneratedWorldlinedirectcoreConstants {
    public static final String EXTENSIONNAME = "worldlinedirectcore";

    // Payment methods

    public static final int PAYMENT_METHOD_HTP = -1;
    public static final int PAYMENT_METHOD_HCP = -2;

    public static final int PAYMENT_METHOD_GROUP_CARDS = -3;

    public static final int PAYMENT_METHOD_IDEAL = 809;
    public static final int PAYMENT_METHOD_PAYPAL = 840;
    public static final int PAYMENT_METHOD_APPLEPAY = 302;
    public static final int PAYMENT_METHOD_KLARNA_PAY_AFTER = 3302;
    public static final int PAYMENT_METHOD_KLARNA_PAY_NOW = 3301;
    public static final int PAYMENT_METHOD_SEPA = 771;
    public static final int PAYMENT_METHOD_ILLICADO = 3112;
    public static final int PAYMENT_METHOD_BCC = 3012;

    public static final int PAYMENT_METHOD_INTERSOLVE = 5700;


    public static final String PAYMENT_METHOD_IDEAL_COUNTRY = "NL";

    public static final String PAYMENT_PROVIDER = "WORLDLINE";
    public static final String WORLDLINE_EVENT_PAYMENT = "worldline_payment_received";
    public static final String WORLDLINE_EVENT_REFUND = "worldline_refund_received";


    public enum PAYMENT_METHOD_TYPE {
        CARD("card"),
        REDIRECT("redirect"),
        MOBILE("mobile"),
        DIRECT_DEBIT("directDebit");

        private final String value;

        PAYMENT_METHOD_TYPE(String value) {
            this.value = value;
        }

        public static PAYMENT_METHOD_TYPE fromString(String text) {
            for (PAYMENT_METHOD_TYPE pmt : PAYMENT_METHOD_TYPE.values()) {
                if (pmt.value.equalsIgnoreCase(text)) {
                    return pmt;
                }
            }
            return null;
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

    public enum SEPA_MANDATE_STATUS {
        ACTIVE("ACTIVE"),
        REVOKED("REVOKED"),
        BLOCKED("BLOCKED");
        private final String value;

        SEPA_MANDATE_STATUS(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum SEPA_RECURRING_TYPE {
        UNIQUE("UNIQUE"), RECURRING("RECURRING");
        private final String value;

        SEPA_RECURRING_TYPE(String value) {
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

    public interface ACCOUNT_TYPE {
        String GUEST = "none";
        String CUSTOMER = "created";
    }

    public interface ADDRESS_INDICATEUR {
        String SAME_AS_BILLING = "same-as-billing";
        String NEW = "different-than-billing";
    }
    public enum WEBHOOK_TYPE_ENUM {
        PAYMENT_CREATED("payment.created"),
        PAYMENT_REDIRECTED("payment.redirected"),
        PAYMENT_AUTH_REQUESTED("payment.authorization_requested"),
        PAYMENT_PENDING_APPROVAL("payment.pending_approval"),
        PAYMENT_PENDING_COMPLETION("payment.pending_completion"),
        PAYMENT_PENDING_CAPTURE("payment.pending_capture"),
        PAYMENT_CAPTURE_REQUEST("payment.capture_requested"),
        PAYMENT_CAPTURED("payment.captured"),
        PAYMENT_CHARGEBACKED("payment.chargebacked"),
        PAYMENT_REJECTED("payment.rejected"),
        PAYMENT_REJECTED_CAPTURE("payment.rejected_capture"),
        PAYMENT_CANCELLED("payment.cancelled"),
        PAYMENT_REFUNDED("payment.refunded"),
        PAYMENT_REFUND_REQUESTED("refund.refund_requested");

        WEBHOOK_TYPE_ENUM(String value) {
            this.value = value;
        }

        private final String value;

        public String getValue() {
            return value;
        }

        public static WEBHOOK_TYPE_ENUM fromString(String text) {
            for (WEBHOOK_TYPE_ENUM wte : WEBHOOK_TYPE_ENUM.values()) {
                if (wte.value.equalsIgnoreCase(text)) {
                    return wte;
                }
            }
            return null;
        }

    }


    private WorldlinedirectcoreConstants() {
        //empty to avoid instantiating this constant class
    }


}
