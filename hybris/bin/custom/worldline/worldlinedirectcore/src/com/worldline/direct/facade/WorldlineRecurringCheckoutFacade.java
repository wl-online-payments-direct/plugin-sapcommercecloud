package com.worldline.direct.facade;

import com.onlinepayments.domain.CreateHostedCheckoutResponse;
import com.onlinepayments.domain.CreatePaymentResponse;
import com.worldline.direct.enums.RecurringPaymentEnum;
import com.worldline.direct.exception.WorldlineNonAuthorizedPaymentException;
import com.worldline.direct.order.data.BrowserData;
import com.worldline.direct.order.data.WorldlineHostedTokenizationData;
import de.hybris.platform.b2bacceleratorfacades.order.data.ScheduledCartData;
import de.hybris.platform.commercefacades.order.data.AbstractOrderData;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.orderscheduling.model.CartToOrderCronJobModel;

public interface WorldlineRecurringCheckoutFacade extends WorldlineCheckoutFacade {

    CreateHostedCheckoutResponse createReplenishmentHostedCheckout(AbstractOrderData abstractOrderData, BrowserData browserData, RecurringPaymentEnum scheduled) throws InvalidCartException;

    ScheduledCartData authorisePaymentForSchudledReplenishmentHostedCheckout(String jobCode, String hostedCheckoutId) throws WorldlineNonAuthorizedPaymentException, InvalidCartException;

    ScheduledCartData authorisePaymentForImmediateReplenishmentHostedCheckout(String orderCode, String hostedCheckoutId) throws WorldlineNonAuthorizedPaymentException, InvalidCartException;

    ScheduledCartData authorizeRecurringPaymentForHostedTokenization(
          String code, WorldlineHostedTokenizationData worldlineHostedTokenizationData, RecurringPaymentEnum recurringPaymentType) throws WorldlineNonAuthorizedPaymentException, InvalidCartException;

}
