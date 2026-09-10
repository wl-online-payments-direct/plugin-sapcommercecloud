package com.worldline.gopay.factory.impl;

import com.onlinepayments.domain.PaymentProduct;
import com.worldline.gopay.enums.WorldlinePaymentProductFilterByCartDataEnum;
import com.worldline.gopay.enums.WorldlinePaymentProductFilterEnum;
import com.worldline.gopay.factory.WorldlinePaymentProductFilterStrategyFactory;
import com.worldline.gopay.strategy.WorldlinePaymentProductFilterByCartDataStrategy;
import com.worldline.gopay.strategy.WorldlinePaymentProductFilterStrategy;
import de.hybris.platform.commercefacades.order.data.CartData;

import java.util.*;
import java.util.function.Supplier;

public class DefaultWorldlinePaymentProductFilterStrategyFactory implements WorldlinePaymentProductFilterStrategyFactory {
    private Map<WorldlinePaymentProductFilterEnum, WorldlinePaymentProductFilterStrategy> paymentProductFilterStrategyMap;
    private Map<WorldlinePaymentProductFilterByCartDataEnum, WorldlinePaymentProductFilterByCartDataStrategy> paymentProductFilterByCartDataStrategyMap;

    @Override
    public Supplier<List<PaymentProduct>> filter(List<PaymentProduct> paymentProducts, WorldlinePaymentProductFilterEnum... paymentProductFilters) {
        return () -> {
            List<PaymentProduct> paymentProductList = new ArrayList<>(paymentProducts);
            for (WorldlinePaymentProductFilterEnum paymentProductFilter : paymentProductFilters) {
                paymentProductList = paymentProductFilterStrategyMap.get(paymentProductFilter).filter(paymentProductList);
            }
            return paymentProductList;
        };
    }

    @Override
    public Supplier<List<PaymentProduct>> filter(List<PaymentProduct> paymentProducts, CartData cartData) {
        return () -> {
            Set<Integer> paymentProductCodes = new HashSet<>();
            paymentProductFilterByCartDataStrategyMap.forEach((key, value) -> {
                paymentProductCodes.addAll(value.filter(cartData));
            });

            // Remove any returned Payment Product IDs.
            List<PaymentProduct> output = new ArrayList<>(paymentProducts);
            output.removeIf(p -> paymentProductCodes.contains(p.getId()));

            return output;
        };
    }

    public void setPaymentProductFilterStrategyMap(Map<WorldlinePaymentProductFilterEnum, WorldlinePaymentProductFilterStrategy> paymentProductFilterStrategyMap) {
        this.paymentProductFilterStrategyMap = paymentProductFilterStrategyMap;
    }

    public void setPaymentProductFilterByCartDataStrategyMap(Map<WorldlinePaymentProductFilterByCartDataEnum, WorldlinePaymentProductFilterByCartDataStrategy> paymentProductFilterByCartDataStrategyMap) {
        this.paymentProductFilterByCartDataStrategyMap = paymentProductFilterByCartDataStrategyMap;
    }
}
