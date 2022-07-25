package com.worldline.direct.service;

import com.onlinepayments.domain.PaymentResponse;
import de.hybris.platform.orderscheduling.model.CartToOrderCronJobModel;

public interface WorldlineCartToOrderService {
    void enableCartToOrderJob(CartToOrderCronJobModel cronJobModel, PaymentResponse paymentID);

    void cancelCartToOrderJob(CartToOrderCronJobModel cronJobModel);
}
