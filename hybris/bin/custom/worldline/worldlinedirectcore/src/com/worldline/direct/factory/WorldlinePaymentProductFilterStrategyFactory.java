package com.worldline.direct.factory;

import com.onlinepayments.domain.PaymentProduct;
import com.worldline.direct.enums.WorldlinePaymentProductFilterEnum;

import java.util.List;
import java.util.function.Supplier;

public interface WorldlinePaymentProductFilterStrategyFactory {
    Supplier<List<PaymentProduct>> filter(List<PaymentProduct> paymentProducts, WorldlinePaymentProductFilterEnum... paymentProductFilter);

    Boolean checkForCardPaymentMethods(List<PaymentProduct> paymentProducts);
}
