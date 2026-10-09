package com.worldline.gopay.factory.impl;

import com.onlinepayments.domain.ThreeDSecure;
import com.onlinepayments.domain.ThreeDSecureBase;
import com.worldline.gopay.enums.WorldlineExemptionType;
import com.worldline.gopay.model.WorldlineConfigurationModel;
import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.CartModel;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@UnitTest
public class WorldlineThreeDSecureFactoryExemptionLimitTest {

    @Test
    public void hostedCheckoutRequestsExemptionWhenOrderEqualsLimit() {
        ThreeDSecureBase threeDSecure = WorldlineThreeDSecureFactory.createThreeDSecureBase(
              configuration(WorldlineExemptionType.TRANSACTION_RISK_ANALYSIS, "95"), order(95d));

        assertEquals("transaction-risk-analysis", threeDSecure.getExemptionRequest());
        assertEquals("no-challenge-requested-risk-analysis-performed", threeDSecure.getChallengeIndicator());
    }

    @Test
    public void hostedTokenizationRequestsExemptionWhenOrderEqualsLimit() {
        ThreeDSecure threeDSecure = WorldlineThreeDSecureFactory.createThreeDSecure(
              configuration(WorldlineExemptionType.LOW_VALUE, "30"), order(30d));

        assertEquals("low-value", threeDSecure.getExemptionRequest());
        assertEquals("no-challenge-requested", threeDSecure.getChallengeIndicator());
    }

    @Test
    public void exemptionNotRequestedWhenOrderIsAboveLimit() {
        ThreeDSecureBase threeDSecure = WorldlineThreeDSecureFactory.createThreeDSecureBase(
              configuration(WorldlineExemptionType.LOW_VALUE, "30"), order(30.01d));

        assertNull(threeDSecure.getExemptionRequest());
        assertEquals("no-preference", threeDSecure.getChallengeIndicator());
    }

    @Test
    public void exemptionNotRequestedWhenLimitIsMissing() {
        ThreeDSecureBase threeDSecure = WorldlineThreeDSecureFactory.createThreeDSecureBase(
              configuration(WorldlineExemptionType.LOW_VALUE, null), order(10d));

        assertNull(threeDSecure.getExemptionRequest());
        assertEquals("no-preference", threeDSecure.getChallengeIndicator());
    }

    private WorldlineConfigurationModel configuration(WorldlineExemptionType exemptionType, String exemptionLimit) {
        WorldlineConfigurationModel configuration = new WorldlineConfigurationModel();
        configuration.setEnable3DS(true);
        configuration.setEnableMandatory3DS(false);
        configuration.setExemptionType3DS(exemptionType);
        configuration.setExemptionLimit3DS(exemptionLimit == null ? null : new BigDecimal(exemptionLimit));
        return configuration;
    }

    private AbstractOrderModel order(double totalPrice) {
        CurrencyModel currency = new CurrencyModel();
        currency.setIsocode("EUR");

        CartModel order = new CartModel();
        order.setCurrency(currency);
        order.setTotalPrice(totalPrice);
        return order;
    }
}
