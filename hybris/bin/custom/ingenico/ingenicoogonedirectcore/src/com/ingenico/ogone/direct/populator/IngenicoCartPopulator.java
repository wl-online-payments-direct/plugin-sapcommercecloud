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

    private Converter<AddressModel, AddressData> addressConverter;

    @Override
    public void populate(final CartModel source, final CartData target) throws ConversionException {

        final PaymentInfoModel paymentInfo = source.getPaymentInfo();
        final IngenicoPaymentInfoData ingenicoPaymentInfoData = new IngenicoPaymentInfoData();
        if (paymentInfo instanceof IngenicoPaymentInfoModel) {
            ingenicoPaymentInfoData.setId(((IngenicoPaymentInfoModel) paymentInfo).getId());
            ingenicoPaymentInfoData.setPaymentMethod(((IngenicoPaymentInfoModel) paymentInfo).getPaymentMethod());
            ingenicoPaymentInfoData.setIngenicoCheckoutType(((IngenicoPaymentInfoModel) paymentInfo).getIngenicoCheckoutType());
            ingenicoPaymentInfoData.setBillingAddress(addressConverter.convert(paymentInfo.getBillingAddress()));
        }
        target.setIngenicoPaymentInfo(ingenicoPaymentInfoData);
    }

    public void setAddressConverter(Converter<AddressModel, AddressData> addressConverter) {
        this.addressConverter = addressConverter;
    }
}
