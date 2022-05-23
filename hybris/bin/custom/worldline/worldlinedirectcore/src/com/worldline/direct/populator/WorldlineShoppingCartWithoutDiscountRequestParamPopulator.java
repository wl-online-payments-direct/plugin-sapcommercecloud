package com.worldline.direct.populator;

import com.ingenico.direct.domain.*;
import com.worldline.direct.service.WorldlineConfigurationService;
import com.worldline.direct.util.WorldlineAmountUtils;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import org.apache.commons.lang3.BooleanUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class WorldlineShoppingCartWithoutDiscountRequestParamPopulator implements Populator<AbstractOrderModel, Order> {

    private WorldlineConfigurationService worldlineConfigurationService;
    private WorldlineAmountUtils worldlineAmountUtils;

    @Override
    public void populate(AbstractOrderModel abstractOrderModel, Order order) throws ConversionException {
        if (BooleanUtils.isFalse(worldlineConfigurationService.getCurrentWorldlineConfiguration().getSubmitOrderPromotion()) || abstractOrderModel.getTotalDiscounts() == 0) {
            order.setShoppingCart(createShoppingCart(abstractOrderModel));
        }
    }

    private ShoppingCart createShoppingCart(AbstractOrderModel abstractOrderModel) {
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

        //workaround for shipping taxes
        lineItems.add(setShippingAsProduct(abstractOrderModel, currencyISOCode));

        cart.setItems(lineItems);

        return cart;
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

    private OrderLineDetails createOrderLineDetails(AbstractOrderEntryModel orderEntry, String currencyISOcode) {
        OrderLineDetails orderLineDetails = new OrderLineDetails();
        orderLineDetails.setProductName(orderEntry.getProduct().getName());
        orderLineDetails.setProductCode(orderEntry.getProduct().getCode());
        orderLineDetails.setTaxAmount(0L);
        orderLineDetails.setQuantity(orderEntry.getQuantity());
        orderLineDetails.setQuantity(orderEntry.getQuantity());
        orderLineDetails.setProductPrice(BigDecimal.valueOf(worldlineAmountUtils.createAmount(orderEntry.getTotalPrice(), currencyISOcode)).divide(BigDecimal.valueOf(orderEntry.getQuantity())).longValue());
        return orderLineDetails;
    }

    public void setWorldlineAmountUtils(WorldlineAmountUtils worldlineAmountUtils) {
        this.worldlineAmountUtils = worldlineAmountUtils;
    }


    public void setWorldlineConfigurationService(WorldlineConfigurationService worldlineConfigurationService) {
        this.worldlineConfigurationService = worldlineConfigurationService;
    }
}
