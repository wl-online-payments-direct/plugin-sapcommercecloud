package com.worldline.direct.factory.impl;

import com.onlinepayments.domain.ThreeDSecure;
import com.onlinepayments.domain.ThreeDSecureBase;
import com.worldline.direct.enums.WorldlineExemptionType;
import com.worldline.direct.model.WorldlineConfigurationModel;
import de.hybris.platform.core.model.order.AbstractOrderModel;

import java.math.BigDecimal;

public class WorldlineThreeDSecureFactory {
    private static final String CHALLENGE_REQUIRED = "challenge-required";
    private static final String NO_PREFERENCE = "no-preference";
    private static final String NO_CHALLENGE_REQUESTED = "no-challenge-requested";
    private static final String NO_CHALLENGE_REQUESTED_RISK_ANALYSIS = "no-challenge-requested-risk-analysis-performed";

    private static final String EXEMPTION_LOW_VALUE = "low-value";
    private static final String EXEMPTION_TRANSACTION_RISK_ANALYSIS = "transaction-risk-analysis";

    public static ThreeDSecure createThreeDSecure(WorldlineConfigurationModel config, AbstractOrderModel order) {
        if (order.getCurrency() == null) {
            return null;
        }

        ThreeDSecure threeDSecure = new ThreeDSecure();

        if (!"EUR".equals(order.getCurrency().getIsocode())) {
            // Non-EUR: no exemption logic
            threeDSecure.setSkipAuthentication(!config.getEnable3DS());
            if (config.getEnable3DS()) {
                threeDSecure.setChallengeIndicator(config.getEnableMandatory3DS() ? CHALLENGE_REQUIRED : NO_PREFERENCE);
            }
            return threeDSecure;
        }

        boolean exemptionEnabled = isExemptionEnabled(config);
        boolean withinLimit = exemptionEnabled && isWithinExemptionLimit(config, order);

        if (config.getEnable3DS()) {
            if (exemptionEnabled && withinLimit) {
                applyExemption(threeDSecure, config.getExemptionType3DS());
            } else {
                // SCA logic
                threeDSecure.setSkipAuthentication(false);
                threeDSecure.setChallengeIndicator(config.getEnableMandatory3DS() ? CHALLENGE_REQUIRED : NO_PREFERENCE);
            }
        } else {
            if (exemptionEnabled && withinLimit) {
                applyExemption(threeDSecure, config.getExemptionType3DS());
            } else {
                // Standard: no 3DS, no exemption
                threeDSecure.setSkipAuthentication(true);
            }
        }

        return threeDSecure;
    }

    public static ThreeDSecureBase createThreeDSecureBase(WorldlineConfigurationModel config, AbstractOrderModel order) {
        if (order.getCurrency() == null) {
            return null;
        }

        ThreeDSecureBase threeDSecure = new ThreeDSecureBase();

        if (!"EUR".equals(order.getCurrency().getIsocode())) {
            // Non-EUR: no exemption logic
            threeDSecure.setSkipAuthentication(!config.getEnable3DS());
            if (config.getEnable3DS()) {
                threeDSecure.setChallengeIndicator(config.getEnableMandatory3DS() ? CHALLENGE_REQUIRED : NO_PREFERENCE);
            }
            return threeDSecure;
        }

        boolean exemptionEnabled = isExemptionEnabled(config);
        boolean withinLimit = exemptionEnabled && isWithinExemptionLimit(config, order);

        if (config.getEnable3DS()) {
            if (exemptionEnabled && withinLimit) {
                applyExemption(threeDSecure, config.getExemptionType3DS());
            } else {
                // SCA logic
                threeDSecure.setSkipAuthentication(false);
                threeDSecure.setChallengeIndicator(config.getEnableMandatory3DS() ? CHALLENGE_REQUIRED : NO_PREFERENCE);
            }
        } else {
            if (exemptionEnabled && withinLimit) {
                applyExemption(threeDSecure, config.getExemptionType3DS());
            } else {
                // Standard: no 3DS, no exemption
                threeDSecure.setSkipAuthentication(true);
            }
        }

        return threeDSecure;
    }

    private static boolean isExemptionEnabled(WorldlineConfigurationModel config) {
        WorldlineExemptionType type = config.getExemptionType3DS();
        return type != null && !WorldlineExemptionType.NO_3DS_EXEMPTION.equals(type);
    }

    private static boolean isWithinExemptionLimit(WorldlineConfigurationModel config, AbstractOrderModel order) {
        BigDecimal limit = config.getExemptionLimit3DS();
        BigDecimal total = BigDecimal.valueOf(order.getTotalPrice());
        return limit.compareTo(total) > 0;
    }

    private static void applyExemption(ThreeDSecure threeDSecure, WorldlineExemptionType type) {
        threeDSecure.setSkipAuthentication(false);
        threeDSecure.setSkipSoftDecline(false);

        switch (type) {
            case NO_CHALLENGE_REQUEST:
                threeDSecure.setChallengeIndicator(NO_CHALLENGE_REQUESTED);
                break;
            case LOW_VALUE:
                threeDSecure.setChallengeIndicator(NO_CHALLENGE_REQUESTED);
                threeDSecure.setExemptionRequest(EXEMPTION_LOW_VALUE);
                break;
            case TRANSACTION_RISK_ANALYSIS:
                threeDSecure.setChallengeIndicator(NO_CHALLENGE_REQUESTED_RISK_ANALYSIS);
                threeDSecure.setExemptionRequest(EXEMPTION_TRANSACTION_RISK_ANALYSIS);
                break;
            default:
                break;
        }
    }

    private static void applyExemption(ThreeDSecureBase threeDSecure, WorldlineExemptionType type) {
        threeDSecure.setSkipAuthentication(false);
        threeDSecure.setSkipSoftDecline(false);

        switch (type) {
            case NO_CHALLENGE_REQUEST:
                threeDSecure.setChallengeIndicator(NO_CHALLENGE_REQUESTED);
                break;
            case LOW_VALUE:
                threeDSecure.setChallengeIndicator(NO_CHALLENGE_REQUESTED);
                threeDSecure.setExemptionRequest(EXEMPTION_LOW_VALUE);
                break;
            case TRANSACTION_RISK_ANALYSIS:
                threeDSecure.setChallengeIndicator(NO_CHALLENGE_REQUESTED_RISK_ANALYSIS);
                threeDSecure.setExemptionRequest(EXEMPTION_TRANSACTION_RISK_ANALYSIS);
                break;
            default:
                break;
        }
    }
}
