package com.worldline.direct.factory.impl;

import com.onlinepayments.domain.PaymentProduct;
import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import com.worldline.direct.enums.WorldlinePaymentProductFilterEnum;
import com.worldline.direct.factory.WorldlinePaymentProductFilterStrategyFactory;
import com.worldline.direct.strategy.WorldlinePaymentProductFilterStrategy;
import org.apache.commons.lang.StringUtils;
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

    @Override
    public Boolean checkForCardPaymentMethods(List<PaymentProduct> paymentProducts) {
        for (PaymentProduct paymentProduct : paymentProducts) {
           if (StringUtils.equals(paymentProduct.getPaymentMethod(), WorldlinedirectcoreConstants.PAYMENT_METHOD_TYPE.CARD.getValue())) {
               return Boolean.TRUE;
           }
        }

        return Boolean.FALSE;
    }

    @Required
    public void setPaymentProductFilterStrategyMap(Map<WorldlinePaymentProductFilterEnum, WorldlinePaymentProductFilterStrategy> paymentProductFilterStrategyMap) {
        this.paymentProductFilterStrategyMap = paymentProductFilterStrategyMap;
    }
}
