package com.worldline.direct.facade.impl;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNullStandardMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.worldline.direct.facade.WorldlineUserFacade;
import com.worldline.direct.service.WorldlineCustomerAccountService;
import com.worldline.direct.service.WorldlinePaymentService;
import de.hybris.platform.commerceservices.strategies.CheckoutCustomerStrategy;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.servicelayer.exceptions.ModelNotFoundException;
import de.hybris.platform.servicelayer.model.ModelService;

import com.ingenico.direct.domain.CardWithoutCvv;
import com.ingenico.direct.domain.PaymentProduct;
import com.ingenico.direct.domain.TokenResponse;
import com.worldline.direct.order.data.WorldlinePaymentInfoData;

public class WorldlineUserFacadeImpl implements WorldlineUserFacade {

    private static final String EXPIRY_DATE_REGEX = "(?<=\\G..)";
    private static final Pattern EXPIRY_DATE_PATTERN = Pattern.compile(EXPIRY_DATE_REGEX);
    private ModelService modelService;
    private CheckoutCustomerStrategy checkoutCustomerStrategy;
    private WorldlineCustomerAccountService worldlineCustomerAccountService;
    private WorldlinePaymentService worldlinePaymentService;
    private Converter<WorldlinePaymentInfoModel, WorldlinePaymentInfoData> worldlinePaymentInfoConverter;

    @Override
    public List<WorldlinePaymentInfoData> getWorldlinePaymentInfos(boolean saved) {
        final CustomerModel currentCustomer = checkoutCustomerStrategy.getCurrentUserForCheckout();
        final List<WorldlinePaymentInfoModel> worldlinePaymentInfos = worldlineCustomerAccountService.getWorldlinePaymentInfos(currentCustomer, saved);
        final List<WorldlinePaymentInfoData> worldlinePaymentInfoDataList = new ArrayList<>();

        for (final WorldlinePaymentInfoModel worldlinePaymentInfoModel : worldlinePaymentInfos) {
            final WorldlinePaymentInfoData paymentInfoData = worldlinePaymentInfoConverter.convert(worldlinePaymentInfoModel);
            worldlinePaymentInfoDataList.add(paymentInfoData);
        }
        return worldlinePaymentInfoDataList;
    }

    @Override
    public List<String> getSavedTokens() {
        final List<WorldlinePaymentInfoData> worldlinePaymentInfos = getWorldlinePaymentInfos(true);
        return worldlinePaymentInfos.stream().map(WorldlinePaymentInfoData::getToken).collect(Collectors.toList());
    }

    @Override
    public List<String> getSavedTokensForPaymentMethod(Integer paymentMethodId) { //return list of unique tokens
        final List<WorldlinePaymentInfoData> worldlinePaymentInfos = getWorldlinePaymentInfos(true);
        return worldlinePaymentInfos.stream()
                .filter(worldlinePaymentInfoData -> worldlinePaymentInfoData.getId().equals(paymentMethodId))
                .map(WorldlinePaymentInfoData::getToken)
                .collect(Collectors.toList());
    }

    @Override
    public WorldlinePaymentInfoData getWorldlinePaymentInfoByToken(String token) {
        final CustomerModel currentCustomer = checkoutCustomerStrategy.getCurrentUserForCheckout();
        final WorldlinePaymentInfoModel worldlinePaymentInfoByToken = worldlineCustomerAccountService.getWorldlinePaymentInfoByToken(currentCustomer, token, true);
        if (worldlinePaymentInfoByToken != null) {
            return worldlinePaymentInfoConverter.convert(worldlinePaymentInfoByToken);
        }
        return null;
    }

    @Override
    public void saveWorldlinePaymentInfo(TokenResponse tokenResponse, PaymentProduct paymentProduct) {
        final CustomerModel currentCustomer = checkoutCustomerStrategy.getCurrentUserForCheckout();
        WorldlinePaymentInfoModel worldlinePaymentInfoByToken = worldlineCustomerAccountService.getWorldlinePaymentInfoByToken(
                currentCustomer,
                tokenResponse.getId(),
                true);
        if (worldlinePaymentInfoByToken == null) {
            worldlinePaymentInfoByToken = modelService.create(WorldlinePaymentInfoModel.class);
            worldlinePaymentInfoByToken.setCode(String.valueOf(UUID.randomUUID()));
            worldlinePaymentInfoByToken.setUser(currentCustomer);
        }
        worldlinePaymentInfoByToken.setId(tokenResponse.getPaymentProductId());
        final CardWithoutCvv cardWithoutCvv = tokenResponse.getCard().getData().getCardWithoutCvv();
        worldlinePaymentInfoByToken.setCardholderName(cardWithoutCvv.getCardholderName());
        worldlinePaymentInfoByToken.setAlias(cardWithoutCvv.getCardNumber());
        worldlinePaymentInfoByToken.setExpiryDate(String.join("/", EXPIRY_DATE_PATTERN.split(cardWithoutCvv.getExpiryDate())));

        worldlinePaymentInfoByToken.setToken(tokenResponse.getId());
        worldlinePaymentInfoByToken.setCardBrand(paymentProduct.getDisplayHints().getLabel());

        worldlinePaymentInfoByToken.setSaved(true);

        modelService.save(worldlinePaymentInfoByToken);
    }

    @Override
    public void deleteSavedWorldlinePaymentInfo(String code) {
        validateParameterNotNullStandardMessage("code", code);
        final CustomerModel currentCustomer = checkoutCustomerStrategy.getCurrentUserForCheckout();
        final WorldlinePaymentInfoModel worldlinePaymentInfoModel = worldlineCustomerAccountService.getWorldlinePaymentInfos(currentCustomer, true)
                .stream()
                .filter(paymentInfo -> code.equals(paymentInfo.getCode()))
                .findFirst()
                .orElseThrow(() -> new ModelNotFoundException("Failed to find WorldlinePaymentInfo for the given code : " + code));

        worldlinePaymentService.deleteToken(worldlinePaymentInfoModel.getToken());
        modelService.remove(worldlinePaymentInfoModel);

    }

    public void setCheckoutCustomerStrategy(CheckoutCustomerStrategy checkoutCustomerStrategy) {
        this.checkoutCustomerStrategy = checkoutCustomerStrategy;
    }

    public void setWorldlineCustomerAccountService(WorldlineCustomerAccountService worldlineCustomerAccountService) {
        this.worldlineCustomerAccountService = worldlineCustomerAccountService;
    }

    public void setWorldlinePaymentService(WorldlinePaymentService worldlinePaymentService) {
        this.worldlinePaymentService = worldlinePaymentService;
    }

    public void setWorldlinePaymentInfoConverter(Converter<WorldlinePaymentInfoModel, WorldlinePaymentInfoData> worldlinePaymentInfoConverter) {
        this.worldlinePaymentInfoConverter = worldlinePaymentInfoConverter;
    }

    public void setModelService(ModelService modelService) {
        this.modelService = modelService;
    }
}
