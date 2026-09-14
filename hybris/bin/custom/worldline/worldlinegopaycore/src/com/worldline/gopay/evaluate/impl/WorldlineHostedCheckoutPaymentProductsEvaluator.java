package com.worldline.gopay.evaluate.impl;

import com.onlinepayments.domain.PaymentProduct;
import com.worldline.gopay.constants.WorldlinegopaycoreConstants;
import com.worldline.gopay.evaluate.WorldlinePaymentProductEvaluator;

import java.util.function.Predicate;

public class WorldlineHostedCheckoutPaymentProductsEvaluator implements WorldlinePaymentProductEvaluator {
    @Override
    public Predicate<PaymentProduct> evaluate() {
        return paymentProduct -> !WorldlinegopaycoreConstants.PAYMENT_METHOD_TYPE.MOBILE.getValue().equals(paymentProduct.getPaymentMethod());
    }
}
