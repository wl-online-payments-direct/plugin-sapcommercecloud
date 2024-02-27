package com.worldline.direct.strategy;

import com.onlinepayments.domain.PaymentProduct;

import java.util.List;

public interface WorldlinePaymentProductFilterStrategy {
    List<PaymentProduct> filter(List<PaymentProduct> paymentProducts);
}
