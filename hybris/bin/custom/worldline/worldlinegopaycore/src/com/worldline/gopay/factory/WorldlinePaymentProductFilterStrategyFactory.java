package com.worldline.gopay.factory;

import com.onlinepayments.domain.PaymentProduct;
import com.worldline.gopay.enums.WorldlinePaymentProductFilterEnum;
import de.hybris.platform.commercefacades.order.data.CartData;

import java.util.List;
import java.util.function.Supplier;

public interface WorldlinePaymentProductFilterStrategyFactory {
    Supplier<List<PaymentProduct>> filter(List<PaymentProduct> paymentProducts, WorldlinePaymentProductFilterEnum... paymentProductFilter);

    Supplier<List<PaymentProduct>> filter(List<PaymentProduct> paymentProducts, CartData cartData);
}
