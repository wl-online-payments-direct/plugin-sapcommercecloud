package com.worldline.direct.service;

import com.onlinepayments.domain.CreateHostedCheckoutResponse;
import com.onlinepayments.domain.CreatePaymentResponse;
import com.worldline.direct.exception.WorldlineNonAuthorizedPaymentException;
import com.worldline.direct.order.data.BrowserData;
import com.worldline.direct.order.data.WorldlineHostedTokenizationData;
import de.hybris.platform.orderscheduling.model.CartToOrderCronJobModel;

public interface WorldlineB2BPaymentService {
    CreateHostedCheckoutResponse createRecurringHostedCheckout(CartToOrderCronJobModel cartToOrderCronJobModel, BrowserData browserData);

    CreatePaymentResponse createRecurringPaymentForHostedTokenization(CartToOrderCronJobModel cartToOrderCronJob, WorldlineHostedTokenizationData worldlineHostedTokenizationData) throws WorldlineNonAuthorizedPaymentException;
}
