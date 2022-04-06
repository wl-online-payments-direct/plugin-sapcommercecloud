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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class WorldlineShoppingCartWithDiscountRequestParamPopulator implements Populator<AbstractOrderModel, Order> {

    private static final int REMAINDER = 1;
    private static final int AMOUNT = 0;
    private WorldlineConfigurationService worldlineConfigurationService;
    private WorldlineAmountUtils worldlineAmountUtils;

    @Override
    public void populate(AbstractOrderModel abstractOrderModel, Order order) throws ConversionException {
        if (BooleanUtils.isTrue(worldlineConfigurationService.getCurrentWorldlineConfiguration().getSubmitOrderPromotion()) && abstractOrderModel.getTotalDiscounts() > 0) {
            order.setShoppingCart(createShoppingCart(abstractOrderModel));
        }
    }

    private ShoppingCart createShoppingCart(AbstractOrderModel abstractOrderModel) {
        String currencyISOCode = abstractOrderModel.getCurrency().getIsocode();
        int entriesCount = abstractOrderModel.getEntries().size();
        long subtotal = worldlineAmountUtils.createAmount(abstractOrderModel.getSubtotal() - abstractOrderModel.getTotalDiscounts(), currencyISOCode);
        List<BigDecimal> itemAmounts = getItemAmounts(BigDecimal.valueOf(subtotal), BigDecimal.valueOf(entriesCount));
        ShoppingCart cart = new ShoppingCart();
        List<LineItem> lineItems = new ArrayList<>();
        int i = 0;
        for (AbstractOrderEntryModel orderEntry : abstractOrderModel.getEntries()) {

            List<LineItem> items = createLineItem(currencyISOCode, orderEntry, itemAmounts.get(i++));
            lineItems.addAll(items);
        }

        //workaround for shipping taxes
        lineItems.add(setShippingAsProduct(abstractOrderModel, currencyISOCode));

        cart.setItems(lineItems);

        return cart;
    }

    private List<LineItem> createLineItem(String currencyISOCode, AbstractOrderEntryModel orderEntry, BigDecimal itemPrice) {
        List<BigDecimal> itemAmounts = getItemAmounts(itemPrice, BigDecimal.valueOf(orderEntry.getQuantity()));
        Map<BigDecimal, Long> countingByAmount = itemAmounts.stream().collect(Collectors.groupingBy(i -> i, Collectors.counting()));
        List<LineItem> lineItems = new ArrayList<>();
        countingByAmount.entrySet().stream().forEach(entry -> {
            LineItem item = new LineItem();
            AmountOfMoney itemAmountOfMoney = new AmountOfMoney();
            itemAmountOfMoney.setAmount(entry.getKey().multiply(BigDecimal.valueOf(entry.getValue())).longValue());
            itemAmountOfMoney.setCurrencyCode(currencyISOCode);
            item.setAmountOfMoney(itemAmountOfMoney);
            item.setOrderLineDetails(createOrderLineDetails(orderEntry.getProduct().getName(), entry.getKey().longValue(), entry.getValue().longValue()));
            lineItems.add(item);
        });
        return lineItems;
    }

    List<BigDecimal> getItemAmounts(BigDecimal totalAmount, BigDecimal totalPurchaseItems) {
        List<BigDecimal> amountAndReminder = Arrays.stream(totalAmount.divideAndRemainder(totalPurchaseItems)).collect(Collectors.toList());
        List<BigDecimal> itemAmounts = new ArrayList<>();
        if (BigDecimal.ZERO.equals(amountAndReminder.get(REMAINDER))) {
            IntStream.rangeClosed(1, totalPurchaseItems.intValue()).forEach(i -> {
                itemAmounts.add(amountAndReminder.get(AMOUNT));
            });

            return itemAmounts;
        } else {
            BigDecimal itemAmount = totalAmount.divide(totalPurchaseItems, RoundingMode.UP);
            itemAmounts.add(itemAmount);
            itemAmounts.addAll(getItemAmounts(totalAmount.subtract(itemAmount), totalPurchaseItems.subtract(BigDecimal.ONE)));
        }
        return itemAmounts;
    }

    private LineItem setShippingAsProduct(AbstractOrderModel abstractOrderModel, String currencyISOCode) {
        LineItem shipping = new LineItem();
        AmountOfMoney itemAmountOfMoney = new AmountOfMoney();
        itemAmountOfMoney.setAmount(worldlineAmountUtils.createAmount(abstractOrderModel.getDeliveryCost(), currencyISOCode));
        itemAmountOfMoney.setCurrencyCode(currencyISOCode);
        shipping.setAmountOfMoney(itemAmountOfMoney);

        OrderLineDetails orderLineDetails = new OrderLineDetails();
        orderLineDetails.setProductName(abstractOrderModel.getDeliveryMode().getName());
        orderLineDetails.setQuantity(1L);
        orderLineDetails.setProductPrice(worldlineAmountUtils.createAmount(abstractOrderModel.getDeliveryCost(), currencyISOCode));

        shipping.setOrderLineDetails(orderLineDetails);
        return shipping;
    }

    private OrderLineDetails createOrderLineDetails(String productName, long productPrice, long quantity) {
        OrderLineDetails orderLineDetails = new OrderLineDetails();
        orderLineDetails.setProductName(productName);
        orderLineDetails.setQuantity(quantity);
        orderLineDetails.setProductPrice(productPrice);
        return orderLineDetails;
    }

    public void setWorldlineConfigurationService(WorldlineConfigurationService worldlineConfigurationService) {
        this.worldlineConfigurationService = worldlineConfigurationService;
    }

    public void setWorldlineAmountUtils(WorldlineAmountUtils worldlineAmountUtils) {
        this.worldlineAmountUtils = worldlineAmountUtils;
    }
}
