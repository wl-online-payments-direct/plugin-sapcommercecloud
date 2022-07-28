package com.worldline.direct.event;

import de.hybris.platform.basecommerce.model.site.BaseSiteModel;
import de.hybris.platform.commerceservices.event.AbstractCommerceUserEvent;
import de.hybris.platform.orderscheduling.model.CartToOrderCronJobModel;


public class WorldlineReplenishmentOrderPlacedEvent extends AbstractCommerceUserEvent<BaseSiteModel> {
    private final CartToOrderCronJobModel cartToOrder;

    public WorldlineReplenishmentOrderPlacedEvent(final CartToOrderCronJobModel cartToOrder) {
        this.cartToOrder = cartToOrder;
    }

    public CartToOrderCronJobModel getCartToOrderCronJob() {
        return cartToOrder;
    }
}
