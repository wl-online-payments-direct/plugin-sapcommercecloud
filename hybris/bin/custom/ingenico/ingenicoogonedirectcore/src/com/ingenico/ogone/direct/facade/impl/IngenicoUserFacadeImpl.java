package com.ingenico.ogone.direct.facade.impl;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNullStandardMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import de.hybris.platform.commerceservices.strategies.CheckoutCustomerStrategy;
import de.hybris.platform.core.model.order.payment.IngenicoPaymentInfoModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.servicelayer.exceptions.ModelNotFoundException;
import de.hybris.platform.servicelayer.model.ModelService;

import com.ingenico.direct.domain.CardWithoutCvv;
import com.ingenico.direct.domain.PaymentProduct;
import com.ingenico.direct.domain.TokenResponse;
import com.ingenico.ogone.direct.facade.IngenicoUserFacade;
import com.ingenico.ogone.direct.order.data.IngenicoPaymentInfoData;
import com.ingenico.ogone.direct.service.IngenicoCustomerAccountService;
import com.ingenico.ogone.direct.service.IngenicoPaymentService;

public class IngenicoUserFacadeImpl implements IngenicoUserFacade {

    private static final String EXPIRY_DATE_REGEX = "(?<=\\G..)";
    private static final Pattern EXPIRY_DATE_PATTERN = Pattern.compile(EXPIRY_DATE_REGEX);
    private ModelService modelService;
    private CheckoutCustomerStrategy checkoutCustomerStrategy;
    private IngenicoCustomerAccountService ingenicoCustomerAccountService;
    private IngenicoPaymentService ingenicoPaymentService;
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
        return ingenicoPaymentInfos.stream()
                .filter(ingenicoPaymentInfoData -> ingenicoPaymentInfoData.getId().equals(paymentMethodId))
                .map(IngenicoPaymentInfoData::getToken)
                .collect(Collectors.toList());
    }

    @Override
    public IngenicoPaymentInfoData getIngenicoPaymentInfoByToken(String token) {
        final CustomerModel currentCustomer = checkoutCustomerStrategy.getCurrentUserForCheckout();
        final IngenicoPaymentInfoModel ingenicoPaymentInfoByToken = ingenicoCustomerAccountService.getIngenicoPaymentInfoByToken(currentCustomer, token, true);
        if (ingenicoPaymentInfoByToken != null) {
            return ingenicoPaymentInfoConverter.convert(ingenicoPaymentInfoByToken);
        }
        return null;
    }

    @Override
    public void saveIngenicoPaymentInfo(TokenResponse tokenResponse, PaymentProduct paymentProduct) {
        final CustomerModel currentCustomer = checkoutCustomerStrategy.getCurrentUserForCheckout();
        IngenicoPaymentInfoModel ingenicoPaymentInfoByToken = ingenicoCustomerAccountService.getIngenicoPaymentInfoByToken(
                currentCustomer,
                tokenResponse.getId(),
                true);
        if (ingenicoPaymentInfoByToken == null) {
            ingenicoPaymentInfoByToken = modelService.create(IngenicoPaymentInfoModel.class);
            ingenicoPaymentInfoByToken.setCode(String.valueOf(UUID.randomUUID()));
            ingenicoPaymentInfoByToken.setUser(currentCustomer);
        }
        ingenicoPaymentInfoByToken.setId(tokenResponse.getPaymentProductId());
        final CardWithoutCvv cardWithoutCvv = tokenResponse.getCard().getData().getCardWithoutCvv();
        ingenicoPaymentInfoByToken.setCardholderName(cardWithoutCvv.getCardholderName());
        ingenicoPaymentInfoByToken.setAlias(cardWithoutCvv.getCardNumber());
        ingenicoPaymentInfoByToken.setExpiryDate(String.join("/", EXPIRY_DATE_PATTERN.split(cardWithoutCvv.getExpiryDate())));

        ingenicoPaymentInfoByToken.setToken(tokenResponse.getId());
        ingenicoPaymentInfoByToken.setCardBrand(paymentProduct.getDisplayHints().getLabel());

        ingenicoPaymentInfoByToken.setSaved(true);

        modelService.save(ingenicoPaymentInfoByToken);
    }

    @Override
    public void deleteSavedIngenicoPaymentInfo(String code) {
        validateParameterNotNullStandardMessage("code", code);
        final CustomerModel currentCustomer = checkoutCustomerStrategy.getCurrentUserForCheckout();
        final IngenicoPaymentInfoModel ingenicoPaymentInfoModel = ingenicoCustomerAccountService.getIngenicoPaymentInfos(currentCustomer, true)
                .stream()
                .filter(paymentInfo -> code.equals(paymentInfo.getCode()))
                .findFirst()
                .orElseThrow(() -> new ModelNotFoundException("Failed to find IngenicoPaymentInfo for the given code : " + code));

        ingenicoPaymentService.deleteToken(ingenicoPaymentInfoModel.getToken());
        modelService.remove(ingenicoPaymentInfoModel);

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

    public void setModelService(ModelService modelService) {
        this.modelService = modelService;
    }

    public void setIngenicoPaymentService(IngenicoPaymentService ingenicoPaymentService) {
        this.ingenicoPaymentService = ingenicoPaymentService;
    }
}
