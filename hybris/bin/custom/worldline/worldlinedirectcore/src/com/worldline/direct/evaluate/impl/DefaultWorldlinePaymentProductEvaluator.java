package com.worldline.direct.evaluate.impl;

import com.onlinepayments.domain.PaymentProduct;
import com.worldline.direct.evaluate.WorldlinePaymentProductEvaluator;
import org.springframework.beans.factory.annotation.Required;

import java.util.function.Predicate;

public class DefaultWorldlinePaymentProductEvaluator implements WorldlinePaymentProductEvaluator {
    private Integer paymentProductId;

    @Override
    public Predicate<PaymentProduct> evaluate() {
        return paymentProduct -> paymentProductId.equals(paymentProduct.getId());
    }

    @Required
    public void setPaymentProductId(Integer paymentProductId) {
        this.paymentProductId = paymentProductId;
    }
}
