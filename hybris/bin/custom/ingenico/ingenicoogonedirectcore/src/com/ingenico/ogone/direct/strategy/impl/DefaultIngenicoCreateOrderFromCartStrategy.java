package com.ingenico.ogone.direct.strategy.impl;

import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.order.strategies.impl.DefaultCreateOrderFromCartStrategy;
import org.apache.commons.lang3.StringUtils;

public class DefaultIngenicoCreateOrderFromCartStrategy extends DefaultCreateOrderFromCartStrategy {

   @Override
   protected String generateOrderCode(final CartModel cart) {
      if (StringUtils.isNotEmpty(cart.getCode())) {
         //Use the Cart code as order code
         return cart.getCode();
      }

      return super.generateOrderCode(cart);
   }
}
