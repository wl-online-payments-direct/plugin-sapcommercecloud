package com.worldline.direct.factory.impl;

import com.ingenico.direct.domain.AmountOfMoney;
import com.ingenico.direct.domain.LineItem;
import com.ingenico.direct.domain.OrderLineDetails;
import com.ingenico.direct.domain.ShoppingCart;
import com.worldline.direct.factory.WorldlineShoppingCartFactory;
import com.worldline.direct.util.WorldlineAmountUtils;
import com.worldline.direct.util.WorldlinePaymentProductUtils;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import org.springframework.beans.factory.annotation.Required;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WorldlineShoppingCartWithoutDiscountFactory implements WorldlineShoppingCartFactory {
    private WorldlineAmountUtils worldlineAmountUtils;
    private WorldlinePaymentProductUtils worldlinePaymentProductUtils;

    @Override
    public ShoppingCart create(AbstractOrderModel abstractOrderModel) {

        String currencyISOCode = abstractOrderModel.getCurrency().getIsocode();
        ShoppingCart cart = new ShoppingCart();

        List<LineItem> lineItems = new ArrayList<>();
        for (AbstractOrderEntryModel orderEntry : abstractOrderModel.getEntries().stream().filter(abstractOrderEntryModel -> abstractOrderEntryModel.getTotalPrice() > 0).collect(Collectors.toList())) {

            LineItem item = new LineItem();
            AmountOfMoney itemAmountOfMoney = new AmountOfMoney();
            itemAmountOfMoney.setAmount(worldlineAmountUtils.createAmount(orderEntry.getTotalPrice(), currencyISOCode));
            itemAmountOfMoney.setCurrencyCode(currencyISOCode);
            item.setAmountOfMoney(itemAmountOfMoney);

            item.setOrderLineDetails(createOrderLineDetails(orderEntry, currencyISOCode));
            lineItems.add(item);
        }
        if (!worldlinePaymentProductUtils.isPaymentByKlarna(((WorldlinePaymentInfoModel)abstractOrderModel.getPaymentInfo()).getId())) {
            lineItems.add(setShippingAsProduct(abstractOrderModel, currencyISOCode));
        }
        cart.setItems(lineItems);

        return cart;
    }

    private OrderLineDetails createOrderLineDetails(AbstractOrderEntryModel orderEntry, String currencyISOcode) {
        OrderLineDetails orderLineDetails = new OrderLineDetails();
        orderLineDetails.setProductName(orderEntry.getProduct().getName());
        orderLineDetails.setProductCode(orderEntry.getProduct().getCode());
        orderLineDetails.setTaxAmount(0L);
        BigDecimal basePrice = BigDecimal.valueOf(worldlineAmountUtils.createAmount(orderEntry.getBasePrice(), currencyISOcode));
        BigDecimal totalPrice = BigDecimal.valueOf(worldlineAmountUtils.createAmount(orderEntry.getTotalPrice(), currencyISOcode));
        orderLineDetails.setQuantity(orderEntry.getQuantity());
        orderLineDetails.setDiscountAmount(basePrice.subtract(totalPrice).longValue());
        orderLineDetails.setProductPrice(basePrice.longValue());
        return orderLineDetails;
    }

    private LineItem setShippingAsProduct(AbstractOrderModel abstractOrderModel, String currencyISOCode) {
        LineItem shipping = new LineItem();
        AmountOfMoney itemAmountOfMoney = new AmountOfMoney();
        itemAmountOfMoney.setAmount(worldlineAmountUtils.createAmount(abstractOrderModel.getDeliveryCost(), currencyISOCode));
        itemAmountOfMoney.setCurrencyCode(currencyISOCode);
        shipping.setAmountOfMoney(itemAmountOfMoney);

        OrderLineDetails orderLineDetails = new OrderLineDetails();
        orderLineDetails.setProductName(abstractOrderModel.getDeliveryMode().getName());
        orderLineDetails.setProductCode(abstractOrderModel.getDeliveryMode().getName());
        orderLineDetails.setQuantity(1L);
        orderLineDetails.setTaxAmount(0L);
        orderLineDetails.setProductPrice(worldlineAmountUtils.createAmount(abstractOrderModel.getDeliveryCost(), currencyISOCode));

        shipping.setOrderLineDetails(orderLineDetails);
        return shipping;
    }

    @Required
    public void setWorldlineAmountUtils(WorldlineAmountUtils worldlineAmountUtils) {
        this.worldlineAmountUtils = worldlineAmountUtils;
    }

    @Required
    public void setWorldlinePaymentProductUtils(WorldlinePaymentProductUtils worldlinePaymentProductUtils) {
        this.worldlinePaymentProductUtils = worldlinePaymentProductUtils;
    }

}
