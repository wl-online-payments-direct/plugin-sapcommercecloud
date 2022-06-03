package com.worldline.direct.evaluate;

import com.ingenico.direct.domain.PaymentProduct;

import java.util.function.Predicate;

public interface WorldlinePaymentProductEvaluator {
    Predicate<PaymentProduct> evaluate();
}
