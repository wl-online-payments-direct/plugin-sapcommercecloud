package com.worldline.direct.populator;

import com.worldline.direct.model.WorldlineMandateModel;
import com.worldline.direct.order.data.WorldlineMandateDetail;
import com.worldline.direct.order.data.WorldlinePaymentInfoData;
import com.worldline.direct.util.WorldlinePaymentProductUtils;
import de.hybris.platform.commercefacades.order.data.CardTypeData;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Required;

public class WorldlinePaymentInfoPopulator implements Populator<WorldlinePaymentInfoModel, WorldlinePaymentInfoData> {

    private Converter<AddressModel, AddressData> addressConverter;
    private Converter<WorldlineMandateModel, WorldlineMandateDetail> worldlineMandateConverter;

    @Override
    public void populate(WorldlinePaymentInfoModel worldlinePaymentInfoModel, WorldlinePaymentInfoData worldlinePaymentInfoData) throws ConversionException {

        worldlinePaymentInfoData.setCode(worldlinePaymentInfoModel.getCode());
        worldlinePaymentInfoData.setId(worldlinePaymentInfoModel.getId());
        worldlinePaymentInfoData.setPaymentMethod(worldlinePaymentInfoModel.getPaymentMethod());
        worldlinePaymentInfoData.setPaymentProductDirectoryId(worldlinePaymentInfoModel.getPaymentProductDirectoryId());
        worldlinePaymentInfoData.setHostedTokenizationId(worldlinePaymentInfoModel.getHostedTokenizationId());
        worldlinePaymentInfoData.setWorldlineCheckoutType(worldlinePaymentInfoModel.getWorldlineCheckoutType());
        worldlinePaymentInfoData.setAlias(formattedAlias(worldlinePaymentInfoModel.getAlias()));
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
        worldlinePaymentInfoData.setSaved(worldlinePaymentInfoModel.isSaved());
        CardTypeData cardTypeData=new CardTypeData();
        cardTypeData.setName(worldlinePaymentInfoModel.getCardBrand());
        worldlinePaymentInfoData.setCardType(cardTypeData);
        if (worldlinePaymentInfoModel.getUsedSavedPayment() != null) {
            worldlinePaymentInfoData.setSavedPayment(worldlinePaymentInfoModel.getUsedSavedPayment().getCode());
        }
        if (WorldlinePaymentProductUtils.isPaymentBySepaDirectDebit(worldlinePaymentInfoModel) && worldlinePaymentInfoModel.getMandateDetail() != null) {
            worldlinePaymentInfoData.setMandateDetail(worldlineMandateConverter.convert(worldlinePaymentInfoModel.getMandateDetail()));
        }

    }

    private String formattedAlias(String alias) {
        if (StringUtils.isNotEmpty(alias) && alias.length()>4) {
            return "*".repeat(12) + alias.substring(alias.length() - 4);
        }else {
            return StringUtils.EMPTY;
        }
    }

    @Required
    public void setWorldlineMandateConverter(Converter<WorldlineMandateModel, WorldlineMandateDetail> worldlineMandateConverter) {
        this.worldlineMandateConverter = worldlineMandateConverter;
    }

    public void setAddressConverter(Converter<AddressModel, AddressData> addressConverter) {
        this.addressConverter = addressConverter;
    }
}
