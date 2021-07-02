package com.ingenico.ogone.direct.facade.impl;

import de.hybris.platform.commercefacades.order.impl.DefaultCheckoutFacade;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.OrderModel;

public class IngenicoExtendedCheckoutFacadeImpl extends DefaultCheckoutFacade {

    @Override
    protected void afterPlaceOrder(@SuppressWarnings("unused") final CartModel cartModel, final OrderModel orderModel) //NOSONAR
    {
        if (orderModel != null) {
            getModelService().refresh(orderModel);
        }
    }
}
