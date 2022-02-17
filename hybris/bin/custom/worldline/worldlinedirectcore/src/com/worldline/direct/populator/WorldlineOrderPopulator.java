package com.worldline.direct.populator;

import com.worldline.direct.order.data.WorldlinePaymentInfoData;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.core.model.order.payment.PaymentInfoModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import de.hybris.platform.servicelayer.dto.converter.Converter;

public class WorldlineOrderPopulator implements Populator<OrderModel, OrderData> {
   private Converter<WorldlinePaymentInfoModel, WorldlinePaymentInfoData> worldlinePaymentInfoConverter;

   @Override public void populate(OrderModel orderModel, OrderData orderData) throws ConversionException {

      final PaymentInfoModel paymentInfo = orderModel.getPaymentInfo();
      if (paymentInfo instanceof WorldlinePaymentInfoModel) {
         WorldlinePaymentInfoData worldlinePaymentInfoData = new WorldlinePaymentInfoData();
         worldlinePaymentInfoConverter.convert((WorldlinePaymentInfoModel) paymentInfo, worldlinePaymentInfoData);
         orderData.setWorldlinePaymentInfo(worldlinePaymentInfoData);
      }
   }

   public void setWorldlinePaymentInfoConverter(Converter<WorldlinePaymentInfoModel, WorldlinePaymentInfoData> worldlinePaymentInfoConverter) {
      this.worldlinePaymentInfoConverter = worldlinePaymentInfoConverter;
   }
}
