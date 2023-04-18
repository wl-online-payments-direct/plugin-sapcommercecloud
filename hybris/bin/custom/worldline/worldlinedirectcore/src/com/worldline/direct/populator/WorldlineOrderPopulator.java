package com.worldline.direct.populator;

import com.worldline.direct.order.data.WorldlinePaymentInfoData;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commercefacades.product.PriceDataFactory;
import de.hybris.platform.commercefacades.product.data.PriceData;
import de.hybris.platform.commercefacades.product.data.PriceDataType;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.core.model.order.payment.PaymentInfoModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import de.hybris.platform.servicelayer.dto.converter.Converter;

import java.math.BigDecimal;

public class WorldlineOrderPopulator implements Populator<OrderModel, OrderData> {
   private Converter<WorldlinePaymentInfoModel, WorldlinePaymentInfoData> worldlinePaymentInfoConverter;

   private PriceDataFactory priceDataFactory;

   @Override public void populate(OrderModel orderModel, OrderData orderData) throws ConversionException {

      final PaymentInfoModel paymentInfo = orderModel.getPaymentInfo();
      if (paymentInfo instanceof WorldlinePaymentInfoModel) {
         WorldlinePaymentInfoData worldlinePaymentInfoData = new WorldlinePaymentInfoData();
         worldlinePaymentInfoConverter.convert((WorldlinePaymentInfoModel) paymentInfo, worldlinePaymentInfoData);
         orderData.setWorldlinePaymentInfo(worldlinePaymentInfoData);
         orderData.setSurcharge(priceDataFactory.create(PriceDataType.BUY, new BigDecimal(orderModel.getPaymentCost()), orderModel.getCurrency()));
      }
   }

   public void setWorldlinePaymentInfoConverter(Converter<WorldlinePaymentInfoModel, WorldlinePaymentInfoData> worldlinePaymentInfoConverter) {
      this.worldlinePaymentInfoConverter = worldlinePaymentInfoConverter;
   }

   public void setPriceDataFactory(PriceDataFactory priceDataFactory) {
      this.priceDataFactory = priceDataFactory;
   }
}
