package com.worldline.direct.factory.impl;

import com.onlinepayments.domain.*;
import com.worldline.direct.enums.WorldlineMealvouchersProductType;
import com.worldline.direct.factory.WorldlineShoppingCartFactory;
import com.worldline.direct.util.WorldlineAmountUtils;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.core.model.product.UnitModel;
import de.hybris.platform.enumeration.EnumerationService;
import de.hybris.platform.util.DiscountValue;
import de.hybris.platform.util.TaxValue;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MealvouchersWorldlineShoppingCartFactory implements WorldlineShoppingCartFactory {
    private WorldlineAmountUtils worldlineAmountUtils;
    private EnumerationService enumerationService;

    private static final Logger LOG = LoggerFactory.getLogger(MealvouchersWorldlineShoppingCartFactory.class);

    private static final String PLUS = "+";
    private static final String MERGED_ITEM = "Merged Item";

    @Override
    public ShoppingCart create(AbstractOrderModel order) {
        ShoppingCart cart = new ShoppingCart();
        List<AbstractOrderEntryModel> entries = order.getEntries();

        if (entries == null || entries.isEmpty()) {
            return cart;
        }

        return populateMergedCart(cart, order);
    }

    private ShoppingCart populateMergedCart(ShoppingCart cart, AbstractOrderModel order) {
        String currencyIsoCode = order.getCurrency().getIsocode();
        LineItem lineItem = new LineItem();
        OrderLineDetails orderLineDetails = new OrderLineDetails();

        // Accumulators
        List<String> productNames = new ArrayList<>();
        BigDecimal linePrice = BigDecimal.ZERO;
        BigDecimal lineDiscounts = BigDecimal.ZERO;
        BigDecimal totalTaxes = BigDecimal.ZERO;
        WorldlineMealvouchersProductType overallProductType = null;

        boolean sameUnit = true;
        UnitModel detectedUnit = null;

        for (AbstractOrderEntryModel entry : order.getEntries()) {
            ProductModel product = entry.getProduct();
            if (product == null) {
                LOG.warn("No Product found for OrderEntry {}, skipping.", entry.getPk());
                continue;
            }

            // Accumulate Totals
            BigDecimal thisDiscount = BigDecimal.valueOf(calculateTotalLineLevelDiscountAmount(entry));
            BigDecimal thisTax = BigDecimal.valueOf(calculateTotalLineLevelTaxAmount(entry));

            lineDiscounts = lineDiscounts.add(thisDiscount);
            totalTaxes = totalTaxes.add(thisTax);
            linePrice = linePrice.add(BigDecimal.valueOf(entry.getTotalPrice()));//.subtract(thisTax).subtract(thisDiscount));

            // Accumulate Name and Type
            productNames.add(product.getName());
            overallProductType = resolveProductType(overallProductType, product.getWorldlineMealvoucherProductType());

            // --- Logic: Check Unit Consistency ---
            if (sameUnit) {
                UnitModel currentUnit = product.getUnit();
                if (detectedUnit == null) {
                    // First non-null unit found, or simply the first unit (even if null)
                    detectedUnit = currentUnit;
                } else if (!detectedUnit.equals(currentUnit)) {
                    // We had a unit, but this one is different (or null while previous wasn't)
                    sameUnit = false;
                }
            }
        }

        validateProductType(overallProductType);

        // Finalise Tax Calculation
        if (totalTaxes.compareTo(BigDecimal.ZERO) == 0) {
            totalTaxes = BigDecimal.valueOf(order.getTotalTax());
        }

        String overallProductTypeName = overallProductType.getCode();

        // Set Details
        if(order.getEntries().size() == 1) {
            // We only have one line, so do it the 'normal' way.
            AbstractOrderEntryModel entry = order.getEntries().get(0);
            ProductModel product = entry.getProduct();
            orderLineDetails.setProductCode(product.getCode());
            orderLineDetails.setProductName(product.getName());
            orderLineDetails.setQuantity(entry.getQuantity());
        } else {
            // More than one line so 'merge' them.
            orderLineDetails.setProductCode(MERGED_ITEM);
            orderLineDetails.setProductName(generateMergedProductName(productNames, overallProductTypeName, order.getEntries().size()));
            orderLineDetails.setQuantity(1L);
        }
        orderLineDetails.setProductType(overallProductTypeName);

        // Price/unit data should be the same whether merged or not.
        orderLineDetails.setProductPrice(worldlineAmountUtils.createAmount(linePrice, currencyIsoCode));
        orderLineDetails.setTaxAmount(0L);
        orderLineDetails.setUnit(resolveUnitString(sameUnit, detectedUnit));
        // Removed because we seem to not be handling tax anywhere else...
        //orderLineDetails.setTaxAmount(worldlineAmountUtils.createAmount(totalTaxes, currencyIsoCode));
        orderLineDetails.setDiscountAmount(worldlineAmountUtils.createAmount(lineDiscounts, currencyIsoCode));


        // Set Money
        AmountOfMoney amountOfMoney = new AmountOfMoney();
        amountOfMoney.setCurrencyCode(currencyIsoCode);
        amountOfMoney.setAmount(worldlineAmountUtils.createAmount(linePrice.subtract(lineDiscounts), currencyIsoCode));

        lineItem.setAmountOfMoney(amountOfMoney);
        lineItem.setOrderLineDetails(orderLineDetails);

        cart.setItems(Collections.singletonList(lineItem));
        return cart;
    }

    /**
     * Resolves the product type based on hierarchy:
     * FOOD > HOME > GIFT.
     */
    private WorldlineMealvouchersProductType resolveProductType(WorldlineMealvouchersProductType currentType,
                                                                WorldlineMealvouchersProductType newType) {
        if (WorldlineMealvouchersProductType.FOODANDDRINK.equals(currentType)) {
            return currentType;
        }

        if (WorldlineMealvouchersProductType.FOODANDDRINK.equals(newType)) {
            return newType;
        }

        if (WorldlineMealvouchersProductType.HOMEANDGARDEN.equals(newType)) {
            return newType;
        }

        if (WorldlineMealvouchersProductType.GIFTANDFLOWERS.equals(newType) && currentType == null) {
            return newType;
        }

        return currentType;
    }

    private void validateProductType(WorldlineMealvouchersProductType type) {
        if (type == null) {
            throw new IllegalStateException("No Mealvoucher products in cart, yet Mealvoucher selected as payment mode. This should not be possible.");
        }
    }

    private String generateMergedProductName(List<String> names, String type, int entryCount) {
        String joinedNames = String.join(PLUS, names);

        if (joinedNames.length() < 50) {
            return joinedNames;
        }

        // Fallback format: "Count + TypeCode"
        StringBuilder fallbackName = new StringBuilder();
        fallbackName.append(entryCount).append(PLUS).append(type);

        if (fallbackName.length() < 50) {
            return fallbackName.toString();
        }

        return fallbackName.substring(0, 49);
    }

    private double calculateTotalLineLevelDiscountAmount(AbstractOrderEntryModel orderEntry) {
        double totalDiscount = 0d;
        for(DiscountValue discountValue : orderEntry.getDiscountValues()) {
            totalDiscount += discountValue.getAppliedValue();
        }
        return totalDiscount;
    }

    private double calculateTotalLineLevelTaxAmount(AbstractOrderEntryModel orderEntry) {
        double totalTaxes = 0d;
        for(TaxValue taxValue : orderEntry.getTaxValues()) {
            totalTaxes += taxValue.getAppliedValue();
        }
        return totalTaxes;
    }

    private String resolveUnitString(boolean sameUnit, UnitModel detectedUnit) {
        if (!sameUnit) {
            return MERGED_ITEM; // Different units found
        }
        if (detectedUnit != null) {
            return detectedUnit.getCode(); // All units are the same
        }
        return ""; // All units were null
    }

    @Required
    public void setWorldlineAmountUtils(WorldlineAmountUtils worldlineAmountUtils) {
        this.worldlineAmountUtils = worldlineAmountUtils;
    }

    @Required
    public void setEnumerationService(EnumerationService enumerationService) {
        this.enumerationService = enumerationService;
    }
}
