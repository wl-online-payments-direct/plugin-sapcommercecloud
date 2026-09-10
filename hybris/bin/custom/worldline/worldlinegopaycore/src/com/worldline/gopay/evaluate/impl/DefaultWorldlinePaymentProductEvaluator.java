package com.worldline.gopay.evaluate.impl;

import com.onlinepayments.domain.PaymentProduct;
import com.worldline.gopay.evaluate.WorldlinePaymentProductEvaluator;

import java.util.function.Predicate;

public class DefaultWorldlinePaymentProductEvaluator implements WorldlinePaymentProductEvaluator {
    private Integer paymentProductId;

    @Override
    public Predicate<PaymentProduct> evaluate() {
        return paymentProduct -> paymentProductId.equals(paymentProduct.getId());
    }

    public void setPaymentProductId(Integer paymentProductId) {
        this.paymentProductId = paymentProductId;
    }
}
