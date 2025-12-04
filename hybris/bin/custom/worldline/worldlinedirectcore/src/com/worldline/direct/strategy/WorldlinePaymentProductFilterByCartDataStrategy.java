package com.worldline.direct.strategy;

import de.hybris.platform.commercefacades.order.data.CartData;

import java.util.Set;

public interface WorldlinePaymentProductFilterByCartDataStrategy {
    Set<Integer> filter(CartData cartData);
}
