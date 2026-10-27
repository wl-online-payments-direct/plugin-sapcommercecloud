package com.worldline.gopay.attributehandlers;

import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;

public class WorldlineLiabilityDisplayAttributeHandler extends AbstractWorldlineThreeDSecureDisplayAttributeHandler {

    static final String MERCHANT_KEY = "com.worldline.gopay.3ds.liability.merchant";
    static final String ISSUER_KEY = "com.worldline.gopay.3ds.liability.issuer";
    static final String NOT_AVAILABLE_KEY = "com.worldline.gopay.3ds.liability.na";

    @Override
    protected String getRawValue(final WorldlinePaymentInfoModel paymentInfo) {
        return paymentInfo.getLiability();
    }

    @Override
    protected String getKeyForValue(final String rawValue) {
        switch (rawValue) {
            case "merchant":
                return MERCHANT_KEY;
            case "issuer":
                return ISSUER_KEY;
            default:
                return null;
        }
    }

    @Override
    protected String getDefaultForKey(final String key) {
        return MERCHANT_KEY.equals(key) ? "Merchant" : "Issuer";
    }

    @Override
    protected String getEmptyKey() {
        return NOT_AVAILABLE_KEY;
    }

    @Override
    protected String getEmptyDefault() {
        return "NA";
    }
}
