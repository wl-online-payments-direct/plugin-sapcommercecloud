package com.worldline.direct.strategy.impl;

import com.onlinepayments.domain.PaymentProduct;
import com.worldline.direct.model.WorldlineConfigurationModel;
import com.worldline.direct.service.WorldlineConfigurationService;
import com.worldline.direct.strategy.WorldlinePaymentProductFilterStrategy;
import de.hybris.platform.core.model.order.payment.PaymentModeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class WorldlinePaymentProductSortStrategy implements WorldlinePaymentProductFilterStrategy {

    private WorldlineConfigurationService worldlineConfigurationService;

    private static final Logger LOG = LoggerFactory.getLogger(WorldlinePaymentProductSortStrategy.class);

    @Override
    public List<PaymentProduct> filter(List<PaymentProduct> paymentProducts) {
        WorldlineConfigurationModel configuration = worldlineConfigurationService.getCurrentWorldlineConfiguration();
        List<PaymentModeModel> paymentModes = configuration.getPaymentModeSort();
        if(paymentModes == null || paymentModes.isEmpty()) {
            return paymentProducts;
        }
        Map<Integer, PaymentProduct> productMap = paymentProducts.stream()
                .collect(Collectors.toMap(
                        PaymentProduct::getId,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));
        int counter = 0;
        List<PaymentProduct> output = new ArrayList<>();
        for(PaymentModeModel paymentMode : paymentModes) {
            Integer paymentModeCode;
            try {
                paymentModeCode = Integer.valueOf(paymentMode.getCode());
            } catch (NumberFormatException e) {
                LOG.warn("Payment Mode {} does not use a number as its code so must not be Worldline. Skipping...", paymentMode.getCode());
                continue;
            }

            if (productMap.containsKey(paymentModeCode)) {
                PaymentProduct paymentProduct = productMap.get(paymentModeCode);
                paymentProduct.getDisplayHints().setDisplayOrder(counter);
                counter++;
                output.add(paymentProduct);
            }
        }

        return output;
    }

    @Required
    public void setWorldlineConfigurationService(WorldlineConfigurationService worldlineConfigurationService) {
        this.worldlineConfigurationService = worldlineConfigurationService;
    }
}