package com.worldline.direct.service;

import de.hybris.platform.core.model.order.payment.PaymentModeModel;
import de.hybris.platform.order.PaymentModeService;

import java.util.List;

public interface WorldlinePaymentModeService extends PaymentModeService {
    List<PaymentModeModel>  getActivePaymentModes();
}
