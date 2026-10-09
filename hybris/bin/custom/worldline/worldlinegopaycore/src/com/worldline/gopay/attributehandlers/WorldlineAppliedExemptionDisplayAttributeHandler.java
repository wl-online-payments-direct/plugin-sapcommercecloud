package com.worldline.gopay.attributehandlers;

import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;

public class WorldlineAppliedExemptionDisplayAttributeHandler extends AbstractWorldlineThreeDSecureDisplayAttributeHandler {

    static final String NO_CHALLENGE_REQUEST_KEY = "com.worldline.gopay.3ds.exemption.nochallengerequest";
    static final String LOW_VALUE_KEY = "com.worldline.gopay.3ds.exemption.lowvalue";
    static final String TRANSACTION_RISK_ANALYSIS_KEY = "com.worldline.gopay.3ds.exemption.transactionriskanalysis";
    static final String NOT_AVAILABLE_KEY = "com.worldline.gopay.3ds.exemption.na";

    @Override
    protected String getRawValue(final WorldlinePaymentInfoModel paymentInfo) {
        return paymentInfo.getAppliedExemption();
    }

    @Override
    protected String getKeyForValue(final String rawValue) {
        switch (rawValue) {
            case "no-challenge-requested":
                return NO_CHALLENGE_REQUEST_KEY;
            case "low-value":
                return LOW_VALUE_KEY;
            case "transaction-risk-analysis":
            case "merchant-acquirer-transaction-risk-analysis":
                return TRANSACTION_RISK_ANALYSIS_KEY;
            default:
                return null;
        }
    }

    @Override
    protected String getDefaultForKey(final String key) {
        if (NO_CHALLENGE_REQUEST_KEY.equals(key)) {
            return "No challenge request";
        }
        return LOW_VALUE_KEY.equals(key) ? "Low value" : "Transaction risk analysis (acquirer)";
    }

    @Override
    protected String getEmptyKey() {
        return NOT_AVAILABLE_KEY;
    }

    @Override
    protected String getEmptyDefault() {
        return "N/A";
    }
}
