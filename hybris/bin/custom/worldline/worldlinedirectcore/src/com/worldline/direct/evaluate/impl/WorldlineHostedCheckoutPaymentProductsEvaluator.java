package com.worldline.direct.evaluate.impl;

import com.ingenico.direct.domain.PaymentProduct;
import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import com.worldline.direct.evaluate.WorldlinePaymentProductEvaluator;

import java.util.function.Predicate;

public class WorldlineHostedCheckoutPaymentProductsEvaluator implements WorldlinePaymentProductEvaluator {
    @Override
    public Predicate<PaymentProduct> evaluate() {
        return paymentProduct -> !WorldlinedirectcoreConstants.PAYMENT_METHOD_TYPE.MOBILE.getValue().equals(paymentProduct.getPaymentMethod());
    }
}
