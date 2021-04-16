package com.ingenico.ogone.direct.facade.impl;

import java.util.ArrayList;
import java.util.List;

import de.hybris.platform.commerceservices.strategies.CheckoutCustomerStrategy;
import de.hybris.platform.core.model.order.payment.IngenicoPaymentInfoModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.servicelayer.dto.converter.Converter;

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
