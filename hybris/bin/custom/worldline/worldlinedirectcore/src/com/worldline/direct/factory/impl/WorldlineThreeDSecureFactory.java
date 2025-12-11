package com.worldline.direct.factory.impl;

import com.onlinepayments.domain.ThreeDSecure;
import com.onlinepayments.domain.ThreeDSecureBase;
import com.worldline.direct.enums.WorldlineExemptionType;
import com.worldline.direct.model.WorldlineConfigurationModel;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.enumeration.EnumerationService;

import java.math.BigDecimal;

public class WorldlineThreeDSecureFactory {
    private static final String CHALLENGE_REQUIRED = "challenge-required";
    private static final String NO_PREFERENCE = "no-preference";

    public static ThreeDSecure createThreeDSecure(WorldlineConfigurationModel config, AbstractOrderModel order, EnumerationService enumService) {
        if (order.getCurrency() == null) {
            // Need to know the currency to generate this object.
            return null;
        }

        ThreeDSecure threeDSecure = new ThreeDSecure();
        if (!"EUR".equals(order.getCurrency().getIsocode())) {
            // If currency is not EUR, skip exemption logic and use simplified version:
            threeDSecure.setSkipAuthentication(!config.getEnable3DS());
            threeDSecure.setChallengeIndicator(config.getEnableMandatory3DS() ? CHALLENGE_REQUIRED : NO_PREFERENCE);

            return threeDSecure;
        }


        if (!config.getEnable3DS()) {
            threeDSecure.setSkipAuthentication(true);
            return threeDSecure;
        }

        threeDSecure.setSkipAuthentication(false);

        String fallbackIndicator = config.getEnableMandatory3DS()
                ? CHALLENGE_REQUIRED
                : NO_PREFERENCE;

        if (shouldApplyExemption(config, order)) {
            threeDSecure.setExemptionRequest(enumService.getEnumerationName(config.getExemptionType3DS()));
            threeDSecure.setSkipAuthentication(true);
            threeDSecure.setSkipSoftDecline(false);
        } else {
            threeDSecure.setChallengeIndicator(fallbackIndicator);
        }

        return threeDSecure;
    }


    public static ThreeDSecureBase createThreeDSecureBase(WorldlineConfigurationModel config, AbstractOrderModel order, EnumerationService enumService) {
        if (order.getCurrency() == null) {
            // Need to know the currency to generate this object.
            return null;
        }

        ThreeDSecureBase threeDSecure = new ThreeDSecureBase();
        if (!"EUR".equals(order.getCurrency().getIsocode())) {
            // If currency is not EUR, skip exemption logic and use simplified version:
            threeDSecure.setSkipAuthentication(!config.getEnable3DS());
            threeDSecure.setChallengeIndicator(config.getEnableMandatory3DS() ? CHALLENGE_REQUIRED : NO_PREFERENCE);

            return threeDSecure;
        }

        if (!config.getEnable3DS()) {
            threeDSecure.setSkipAuthentication(true);
            return threeDSecure;
        }

        threeDSecure.setSkipAuthentication(false);

        String fallbackIndicator = config.getEnableMandatory3DS()
                ? CHALLENGE_REQUIRED
                : NO_PREFERENCE;

        if (shouldApplyExemption(config, order)) {
            threeDSecure.setExemptionRequest(enumService.getEnumerationName(config.getExemptionType3DS()));
            threeDSecure.setSkipAuthentication(true);
            threeDSecure.setSkipSoftDecline(false);
        } else {
            threeDSecure.setChallengeIndicator(fallbackIndicator);
        }

        return threeDSecure;
    }

    /**
     * Checks if the configuration and order details qualify for an exemption.
     */
    private static boolean shouldApplyExemption(WorldlineConfigurationModel config, AbstractOrderModel order) {
        WorldlineExemptionType type = config.getExemptionType3DS();

        if (type == null || WorldlineExemptionType.NO_3DS_EXEMPTION.equals(type)) {
            return false;
        }

        switch (type) {
            case LOW_VALUE, TRANSACTION_RISK_ANALYSIS -> {
                BigDecimal limit = config.getExemptionLimit3DS();
                BigDecimal total = BigDecimal.valueOf(order.getTotalPrice());

                return limit.compareTo(total) > 0;
            }
            default -> {
                return false;
            }
        }
    }
}