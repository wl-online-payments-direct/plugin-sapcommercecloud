package com.worldline.gopay.strategy.impl;

import com.onlinepayments.domain.PaymentProduct;
import com.worldline.gopay.constants.WorldlinegopaycoreConstants;
import com.worldline.gopay.service.WorldlinePaymentModeService;
import com.worldline.gopay.strategy.WorldlinePaymentProductFilterStrategy;
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
                .filter(paymentProduct -> activePaymentModeCodes.contains(String.valueOf(paymentProduct.getId())) || WorldlinegopaycoreConstants.PAYMENT_METHOD_HTP == paymentProduct.getId() || WorldlinegopaycoreConstants.PAYMENT_METHOD_HCP == paymentProduct.getId()).collect(Collectors.toList());
    }

    @Required
    public void setWorldlinePaymentModeService(WorldlinePaymentModeService worldlinePaymentModeService) {
        this.worldlinePaymentModeService = worldlinePaymentModeService;
    }
}
