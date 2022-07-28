package com.worldline.direct.strategy.impl;

import com.onlinepayments.domain.PaymentProduct;
import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import com.worldline.direct.service.WorldlinePaymentModeService;
import com.worldline.direct.strategy.WorldlinePaymentProductFilterStrategy;
import de.hybris.platform.core.model.order.payment.PaymentModeModel;
import org.springframework.beans.factory.annotation.Required;

import java.util.List;
import java.util.stream.Collectors;

public class WorldlinePaymentProductFilterByAvailabilityStrategy implements WorldlinePaymentProductFilterStrategy {
    private WorldlinePaymentModeService worldlinePaymentModeService;

    @Override
    public List<PaymentProduct> filter(List<PaymentProduct> paymentProducts) {
        List<String> activePaymentModeCodes = worldlinePaymentModeService.getActivePaymentModes().stream().map(PaymentModeModel::getCode).collect(Collectors.toList());
        return paymentProducts.stream()
                .filter(paymentProduct -> activePaymentModeCodes.contains(String.valueOf(paymentProduct.getId())) || WorldlinedirectcoreConstants.PAYMENT_METHOD_HTP == paymentProduct.getId() || WorldlinedirectcoreConstants.PAYMENT_METHOD_HCP == paymentProduct.getId()).collect(Collectors.toList());
    }

    @Required
    public void setWorldlinePaymentModeService(WorldlinePaymentModeService worldlinePaymentModeService) {
        this.worldlinePaymentModeService = worldlinePaymentModeService;
    }
}
