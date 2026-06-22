package com.worldline.direct.factory.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onlinepayments.domain.CreatePaymentRequest;
import com.onlinepayments.domain.GPayThreeDSecure;
import com.onlinepayments.domain.MobilePaymentMethodSpecificInput;
import com.onlinepayments.domain.MobilePaymentProduct320SpecificInput;
import com.onlinepayments.json.DefaultMarshaller;
import com.worldline.direct.enums.WorldlineExemptionType;
import com.worldline.direct.model.WorldlineConfigurationModel;
import com.worldline.direct.model.WorldlineGPayThreeDSecure;
import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.CartModel;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
    public void createGPayThreeDSecureSerializesAcquirerExemptionForGooglePayTra() {
        GPayThreeDSecure threeDSecure = WorldlineThreeDSecureFactory.createGPayThreeDSecure(
              configuration(true, false, WorldlineExemptionType.TRANSACTION_RISK_ANALYSIS, BigDecimal.valueOf(50)),
              order("EUR", 25d));

        MobilePaymentProduct320SpecificInput product320SpecificInput = new MobilePaymentProduct320SpecificInput();
        product320SpecificInput.setThreeDSecure(threeDSecure);
        MobilePaymentMethodSpecificInput mobilePaymentMethodSpecificInput = new MobilePaymentMethodSpecificInput();
        mobilePaymentMethodSpecificInput.setPaymentProduct320SpecificInput(product320SpecificInput);
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setMobilePaymentMethodSpecificInput(mobilePaymentMethodSpecificInput);

        String json = DefaultMarshaller.INSTANCE.marshal(request);
        JsonObject threeDSecureJson = new JsonParser().parse(json).getAsJsonObject()
              .getAsJsonObject("mobilePaymentMethodSpecificInput")
              .getAsJsonObject("paymentProduct320SpecificInput")
              .getAsJsonObject("threeDSecure");

        assertTrue(json.contains("\"acquirerExemption\":true"));
        assertTrue(threeDSecureJson.get("acquirerExemption").getAsBoolean());
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
        WorldlineConfigurationModel configuration = new WorldlineConfigurationModel();
        configuration.setEnable3DS(enable3ds);
        configuration.setEnableMandatory3DS(mandatory3ds);
        configuration.setExemptionType3DS(exemptionType);
        configuration.setExemptionLimit3DS(exemptionLimit);
        return configuration;
    }

    private AbstractOrderModel order(String currencyCode, double totalPrice) {
        CurrencyModel currency = new CurrencyModel();
        currency.setIsocode(currencyCode);

        CartModel order = new CartModel();
        order.setCurrency(currency);
        order.setTotalPrice(totalPrice);
        return order;
    }
}
