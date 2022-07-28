package com.worldline.direct.evaluate.impl;

import com.onlinepayments.domain.PaymentProduct;
import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import com.worldline.direct.evaluate.WorldlinePaymentProductEvaluator;

import java.util.function.Predicate;

public class WorldlineHostedTokenizationPaymentProductsEvaluator implements WorldlinePaymentProductEvaluator {
    @Override
    public Predicate<PaymentProduct> evaluate() {
        Predicate<PaymentProduct> hostedTokenizationPredicate = (paymentProduct -> WorldlinedirectcoreConstants.PAYMENT_METHOD_TYPE.CARD.getValue().equals(paymentProduct.getPaymentMethod()));
        return hostedTokenizationPredicate.negate();
    }
}
