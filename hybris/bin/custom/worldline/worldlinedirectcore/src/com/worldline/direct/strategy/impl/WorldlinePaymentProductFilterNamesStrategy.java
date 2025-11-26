package com.worldline.direct.strategy.impl;

import com.onlinepayments.domain.PaymentProduct;
import com.worldline.direct.service.WorldlinePaymentModeService;
import com.worldline.direct.strategy.WorldlinePaymentProductFilterStrategy;
import de.hybris.platform.core.model.order.payment.PaymentModeModel;
import org.springframework.beans.factory.annotation.Required;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class WorldlinePaymentProductFilterNamesStrategy implements WorldlinePaymentProductFilterStrategy {
    private WorldlinePaymentModeService worldlinePaymentModeService;

    @Override
    public List<PaymentProduct> filter(List<PaymentProduct> paymentProducts) {
        List<PaymentModeModel> paymentModes = worldlinePaymentModeService.getActivePaymentModes();
        Map<String, PaymentModeModel> modeMap = paymentModes.stream()
                .collect(Collectors.toMap(
                        PaymentModeModel::getCode,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        paymentProducts.forEach(paymentProduct -> {
            String key = String.valueOf(paymentProduct.getId());
            if (modeMap.containsKey(key)) {
                PaymentModeModel matchedPaymentMode = modeMap.get(key);
                if(Boolean.TRUE.equals(matchedPaymentMode.getOverrideName())) {
                    paymentProduct.getDisplayHints().setLabel(matchedPaymentMode.getName());
                }
            }
        });
        return paymentProducts;
    }

    @Required
    public void setWorldlinePaymentModeService(WorldlinePaymentModeService worldlinePaymentModeService) {
        this.worldlinePaymentModeService = worldlinePaymentModeService;
    }
}
