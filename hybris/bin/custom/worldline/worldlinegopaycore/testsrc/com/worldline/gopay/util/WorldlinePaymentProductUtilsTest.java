package com.worldline.gopay.util;

import com.onlinepayments.domain.PaymentProduct;
import com.worldline.gopay.constants.WorldlinegopaycoreConstants;
import de.hybris.bootstrap.annotations.UnitTest;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@UnitTest
public class WorldlinePaymentProductUtilsTest {

    @Test
    public void mobileWalletsAreDetectedByPaymentMethod() {
        assertTrue(WorldlinePaymentProductUtils.isMobileWallet(
              paymentProduct(WorldlinegopaycoreConstants.PAYMENT_METHOD_GOOGLEPAY, WorldlinegopaycoreConstants.PAYMENT_METHOD_TYPE.MOBILE.getValue())));
        assertTrue(WorldlinePaymentProductUtils.isMobileWallet(
              paymentProduct(WorldlinegopaycoreConstants.PAYMENT_METHOD_APPLEPAY, WorldlinegopaycoreConstants.PAYMENT_METHOD_TYPE.MOBILE.getValue())));
        assertFalse(WorldlinePaymentProductUtils.isMobileWallet(
              paymentProduct(WorldlinegopaycoreConstants.PAYMENT_METHOD_VISA, WorldlinegopaycoreConstants.PAYMENT_METHOD_TYPE.CARD.getValue())));
        assertFalse(WorldlinePaymentProductUtils.isMobileWallet(
              paymentProduct(WorldlinegopaycoreConstants.PAYMENT_METHOD_IDEAL, null)));
        assertFalse(WorldlinePaymentProductUtils.isMobileWallet(null));
    }

    @Test
    public void mobileWalletsAreSplitOutInTheirOriginalOrder() {
        List<PaymentProduct> paymentProducts = Arrays.asList(
              paymentProduct(WorldlinegopaycoreConstants.PAYMENT_METHOD_VISA, WorldlinegopaycoreConstants.PAYMENT_METHOD_TYPE.CARD.getValue()),
              paymentProduct(WorldlinegopaycoreConstants.PAYMENT_METHOD_GOOGLEPAY, WorldlinegopaycoreConstants.PAYMENT_METHOD_TYPE.MOBILE.getValue()),
              paymentProduct(WorldlinegopaycoreConstants.PAYMENT_METHOD_IDEAL, WorldlinegopaycoreConstants.PAYMENT_METHOD_TYPE.REDIRECT.getValue()),
              paymentProduct(WorldlinegopaycoreConstants.PAYMENT_METHOD_APPLEPAY, WorldlinegopaycoreConstants.PAYMENT_METHOD_TYPE.MOBILE.getValue()),
              paymentProduct(WorldlinegopaycoreConstants.PAYMENT_METHOD_SEPA, WorldlinegopaycoreConstants.PAYMENT_METHOD_TYPE.DIRECT_DEBIT.getValue()));

        assertEquals(Arrays.asList(WorldlinegopaycoreConstants.PAYMENT_METHOD_GOOGLEPAY, WorldlinegopaycoreConstants.PAYMENT_METHOD_APPLEPAY),
              ids(WorldlinePaymentProductUtils.getMobileWallets(paymentProducts)));
        assertEquals(Arrays.asList(WorldlinegopaycoreConstants.PAYMENT_METHOD_VISA, WorldlinegopaycoreConstants.PAYMENT_METHOD_IDEAL, WorldlinegopaycoreConstants.PAYMENT_METHOD_SEPA),
              ids(WorldlinePaymentProductUtils.getNonMobileWallets(paymentProducts)));
    }

    @Test
    public void splittingLeavesTheListUntouchedWhenThereAreNoMobileWallets() {
        List<PaymentProduct> paymentProducts = Arrays.asList(
              paymentProduct(WorldlinegopaycoreConstants.PAYMENT_METHOD_VISA, WorldlinegopaycoreConstants.PAYMENT_METHOD_TYPE.CARD.getValue()),
              paymentProduct(WorldlinegopaycoreConstants.PAYMENT_METHOD_IDEAL, WorldlinegopaycoreConstants.PAYMENT_METHOD_TYPE.REDIRECT.getValue()));

        assertTrue(WorldlinePaymentProductUtils.getMobileWallets(paymentProducts).isEmpty());
        assertEquals(ids(paymentProducts), ids(WorldlinePaymentProductUtils.getNonMobileWallets(paymentProducts)));
    }

    @Test
    public void splittingHandlesEmptyAndNullLists() {
        assertTrue(WorldlinePaymentProductUtils.getMobileWallets(null).isEmpty());
        assertTrue(WorldlinePaymentProductUtils.getNonMobileWallets(null).isEmpty());
        assertTrue(WorldlinePaymentProductUtils.getMobileWallets(Collections.emptyList()).isEmpty());
        assertTrue(WorldlinePaymentProductUtils.getNonMobileWallets(Collections.emptyList()).isEmpty());
    }

    private List<Integer> ids(List<PaymentProduct> paymentProducts) {
        return paymentProducts.stream().map(PaymentProduct::getId).collect(Collectors.toList());
    }

    private PaymentProduct paymentProduct(Integer id, String paymentMethod) {
        PaymentProduct paymentProduct = new PaymentProduct();
        paymentProduct.setId(id);
        paymentProduct.setPaymentMethod(paymentMethod);
        return paymentProduct;
    }
}
