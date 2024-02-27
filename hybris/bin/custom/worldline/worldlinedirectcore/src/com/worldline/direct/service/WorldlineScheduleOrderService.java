package com.worldline.direct.service;

import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.payment.PaymentInfoModel;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.cronjob.model.TriggerModel;
import de.hybris.platform.orderscheduling.model.CartToOrderCronJobModel;

import java.util.List;

public interface WorldlineScheduleOrderService {
    CartToOrderCronJobModel createOrderFromCartCronJob(CartModel cart, AddressModel deliveryAddress, AddressModel paymentAddress, PaymentInfoModel paymentInfo, List<TriggerModel> triggers);

    void updateCartRecurringPaymentInfo(CartModel cartModel, boolean tokenizePayment);
}
