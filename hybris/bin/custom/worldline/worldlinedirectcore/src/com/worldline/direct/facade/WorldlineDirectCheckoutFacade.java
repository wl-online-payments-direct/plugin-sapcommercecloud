package com.worldline.direct.facade;

import de.hybris.platform.b2bacceleratorfacades.checkout.data.PlaceOrderData;
import de.hybris.platform.b2bacceleratorfacades.order.data.B2BDaysOfWeekData;
import de.hybris.platform.commercefacades.order.CheckoutFacade;
import de.hybris.platform.commercefacades.order.data.AbstractOrderData;
import de.hybris.platform.order.InvalidCartException;

import java.util.List;


public interface WorldlineDirectCheckoutFacade extends CheckoutFacade {
    <T extends AbstractOrderData> T placeOrder(PlaceOrderData placeOrderData) throws InvalidCartException;

    List<B2BDaysOfWeekData> getDaysOfWeekForReplenishmentCheckoutSummary();
}
