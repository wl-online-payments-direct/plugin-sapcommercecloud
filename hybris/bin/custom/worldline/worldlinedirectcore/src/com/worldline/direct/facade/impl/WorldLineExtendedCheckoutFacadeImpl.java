package com.worldline.direct.facade.impl;

import de.hybris.platform.commercefacades.order.impl.DefaultCheckoutFacade;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.servicelayer.keygenerator.KeyGenerator;

public class WorldLineExtendedCheckoutFacadeImpl extends DefaultCheckoutFacade {

    private KeyGenerator guidKeyGenerator;

    @Override
    protected void afterPlaceOrder(@SuppressWarnings("unused") final CartModel cartModel, final OrderModel orderModel) //NOSONAR
    {
        if (orderModel != null) {
            orderModel.setGuid(guidKeyGenerator.generate().toString());
            getModelService().save(orderModel);
            getModelService().refresh(orderModel);
        }
    }

    public void setGuidKeyGenerator(KeyGenerator guidKeyGenerator) {
        this.guidKeyGenerator = guidKeyGenerator;
    }
}
