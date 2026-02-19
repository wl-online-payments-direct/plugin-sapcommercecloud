package com.worldline.direct.factory.impl;

import com.onlinepayments.domain.PaymentProduct;
import com.worldline.direct.enums.WorldlinePaymentProductFilterByCartDataEnum;
import com.worldline.direct.enums.WorldlinePaymentProductFilterEnum;
import com.worldline.direct.factory.WorldlinePaymentProductFilterStrategyFactory;
import com.worldline.direct.strategy.WorldlinePaymentProductFilterByCartDataStrategy;
import com.worldline.direct.strategy.WorldlinePaymentProductFilterStrategy;
import de.hybris.platform.commercefacades.order.data.CartData;
import org.springframework.beans.factory.annotation.Required;

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

    @Required
    public void setPaymentProductFilterStrategyMap(Map<WorldlinePaymentProductFilterEnum, WorldlinePaymentProductFilterStrategy> paymentProductFilterStrategyMap) {
        this.paymentProductFilterStrategyMap = paymentProductFilterStrategyMap;
    }

    @Required
    public void setPaymentProductFilterByCartDataStrategyMap(Map<WorldlinePaymentProductFilterByCartDataEnum, WorldlinePaymentProductFilterByCartDataStrategy> paymentProductFilterByCartDataStrategyMap) {
        this.paymentProductFilterByCartDataStrategyMap = paymentProductFilterByCartDataStrategyMap;
    }
}
