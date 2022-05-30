package com.worldline.direct.factory;

import com.ingenico.direct.domain.ShoppingCart;
import de.hybris.platform.core.model.order.AbstractOrderModel;

public interface WorldlineShoppingCartFactory {
    ShoppingCart create(AbstractOrderModel abstractOrderModel);
}
