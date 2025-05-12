package com.worldline.direct.factory.impl;

import com.onlinepayments.domain.*;
import com.worldline.direct.factory.WorldlineShoppingCartFactory;
import com.worldline.direct.populator.WorldlineOrderRequestParamPopulator;
import com.worldline.direct.util.WorldlineAmountUtils;
import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.price.DiscountModel;
import de.hybris.platform.util.DiscountValue;
import org.springframework.beans.factory.annotation.Required;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DefaultWorldlineShoppingCartFactory implements WorldlineShoppingCartFactory {
    private WorldlineAmountUtils worldlineAmountUtils;

    @Override
    public ShoppingCart create(AbstractOrderModel abstractOrderModel) {
        String currencyISOCode = abstractOrderModel.getCurrency().getIsocode();
        ShoppingCart cart = new ShoppingCart();

        List<LineItem> lineItems = new ArrayList<>();
        for (AbstractOrderEntryModel orderEntry : abstractOrderModel.getEntries()) {
            LineItem item = new LineItem();
            AmountOfMoney itemAmountOfMoney = new AmountOfMoney();
            itemAmountOfMoney.setAmount(worldlineAmountUtils.createAmount(orderEntry.getTotalPrice(), currencyISOCode));
            itemAmountOfMoney.setCurrencyCode(currencyISOCode);
            item.setAmountOfMoney(itemAmountOfMoney);
            item.setOrderLineDetails(createOrderLineDetails(orderEntry, currencyISOCode));
            lineItems.add(item);
        }

        cart.setItems(lineItems);
        return cart;
    }

    private long calculateTotalLineLevelDiscountAmount(AbstractOrderEntryModel orderEntry, String currencyISOCode) {
        long totalDiscount = 0L;
        for(DiscountValue discountValue : orderEntry.getDiscountValues()) {
            totalDiscount += worldlineAmountUtils.createAmount(discountValue.getAppliedValue(), currencyISOCode);
        }
        return totalDiscount;
    }

    private OrderLineDetails createOrderLineDetails(AbstractOrderEntryModel orderEntry, String currencyISOCode) {
        OrderLineDetails orderLineDetails = new OrderLineDetails();
        orderLineDetails.setProductName(orderEntry.getProduct().getName());
        orderLineDetails.setProductCode(orderEntry.getProduct().getCode());
        orderLineDetails.setTaxAmount(0L);
        BigDecimal basePrice = BigDecimal.valueOf(worldlineAmountUtils.createAmount(orderEntry.getBasePrice(), currencyISOCode));
        orderLineDetails.setQuantity(orderEntry.getQuantity());
        orderLineDetails.setDiscountAmount(calculateTotalLineLevelDiscountAmount(orderEntry, currencyISOCode));
        orderLineDetails.setProductPrice(basePrice.longValue());
        return orderLineDetails;
    }

    @Required
    public void setWorldlineAmountUtils(WorldlineAmountUtils worldlineAmountUtils) {
        this.worldlineAmountUtils = worldlineAmountUtils;
    }

}
