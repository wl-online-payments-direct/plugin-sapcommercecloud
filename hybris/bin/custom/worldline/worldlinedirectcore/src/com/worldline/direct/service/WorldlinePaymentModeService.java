package com.worldline.direct.service;

import com.onlinepayments.domain.PaymentProduct;
import de.hybris.platform.core.model.order.payment.PaymentModeModel;
import de.hybris.platform.order.PaymentModeService;

import java.util.List;

public interface WorldlinePaymentModeService extends PaymentModeService {
    List<PaymentModeModel>  getActivePaymentModes();

    boolean isIntersolve(String paymentModeId);

    boolean isSaleOnly(String paymentModeId);

    PaymentModeModel getPaymentModeForPaymentProduct(PaymentProduct paymentProduct);
}
