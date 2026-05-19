package com.worldline.direct.factory.impl;

import com.onlinepayments.domain.GPayThreeDSecure;
import com.worldline.direct.enums.WorldlineExemptionType;
import com.worldline.direct.model.WorldlineConfigurationModel;
import com.worldline.direct.model.WorldlineGPayThreeDSecure;
import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@UnitTest
public class WorldlineGooglePayThreeDSecureFactoryTest {

    @Test
    public void createGPayThreeDSecureSetsTraExemptionFieldsForEurOrdersWithinLimit() {
        GPayThreeDSecure threeDSecure = WorldlineThreeDSecureFactory.createGPayThreeDSecure(
              configuration(true, false, WorldlineExemptionType.TRANSACTION_RISK_ANALYSIS, BigDecimal.valueOf(50)),
              order("EUR", 25d));

        assertFalse(threeDSecure.getSkipAuthentication());
        assertFalse(threeDSecure.getSkipSoftDecline());
        assertEquals("no-challenge-requested-risk-analysis-performed", threeDSecure.getChallengeIndicator());
        assertEquals("transaction-risk-analysis", threeDSecure.getExemptionRequest());
        assertTrue(((WorldlineGPayThreeDSecure) threeDSecure).getAcquirerExemption());
    }

    @Test
    public void isGooglePayAcquirerExemptionRequiredOnlyForTraExemption() {
        assertTrue(WorldlineThreeDSecureFactory.isGooglePayAcquirerExemptionRequired(
              configuration(true, false, WorldlineExemptionType.TRANSACTION_RISK_ANALYSIS, BigDecimal.valueOf(50)),
              order("EUR", 25d)));

        assertFalse(WorldlineThreeDSecureFactory.isGooglePayAcquirerExemptionRequired(
              configuration(true, false, WorldlineExemptionType.LOW_VALUE, BigDecimal.valueOf(50)),
              order("EUR", 25d)));
    }

    @Test
    public void createGPayThreeDSecureUsesScaFieldsWhenOrderIsOutsideExemptionLimit() {
        GPayThreeDSecure threeDSecure = WorldlineThreeDSecureFactory.createGPayThreeDSecure(
              configuration(true, true, WorldlineExemptionType.TRANSACTION_RISK_ANALYSIS, BigDecimal.valueOf(50)),
              order("EUR", 75d));

        assertFalse(threeDSecure.getSkipAuthentication());
        assertNull(threeDSecure.getSkipSoftDecline());
        assertEquals("challenge-required", threeDSecure.getChallengeIndicator());
        assertNull(threeDSecure.getExemptionRequest());
    }

    private WorldlineConfigurationModel configuration(boolean enable3ds, boolean mandatory3ds, WorldlineExemptionType exemptionType, BigDecimal exemptionLimit) {
        WorldlineConfigurationModel configuration = mock(WorldlineConfigurationModel.class);
        when(configuration.getEnable3DS()).thenReturn(enable3ds);
        when(configuration.getEnableMandatory3DS()).thenReturn(mandatory3ds);
        when(configuration.getExemptionType3DS()).thenReturn(exemptionType);
        when(configuration.getExemptionLimit3DS()).thenReturn(exemptionLimit);
        return configuration;
    }

    private AbstractOrderModel order(String currencyCode, double totalPrice) {
        CurrencyModel currency = mock(CurrencyModel.class);
        when(currency.getIsocode()).thenReturn(currencyCode);

        AbstractOrderModel order = mock(AbstractOrderModel.class);
        when(order.getCurrency()).thenReturn(currency);
        when(order.getTotalPrice()).thenReturn(totalPrice);
        return order;
    }
}
