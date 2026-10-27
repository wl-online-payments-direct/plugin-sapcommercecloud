package com.worldline.gopay.attributehandlers;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@UnitTest
public class WorldlineThreeDSecureDisplayAttributeHandlerTest {

    // Localization needs a running platform, so the tests use the English defaults.
    private final WorldlineLiabilityDisplayAttributeHandler liabilityHandler = new WorldlineLiabilityDisplayAttributeHandler() {
        @Override
        protected String localize(final String key, final String defaultValue) {
            return defaultValue;
        }
    };

    private final WorldlineAppliedExemptionDisplayAttributeHandler exemptionHandler = new WorldlineAppliedExemptionDisplayAttributeHandler() {
        @Override
        protected String localize(final String key, final String defaultValue) {
            return defaultValue;
        }
    };

    @Test
    public void liabilityIsShownAsFriendlyName() {
        assertEquals("Merchant", liabilityHandler.get(paymentInfo("merchant", null)));
        assertEquals("Issuer", liabilityHandler.get(paymentInfo("issuer", null)));
        assertEquals("Issuer", liabilityHandler.get(paymentInfo("ISSUER", null)));
    }

    @Test
    public void missingLiabilityIsShownAsNotAvailable() {
        assertEquals("NA", liabilityHandler.get(paymentInfo(null, null)));
        assertEquals("NA", liabilityHandler.get(paymentInfo(" ", null)));
    }

    @Test
    public void unknownLiabilityIsShownAsReceived() {
        assertEquals("cardholder", liabilityHandler.get(paymentInfo("cardholder", null)));
    }

    @Test
    public void appliedExemptionIsShownAsFriendlyName() {
        assertEquals("Low value", exemptionHandler.get(paymentInfo(null, "low-value")));
        assertEquals("Transaction risk analysis (acquirer)", exemptionHandler.get(paymentInfo(null, "merchant-acquirer-transaction-risk-analysis")));
        assertEquals("Transaction risk analysis (acquirer)", exemptionHandler.get(paymentInfo(null, "transaction-risk-analysis")));
        assertEquals("No challenge request", exemptionHandler.get(paymentInfo(null, "no-challenge-requested")));
    }

    @Test
    public void missingOrUnknownAppliedExemption() {
        assertEquals("N/A", exemptionHandler.get(paymentInfo(null, null)));
        assertEquals("issuer-transaction-risk-analysis", exemptionHandler.get(paymentInfo(null, "issuer-transaction-risk-analysis")));
    }

    private WorldlinePaymentInfoModel paymentInfo(String liability, String appliedExemption) {
        WorldlinePaymentInfoModel paymentInfo = new WorldlinePaymentInfoModel();
        paymentInfo.setLiability(liability);
        paymentInfo.setAppliedExemption(appliedExemption);
        return paymentInfo;
    }
}
