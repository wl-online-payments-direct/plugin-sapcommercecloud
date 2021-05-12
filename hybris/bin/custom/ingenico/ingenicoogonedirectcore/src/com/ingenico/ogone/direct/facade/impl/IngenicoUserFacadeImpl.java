package com.ingenico.ogone.direct.facade.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.hybris.platform.commerceservices.strategies.CheckoutCustomerStrategy;
import de.hybris.platform.core.model.order.payment.IngenicoPaymentInfoModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.servicelayer.dto.converter.Converter;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import com.ingenico.ogone.direct.facade.IngenicoUserFacade;
import com.ingenico.ogone.direct.order.data.IngenicoPaymentInfoData;
import com.ingenico.ogone.direct.service.IngenicoCustomerAccountService;

public class IngenicoUserFacadeImpl implements IngenicoUserFacade {

    private CheckoutCustomerStrategy checkoutCustomerStrategy;
    private IngenicoCustomerAccountService ingenicoCustomerAccountService;
    private Converter<IngenicoPaymentInfoModel, IngenicoPaymentInfoData> ingenicoPaymentInfoConverter;

    @Override
    public List<IngenicoPaymentInfoData> getIngenicoPaymentInfos(boolean saved) {
        final CustomerModel currentCustomer = checkoutCustomerStrategy.getCurrentUserForCheckout();
        final List<IngenicoPaymentInfoModel> ingenicoPaymentInfos = ingenicoCustomerAccountService.getIngenicoPaymentInfos(currentCustomer, saved);
        final List<IngenicoPaymentInfoData> ingenicoPaymentInfoDataList = new ArrayList<>();

        for (final IngenicoPaymentInfoModel ingenicoPaymentInfoModel : ingenicoPaymentInfos) {
            final IngenicoPaymentInfoData paymentInfoData = ingenicoPaymentInfoConverter.convert(ingenicoPaymentInfoModel);
            ingenicoPaymentInfoDataList.add(paymentInfoData);
        }
        return ingenicoPaymentInfoDataList;
    }

    @Override
    public List<String> getSavedTokens() {
        final List<IngenicoPaymentInfoData> ingenicoPaymentInfos = getIngenicoPaymentInfos(true);
        return ingenicoPaymentInfos.stream().map(IngenicoPaymentInfoData::getToken).collect(Collectors.toList());
    }

    @Override
    public List<String> getSavedTokensForPaymentMethod(Integer paymentMethodId) { //return list of unique tokens
        final List<IngenicoPaymentInfoData> ingenicoPaymentInfos = getIngenicoPaymentInfos(true);
        final List<String> allSavedTokensForPM = ingenicoPaymentInfos.stream().filter(ingenicoPaymentInfoData -> ingenicoPaymentInfoData.getId().equals(paymentMethodId)).map(IngenicoPaymentInfoData::getToken).collect(Collectors.toList());
        return allSavedTokensForPM.stream().distinct().collect(Collectors.toList());
    }

    @Override
    public List<IngenicoPaymentInfoData> getIngenicoPaymentInfoByToken(String token) {
        return null;
    }

    public void setCheckoutCustomerStrategy(CheckoutCustomerStrategy checkoutCustomerStrategy) {
        this.checkoutCustomerStrategy = checkoutCustomerStrategy;
    }

    public void setIngenicoCustomerAccountService(IngenicoCustomerAccountService ingenicoCustomerAccountService) {
        this.ingenicoCustomerAccountService = ingenicoCustomerAccountService;
    }

    public void setIngenicoPaymentInfoConverter(Converter<IngenicoPaymentInfoModel, IngenicoPaymentInfoData> ingenicoPaymentInfoConverter) {
        this.ingenicoPaymentInfoConverter = ingenicoPaymentInfoConverter;
    }
}
