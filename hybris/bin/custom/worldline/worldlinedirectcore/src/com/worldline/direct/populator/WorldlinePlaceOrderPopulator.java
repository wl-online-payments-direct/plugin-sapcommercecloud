package com.worldline.direct.populator;

import de.hybris.platform.b2bacceleratorfacades.checkout.data.PlaceOrderData;
import de.hybris.platform.b2bacceleratorfacades.order.data.B2BReplenishmentRecurrenceEnum;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;

public class WorldlinePlaceOrderPopulator implements Populator<CartModel, PlaceOrderData> {

   @Override
   public void populate(CartModel cartModel, PlaceOrderData placeOrderData) throws ConversionException {

      placeOrderData.setNDays(cartModel.getWorldlineNDays());
      placeOrderData.setNDaysOfWeek(cartModel.getWorldlineNDaysOfWeek());
      placeOrderData.setNthDayOfMonth(cartModel.getWorldlineNthDayOfMonth());
      placeOrderData.setNWeeks(cartModel.getWorldlineNWeeks());
      placeOrderData.setNMonths(cartModel.getWorldlineNMonths());
      placeOrderData.setReplenishmentOrder(cartModel.isWorldlineReplenishmentOrder());
      placeOrderData.setReplenishmentRecurrence(B2BReplenishmentRecurrenceEnum.valueOf(cartModel.getWorldlineReplenishmentRecurrence().getCode()));
      placeOrderData.setReplenishmentStartDate(cartModel.getWorldlineReplenishmentStartDate());
      placeOrderData.setReplenishmentEndDate(cartModel.getWorldlineReplenishmentEndDate());
   }
}
