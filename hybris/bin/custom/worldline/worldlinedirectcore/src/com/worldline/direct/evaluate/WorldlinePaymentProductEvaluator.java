package com.worldline.direct.evaluate;

import com.onlinepayments.domain.PaymentProduct;

import java.util.function.Predicate;

public interface WorldlinePaymentProductEvaluator {
    Predicate<PaymentProduct> evaluate();
}
