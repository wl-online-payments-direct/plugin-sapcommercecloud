package com.worldline.direct.populator;

import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import de.hybris.platform.servicelayer.dto.converter.Converter;

import org.apache.commons.lang.StringUtils;

import com.worldline.direct.order.data.WorldlinePaymentInfoData;

public class WorldlinePaymentInfoPopulator implements Populator<WorldlinePaymentInfoModel, WorldlinePaymentInfoData> {

    private Converter<AddressModel, AddressData> addressConverter;

    @Override
    public void populate(WorldlinePaymentInfoModel worldlinePaymentInfoModel, WorldlinePaymentInfoData worldlinePaymentInfoData) throws ConversionException {

        worldlinePaymentInfoData.setCode(worldlinePaymentInfoModel.getCode());
        worldlinePaymentInfoData.setId(worldlinePaymentInfoModel.getId());
        worldlinePaymentInfoData.setPaymentMethod(worldlinePaymentInfoModel.getPaymentMethod());
        worldlinePaymentInfoData.setPaymentProductDirectoryId(worldlinePaymentInfoModel.getPaymentProductDirectoryId());
        worldlinePaymentInfoData.setHostedTokenizationId(worldlinePaymentInfoModel.getHostedTokenizationId());
        worldlinePaymentInfoData.setWorldlineCheckoutType(worldlinePaymentInfoModel.getWorldlineCheckoutType());
        worldlinePaymentInfoData.setAlias(worldlinePaymentInfoModel.getAlias());
        worldlinePaymentInfoData.setCardholderName(worldlinePaymentInfoModel.getCardholderName());
        worldlinePaymentInfoData.setExpiryDate(worldlinePaymentInfoModel.getExpiryDate());
        String[] splittedDate = StringUtils.split(worldlinePaymentInfoModel.getExpiryDate(), "/");
        if (splittedDate != null && splittedDate.length == 2) {
            worldlinePaymentInfoData.setExpiryMonth(splittedDate[0]);
            worldlinePaymentInfoData.setExpiryYear(splittedDate[1]);
        }
        worldlinePaymentInfoData.setToken(worldlinePaymentInfoModel.getToken());
        worldlinePaymentInfoData.setCardBrand(worldlinePaymentInfoModel.getCardBrand());
        worldlinePaymentInfoData.setType(worldlinePaymentInfoModel.getType());

        if (worldlinePaymentInfoModel.getBillingAddress() != null) {
            AddressData addressData = new AddressData();
            addressConverter.convert(worldlinePaymentInfoModel.getBillingAddress(), addressData);
            worldlinePaymentInfoData.setBillingAddress(addressData);
        }


        worldlinePaymentInfoData.setReturnMAC(worldlinePaymentInfoModel.getReturnMAC());
    }

    public void setAddressConverter(Converter<AddressModel, AddressData> addressConverter) {
        this.addressConverter = addressConverter;
    }
}
