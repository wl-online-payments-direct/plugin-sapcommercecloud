package com.worldline.direct.populator;

import com.worldline.direct.order.data.WorldlinePaymentInfoData;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.payment.PaymentInfoModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import de.hybris.platform.servicelayer.dto.converter.Converter;

public class WorldlineCartPopulator implements Populator<CartModel, CartData> {

    private Converter<WorldlinePaymentInfoModel, WorldlinePaymentInfoData> worldlinePaymentInfoConverter;

    @Override
    public void populate(final CartModel source, final CartData target) throws ConversionException {
        final PaymentInfoModel paymentInfo = source.getPaymentInfo();
        if (paymentInfo instanceof WorldlinePaymentInfoModel) {
            WorldlinePaymentInfoData worldlinePaymentInfoData = new WorldlinePaymentInfoData();
            worldlinePaymentInfoConverter.convert((WorldlinePaymentInfoModel) paymentInfo, worldlinePaymentInfoData);
            target.setWorldlinePaymentInfo(worldlinePaymentInfoData);
        }
    }

    public void setWorldlinePaymentInfoConverter(Converter<WorldlinePaymentInfoModel, WorldlinePaymentInfoData> worldlinePaymentInfoConverter) {
        this.worldlinePaymentInfoConverter = worldlinePaymentInfoConverter;
    }
}
