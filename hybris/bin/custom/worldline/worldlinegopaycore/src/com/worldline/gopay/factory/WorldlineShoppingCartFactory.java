package com.worldline.gopay.factory;

import com.onlinepayments.domain.ShoppingCart;
import de.hybris.platform.core.model.order.AbstractOrderModel;

public interface WorldlineShoppingCartFactory {
    ShoppingCart create(AbstractOrderModel abstractOrderModel);
}
