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
            BigDecimal tax = calculateTotalLineLevelTax(orderEntry);
            long lineAmount = worldlineAmountUtils.createAmount(calculateLineAmount(abstractOrderModel, orderEntry, tax), currencyISOCode);
            long lineTax = worldlineAmountUtils.createAmount(tax, currencyISOCode);
            long lineDiscount = worldlineAmountUtils.createAmount(calculateTotalLineLevelDiscount(orderEntry), currencyISOCode);
            for (WorldlineLineItemPortions.Portion portion : WorldlineLineItemPortions.split(orderEntry.getQuantity(), lineAmount - lineTax, lineDiscount, lineTax)) {
                LineItem item = new LineItem();
                AmountOfMoney itemAmountOfMoney = new AmountOfMoney();
                itemAmountOfMoney.setAmount(portion.amount());
                itemAmountOfMoney.setCurrencyCode(currencyISOCode);
                item.setAmountOfMoney(itemAmountOfMoney);
                item.setOrderLineDetails(createOrderLineDetails(orderEntry, portion));
                lineItems.add(item);
            }
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

    private OrderLineDetails createOrderLineDetails(AbstractOrderEntryModel orderEntry, WorldlineLineItemPortions.Portion portion) {
        OrderLineDetails orderLineDetails = new OrderLineDetails();
        orderLineDetails.setProductName(orderEntry.getProduct().getName());
        orderLineDetails.setProductCode(orderEntry.getProduct().getCode());
        orderLineDetails.setQuantity(portion.quantity());
        orderLineDetails.setDiscountAmount(portion.discount());
        orderLineDetails.setProductPrice(portion.productPrice());
        orderLineDetails.setTaxAmount(portion.tax());
        return orderLineDetails;
    }

    public void setWorldlineAmountUtils(WorldlineAmountUtils worldlineAmountUtils) {
        this.worldlineAmountUtils = worldlineAmountUtils;
    }

}
