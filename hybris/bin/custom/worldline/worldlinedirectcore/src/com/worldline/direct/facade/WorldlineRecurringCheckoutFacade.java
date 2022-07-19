package com.worldline.direct.facade;

import com.onlinepayments.domain.CreateHostedCheckoutResponse;
import com.worldline.direct.exception.WorldlineNonAuthorizedPaymentException;
import com.worldline.direct.order.data.BrowserData;
import de.hybris.platform.b2bacceleratorfacades.order.data.ScheduledCartData;
import de.hybris.platform.order.InvalidCartException;

public interface WorldlineRecurringCheckoutFacade extends WorldlineCheckoutFacade {

    CreateHostedCheckoutResponse createReplenishmentHostedCheckout(String jobCode, BrowserData browserData) throws InvalidCartException;

    ScheduledCartData authorisePaymentForReplenishmentHostedCheckout(String jobCode, String hostedCheckoutId) throws WorldlineNonAuthorizedPaymentException, InvalidCartException;
}
