package com.worldline.direct.service;

import com.onlinepayments.domain.CreatePaymentResponse;
import com.worldline.direct.enums.WorldlineRecurringPaymentStatus;
import com.worldline.direct.model.WorldlineMandateModel;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.orderscheduling.model.CartToOrderCronJobModel;

import java.util.Optional;

public interface WorldlineRecurringService {
    Optional<CreatePaymentResponse> createRecurringPayment(AbstractOrderModel abstractOrderModel) throws Exception;

    void cancelRecurringPayment(CartToOrderCronJobModel cronJobModel);

    void updateMandate(WorldlineMandateModel mandateModel);

    WorldlineRecurringPaymentStatus isRecurringPaymentEnable(CartToOrderCronJobModel cartToOrderCronJobModel);

    void blockRecurringPayment(AbstractOrderModel abstractOrderModel);
}
