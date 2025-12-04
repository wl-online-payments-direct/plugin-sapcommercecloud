package com.worldline.direct.strategy.impl;

import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import com.worldline.direct.strategy.WorldlinePaymentProductFilterByCartDataStrategy;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.order.data.OrderEntryData;
import de.hybris.platform.commercefacades.product.data.ProductData;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Set;

public class WorldlinePaymentProductFilterByCartDataMealvouchersStrategy implements WorldlinePaymentProductFilterByCartDataStrategy {
    @Override
    public Set<Integer> filter(CartData cartData) {
        Set<Integer> output = new HashSet<>();
        boolean hasMealvouchersType = false;
        for(OrderEntryData orderEntry : cartData.getEntries()) {
            ProductData product = orderEntry.getProduct();
            if(product != null && StringUtils.isNotBlank(product.getWorldlineMealvoucherProductType())) {
                // If any Product in the cart has a mealvoucher type, we can show mealvouchers.
                hasMealvouchersType = true;
                break;
            }
        }
        if(!hasMealvouchersType) {
            // If no product has Mealvouchers type, add it to the list to filter out.
            output.add(WorldlinedirectcoreConstants.PAYMENT_METHOD_MEALVOUCHER);
        }
        return output;
    }
}
