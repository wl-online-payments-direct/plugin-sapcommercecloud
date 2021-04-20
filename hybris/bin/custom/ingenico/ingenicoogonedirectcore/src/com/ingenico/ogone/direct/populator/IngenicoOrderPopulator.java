package com.ingenico.ogone.direct.populator;

import com.ingenico.ogone.direct.order.data.IngenicoPaymentInfoData;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.payment.IngenicoPaymentInfoModel;
import de.hybris.platform.core.model.order.payment.PaymentInfoModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import de.hybris.platform.servicelayer.dto.converter.Converter;

public class IngenicoOrderPopulator implements Populator<OrderModel, OrderData> {
   private Converter<IngenicoPaymentInfoModel, IngenicoPaymentInfoData> ingenicoPaymentInfoConverter;

   @Override public void populate(OrderModel orderModel, OrderData orderData) throws ConversionException {

      final PaymentInfoModel paymentInfo = orderModel.getPaymentInfo();
      if (paymentInfo instanceof IngenicoPaymentInfoModel) {
         IngenicoPaymentInfoData ingenicoPaymentInfoData = new IngenicoPaymentInfoData();
         ingenicoPaymentInfoConverter.convert((IngenicoPaymentInfoModel) paymentInfo, ingenicoPaymentInfoData);
         orderData.setIngenicoPaymentInfo(ingenicoPaymentInfoData);
      }
   }

   public void setIngenicoPaymentInfoConverter(Converter<IngenicoPaymentInfoModel, IngenicoPaymentInfoData> ingenicoPaymentInfoConverter) {
      this.ingenicoPaymentInfoConverter = ingenicoPaymentInfoConverter;
   }
}
