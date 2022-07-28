package com.worldline.direct.service;

import com.onlinepayments.domain.CreatePaymentResponse;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.orderscheduling.model.CartToOrderCronJobModel;

import java.util.Optional;

public interface WorldlineRecurringService {
    Optional<CreatePaymentResponse> createRecurringPayment(AbstractOrderModel abstractOrderModel);

    void cancelRecurringPayment(CartToOrderCronJobModel cronJobModel);
}
