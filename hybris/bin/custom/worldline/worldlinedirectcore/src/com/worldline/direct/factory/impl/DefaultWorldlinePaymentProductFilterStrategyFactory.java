package com.worldline.direct.factory.impl;

import com.ingenico.direct.domain.PaymentProduct;
import com.worldline.direct.enums.WorldlinePaymentProductFilterEnum;
import com.worldline.direct.factory.WorldlinePaymentProductFilterStrategyFactory;
import com.worldline.direct.strategy.WorldlinePaymentProductFilterStrategy;
import org.springframework.beans.factory.annotation.Required;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class DefaultWorldlinePaymentProductFilterStrategyFactory implements WorldlinePaymentProductFilterStrategyFactory {
    private Map<WorldlinePaymentProductFilterEnum, WorldlinePaymentProductFilterStrategy> paymentProductFilterStrategyMap;

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

    @Required
    public void setPaymentProductFilterStrategyMap(Map<WorldlinePaymentProductFilterEnum, WorldlinePaymentProductFilterStrategy> paymentProductFilterStrategyMap) {
        this.paymentProductFilterStrategyMap = paymentProductFilterStrategyMap;
    }
}
