package com.worldline.direct.factory.impl;

import com.onlinepayments.domain.AmountOfMoney;
import com.onlinepayments.domain.LineItem;
import com.onlinepayments.domain.OrderLineDetails;
import com.onlinepayments.domain.ShoppingCart;
import com.worldline.direct.factory.WorldlineShoppingCartFactory;
import com.worldline.direct.util.WorldlineAmountUtils;
import com.worldline.direct.util.WorldlinePaymentProductUtils;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import org.springframework.beans.factory.annotation.Required;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class WorldlineShoppingCartWithDiscountFactory implements WorldlineShoppingCartFactory {

    private static final String ENTRY_PRICE = "entryPrice";
    private static final String PRICE_AFTER_DISCOUNT = "priceAfterDiscount";
    private WorldlineAmountUtils worldlineAmountUtils;

    @Override
    public ShoppingCart create(AbstractOrderModel abstractOrderModel) {
        String currencyISOCode = abstractOrderModel.getCurrency().getIsocode();
        BigDecimal orderPriceWithDiscount = BigDecimal.valueOf(worldlineAmountUtils.createAmount(abstractOrderModel.getTotalPrice() - abstractOrderModel.getDeliveryCost(), currencyISOCode));
        BigDecimal orderPriceWithoutDiscount = orderPriceWithDiscount.add(BigDecimal.valueOf(worldlineAmountUtils.createAmount(abstractOrderModel.getTotalDiscounts(), currencyISOCode)));
        Map<AbstractOrderEntryModel, List<Map<String, BigDecimal>>> orderEntriesToPricesMap = calculatePromotions(abstractOrderModel, currencyISOCode, orderPriceWithDiscount, orderPriceWithoutDiscount);
        ShoppingCart cart = new ShoppingCart();
        List<LineItem> lineItems = new ArrayList();
        for (AbstractOrderEntryModel orderEntry : abstractOrderModel.getEntries().stream().filter(abstractOrderEntryModel -> abstractOrderEntryModel.getTotalPrice() > 0).collect(Collectors.toList())) {
            lineItems.addAll(createSplittedLineItem(currencyISOCode, orderEntriesToPricesMap.get(orderEntry), orderEntry));
        }
        if (!WorldlinePaymentProductUtils.isPaymentByKlarna(((WorldlinePaymentInfoModel) abstractOrderModel.getPaymentInfo()))) {
            lineItems.add(setShippingAsProduct(abstractOrderModel, currencyISOCode));
        }

        cart.setItems(lineItems);

        return cart;


    }

    private List<LineItem> createSplittedLineItem(String currencyISOCode, List<Map<String, BigDecimal>> entryPrices, AbstractOrderEntryModel orderEntry) {
        List<LineItem> lineItems = new ArrayList<>();
        List<BigDecimal> itemAmounts = getItemAmounts(entryPrices);
        Map<BigDecimal, Long> countingByAmount = itemAmounts.stream().collect(Collectors.groupingBy(i -> i, Collectors.counting()));
        countingByAmount.entrySet().stream().forEach(entry -> {

            LineItem item = new LineItem();
            AmountOfMoney itemAmountOfMoney = new AmountOfMoney();
            BigDecimal amount = entry.getKey();
            Long quantity = entry.getValue();
            itemAmountOfMoney.setAmount(amount.multiply(BigDecimal.valueOf(quantity)).longValue());
            itemAmountOfMoney.setCurrencyCode(currencyISOCode);
            item.setAmountOfMoney(itemAmountOfMoney);
            item.setOrderLineDetails(createSplitedOrderLineDetails(orderEntry, amount, quantity));
            lineItems.add(item);
        });
        return lineItems;
    }

    private Map<AbstractOrderEntryModel, List<Map<String, BigDecimal>>> calculatePromotions(AbstractOrderModel abstractOrderModel, String currencyISOCode, BigDecimal orderPriceWithDiscount, BigDecimal orderPriceWithoutDiscount) {
        Map<AbstractOrderEntryModel, List<Map<String, BigDecimal>>> orderEntryToPricesMap = new LinkedHashMap();
        abstractOrderModel.getEntries().stream()
                .filter(abstractOrderEntryModel -> abstractOrderEntryModel.getTotalPrice() > 0)
                .sorted(Comparator.comparing(AbstractOrderEntryModel::getTotalPrice))
                .forEach(entry -> {
                    List<Map<String, BigDecimal>> entryPriceList = new ArrayList<>();
                    for (int i = 0; i < entry.getQuantity(); i++) {
                        Map<String, BigDecimal> entryPrices = new HashMap<>();
                        entryPrices.put(ENTRY_PRICE, BigDecimal.valueOf(worldlineAmountUtils.createAmount(entry.getTotalPrice(), currencyISOCode)).divide(BigDecimal.valueOf(entry.getQuantity())));
                        entryPriceList.add(entryPrices);
                    }
                    orderEntryToPricesMap.put(entry, entryPriceList);
                });

        BigDecimal splitDiscountSum = BigDecimal.ZERO;
        for (List<Map<String, BigDecimal>> entryPriceList : orderEntryToPricesMap.values()) {
            for (Map<String, BigDecimal> entryPrice : entryPriceList) {
                BigDecimal priceAfterDiscount = orderPriceWithDiscount.multiply(entryPrice.get(ENTRY_PRICE)).divideToIntegralValue(orderPriceWithoutDiscount);
                entryPrice.put(PRICE_AFTER_DISCOUNT, priceAfterDiscount);
                splitDiscountSum = splitDiscountSum.add(priceAfterDiscount);

            }
        }
        BigDecimal reminderDiscount = orderPriceWithDiscount.subtract(splitDiscountSum);
        while (!reminderDiscount.equals(BigDecimal.ZERO) && orderEntryToPricesMap.values().stream().anyMatch(entryPriceList->entryPriceList.stream().anyMatch(this::canBeDiscountedByOne))) {
            for (List<Map<String, BigDecimal>> entryPriceList : orderEntryToPricesMap.values()) {
                for (Map<String, BigDecimal> entryPrice : entryPriceList.stream().filter(this::canBeDiscountedByOne).collect(Collectors.toList())) {
                    if (!reminderDiscount.equals(BigDecimal.ZERO)) {
                        entryPrice.put(PRICE_AFTER_DISCOUNT, (entryPrice.get(PRICE_AFTER_DISCOUNT).add(BigDecimal.ONE)));
                        reminderDiscount = reminderDiscount.subtract(BigDecimal.ONE);
                    }
                }
            }
        }
        return orderEntryToPricesMap;
    }


    private OrderLineDetails createSplitedOrderLineDetails(AbstractOrderEntryModel orderEntry, BigDecimal priceAfterDiscount, Long quantity) {
        OrderLineDetails orderLineDetails = new OrderLineDetails();
        orderLineDetails.setProductName(orderEntry.getProduct().getName());
        orderLineDetails.setProductCode(orderEntry.getProduct().getCode());
        orderLineDetails.setTaxAmount(0L);
        orderLineDetails.setQuantity(quantity);
        BigDecimal basePrice = BigDecimal.valueOf(worldlineAmountUtils.createAmount(orderEntry.getBasePrice(), orderEntry.getOrder().getCurrency().getIsocode()));

        orderLineDetails.setProductPrice(basePrice.longValue());
        orderLineDetails.setDiscountAmount(basePrice.subtract(priceAfterDiscount).longValue());
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

    List<BigDecimal> getItemAmounts(List<Map<String, BigDecimal>> entryPrices) {
        return entryPrices.stream().map(entryPrice -> entryPrice.get(PRICE_AFTER_DISCOUNT)).collect(Collectors.toList());
    }

    private boolean canBeDiscountedByOne(Map<String, BigDecimal> entryPrices) {
        return entryPrices.get(ENTRY_PRICE).subtract(entryPrices.get(PRICE_AFTER_DISCOUNT)).subtract(BigDecimal.ONE).compareTo(BigDecimal.ONE) >= 0;
    }

    @Required
    public void setWorldlineAmountUtils(WorldlineAmountUtils worldlineAmountUtils) {
        this.worldlineAmountUtils = worldlineAmountUtils;
    }

}
