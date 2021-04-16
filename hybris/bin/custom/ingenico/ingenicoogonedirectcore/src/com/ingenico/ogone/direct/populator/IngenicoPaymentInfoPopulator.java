package com.ingenico.ogone.direct.populator;

import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.payment.IngenicoPaymentInfoModel;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import de.hybris.platform.servicelayer.dto.converter.Converter;

import com.ingenico.ogone.direct.order.data.IngenicoPaymentInfoData;

public class IngenicoPaymentInfoPopulator implements Populator<IngenicoPaymentInfoModel, IngenicoPaymentInfoData> {

    private Converter<AddressModel, AddressData> addressConverter;

    @Override
    public void populate(IngenicoPaymentInfoModel ingenicoPaymentInfoModel, IngenicoPaymentInfoData ingenicoPaymentInfoData) throws ConversionException {

        ingenicoPaymentInfoData.setId(ingenicoPaymentInfoModel.getId());
        ingenicoPaymentInfoData.setPaymentMethod(ingenicoPaymentInfoModel.getPaymentMethod());
        ingenicoPaymentInfoData.setIngenicoCheckoutType(ingenicoPaymentInfoModel.getIngenicoCheckoutType());
        ingenicoPaymentInfoData.setAlias(ingenicoPaymentInfoModel.getAlias());
        ingenicoPaymentInfoData.setCardholderName(ingenicoPaymentInfoModel.getCardholderName());
        ingenicoPaymentInfoData.setExpiryDate(ingenicoPaymentInfoModel.getExpiryDate());
        ingenicoPaymentInfoData.setToken(ingenicoPaymentInfoModel.getToken());
        ingenicoPaymentInfoData.setType(ingenicoPaymentInfoModel.getType());

        AddressData addressData = new AddressData();
        addressConverter.convert(ingenicoPaymentInfoModel.getBillingAddress(), addressData);
        ingenicoPaymentInfoData.setBillingAddress(addressData);

    }

    public void setAddressConverter(Converter<AddressModel, AddressData> addressConverter) {
        this.addressConverter = addressConverter;
    }


}
