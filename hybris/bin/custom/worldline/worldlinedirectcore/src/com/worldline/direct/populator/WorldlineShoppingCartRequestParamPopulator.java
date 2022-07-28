package com.worldline.direct.populator;

import com.onlinepayments.domain.Order;
import com.onlinepayments.domain.ShoppingCart;
import com.worldline.direct.factory.impl.WorldlineShoppingCartFactoriesConfiguration;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import org.springframework.beans.factory.annotation.Required;

public class WorldlineShoppingCartRequestParamPopulator implements Populator<AbstractOrderModel, Order> {

    private WorldlineShoppingCartFactoriesConfiguration worldlineShoppingCartFactoriesConfiguration;

    @Override
    public void populate(AbstractOrderModel abstractOrderModel, Order order) throws ConversionException {
        if (abstractOrderModel instanceof OrderModel || (abstractOrderModel instanceof CartModel && ((CartModel) abstractOrderModel).getReplenishmentOrderProcess() != null)) {
            ShoppingCart shoppingCart = worldlineShoppingCartFactoriesConfiguration.getShoppingCartFactory(abstractOrderModel).create(abstractOrderModel);
            order.setShoppingCart(shoppingCart);
        }
    }

    @Required
    public void setWorldlineShoppingCartFactoriesConfiguration(WorldlineShoppingCartFactoriesConfiguration worldlineShoppingCartFactoriesConfiguration) {
        this.worldlineShoppingCartFactoriesConfiguration = worldlineShoppingCartFactoriesConfiguration;
    }
}
