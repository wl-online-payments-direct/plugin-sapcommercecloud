package com.worldline.gopay.evaluate.impl;

import com.onlinepayments.domain.PaymentProduct;
import com.worldline.gopay.constants.WorldlinegopaycoreConstants;
import com.worldline.gopay.evaluate.WorldlinePaymentProductEvaluator;

import java.util.function.Predicate;

public class WorldlineHostedTokenizationPaymentProductsEvaluator implements WorldlinePaymentProductEvaluator {
    @Override
    public Predicate<PaymentProduct> evaluate() {
        Predicate<PaymentProduct> hostedTokenizationPredicate = (paymentProduct -> WorldlinegopaycoreConstants.PAYMENT_METHOD_TYPE.CARD.getValue().equals(paymentProduct.getPaymentMethod()));
        return hostedTokenizationPredicate.negate();
    }
}
