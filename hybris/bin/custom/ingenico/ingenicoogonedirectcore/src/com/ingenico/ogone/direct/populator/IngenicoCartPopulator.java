package com.ingenico.ogone.direct.populator;

import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.payment.IngenicoPaymentInfoModel;
import de.hybris.platform.core.model.order.payment.PaymentInfoModel;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import de.hybris.platform.servicelayer.dto.converter.Converter;

import com.ingenico.ogone.direct.order.data.IngenicoPaymentInfoData;

public class IngenicoCartPopulator implements Populator<CartModel, CartData> {

    private Converter<IngenicoPaymentInfoModel, IngenicoPaymentInfoData> ingenicoPaymentInfoConverter;

    @Override
    public void populate(final CartModel source, final CartData target) throws ConversionException {
        final PaymentInfoModel paymentInfo = source.getPaymentInfo();
        if (paymentInfo instanceof IngenicoPaymentInfoModel) {
            IngenicoPaymentInfoData ingenicoPaymentInfoData = new IngenicoPaymentInfoData();
            ingenicoPaymentInfoConverter.convert((IngenicoPaymentInfoModel) paymentInfo, ingenicoPaymentInfoData);
            target.setIngenicoPaymentInfo(ingenicoPaymentInfoData);
        }
    }

    public void setIngenicoPaymentInfoConverter(Converter<IngenicoPaymentInfoModel, IngenicoPaymentInfoData> ingenicoPaymentInfoConverter) {
        this.ingenicoPaymentInfoConverter = ingenicoPaymentInfoConverter;
    }
}
