package com.worldline.direct.evaluate.impl;

import com.onlinepayments.domain.PaymentProduct;
import com.worldline.direct.evaluate.WorldlinePaymentProductEvaluator;
import com.worldline.direct.service.WorldlinePaymentModeService;

import java.util.function.Predicate;

public class IntersolveWorldlinePaymentProductEvaluator implements WorldlinePaymentProductEvaluator {
    private WorldlinePaymentModeService worldlinePaymentModeService;
    @Override
    public Predicate<PaymentProduct> evaluate() {
        return paymentProduct -> worldlinePaymentModeService.isIntersolve(String.valueOf(paymentProduct.getId()));
    }

    public void setWorldlinePaymentModeService(WorldlinePaymentModeService worldlinePaymentModeService) {
        this.worldlinePaymentModeService = worldlinePaymentModeService;
    }
}
