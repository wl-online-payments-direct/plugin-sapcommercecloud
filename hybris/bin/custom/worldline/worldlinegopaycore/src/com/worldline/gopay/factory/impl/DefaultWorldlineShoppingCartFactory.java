package com.worldline.gopay.factory.impl;

import com.onlinepayments.domain.AmountOfMoney;
import com.onlinepayments.domain.LineItem;
import com.onlinepayments.domain.OrderLineDetails;
import com.onlinepayments.domain.ShoppingCart;
import com.worldline.gopay.factory.WorldlineShoppingCartFactory;
import com.worldline.gopay.util.WorldlineAmountUtils;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.util.DiscountValue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
            BigDecimal tax = calculateTotalLineLevelTax(orderEntry);
            itemAmountOfMoney.setAmount(worldlineAmountUtils.createAmount(calculateLineAmount(abstractOrderModel, orderEntry, tax), currencyISOCode));
            itemAmountOfMoney.setCurrencyCode(currencyISOCode);
            item.setAmountOfMoney(itemAmountOfMoney);
            item.setOrderLineDetails(createOrderLineDetails(abstractOrderModel, orderEntry, currencyISOCode));
            lineItems.add(item);
        }

        cart.setItems(lineItems);
        return cart;
    }

    private BigDecimal calculateTotalLineLevelDiscount(AbstractOrderEntryModel orderEntry) {
        BigDecimal totalDiscount = BigDecimal.ZERO;
        for(DiscountValue discountValue : orderEntry.getDiscountValues()) {
            totalDiscount = totalDiscount.add(BigDecimal.valueOf(discountValue.getAppliedValue()));
        }
        return totalDiscount;
    }

    private BigDecimal calculateTotalLineLevelTax(AbstractOrderEntryModel orderEntry) {
        BigDecimal totalTax = BigDecimal.ZERO;
        Collection<de.hybris.platform.util.TaxValue> taxValues = orderEntry.getTaxValues();
        if (taxValues == null) {
            return totalTax;
        }
        for (de.hybris.platform.util.TaxValue taxValue : taxValues) {
            totalTax = totalTax.add(BigDecimal.valueOf(taxValue.getAppliedValue()));
        }
        return totalTax;
    }

    private BigDecimal calculateLineAmount(AbstractOrderModel order, AbstractOrderEntryModel orderEntry, BigDecimal tax) {
        BigDecimal lineAmount = BigDecimal.valueOf(orderEntry.getTotalPrice());
        if (Boolean.TRUE.equals(order.getNet())) {
            lineAmount = lineAmount.add(tax);
        }
        return lineAmount;
    }

    private OrderLineDetails createOrderLineDetails(AbstractOrderModel order, AbstractOrderEntryModel orderEntry, String currencyISOCode) {
        OrderLineDetails orderLineDetails = new OrderLineDetails();
        orderLineDetails.setProductName(orderEntry.getProduct().getName());
        orderLineDetails.setProductCode(orderEntry.getProduct().getCode());
        BigDecimal discount = calculateTotalLineLevelDiscount(orderEntry);
        BigDecimal tax = calculateTotalLineLevelTax(orderEntry);
        BigDecimal productPrice = calculateNetProductPrice(order, orderEntry, discount, tax);
        orderLineDetails.setQuantity(orderEntry.getQuantity());
        orderLineDetails.setDiscountAmount(worldlineAmountUtils.createAmount(discount, currencyISOCode));
        orderLineDetails.setProductPrice(worldlineAmountUtils.createAmount(productPrice, currencyISOCode));
        orderLineDetails.setTaxAmount(worldlineAmountUtils.createAmount(tax, currencyISOCode));
        return orderLineDetails;
    }

    private BigDecimal calculateNetProductPrice(AbstractOrderModel order, AbstractOrderEntryModel orderEntry, BigDecimal discount, BigDecimal tax) {
        BigDecimal netLinePrice = BigDecimal.valueOf(orderEntry.getTotalPrice()).add(discount);
        if (!Boolean.TRUE.equals(order.getNet())) {
            netLinePrice = netLinePrice.subtract(tax);
        }
        Long quantity = orderEntry.getQuantity();
        if (quantity == null || quantity == 0L) {
            return netLinePrice;
        }
        return netLinePrice.divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP);
    }

    public void setWorldlineAmountUtils(WorldlineAmountUtils worldlineAmountUtils) {
        this.worldlineAmountUtils = worldlineAmountUtils;
    }

}
