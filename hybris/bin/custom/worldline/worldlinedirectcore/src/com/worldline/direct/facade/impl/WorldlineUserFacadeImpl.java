package com.worldline.direct.facade.impl;

import com.ingenico.direct.domain.CardWithoutCvv;
import com.ingenico.direct.domain.PaymentProduct;
import com.ingenico.direct.domain.TokenResponse;
import com.worldline.direct.enums.WorldlineCheckoutTypesEnum;
import com.worldline.direct.facade.WorldlineUserFacade;
import com.worldline.direct.order.data.WorldlinePaymentInfoData;
import com.worldline.direct.service.WorldlineCustomerAccountService;
import com.worldline.direct.service.WorldlinePaymentService;
import de.hybris.platform.commerceservices.strategies.CheckoutCustomerStrategy;
import de.hybris.platform.core.model.order.payment.PaymentInfoModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.servicelayer.exceptions.ModelNotFoundException;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.commerceservices.customer.CustomerAccountService;
import org.apache.commons.lang.BooleanUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNullStandardMessage;

public class WorldlineUserFacadeImpl implements WorldlineUserFacade {

    private static final String EXPIRY_DATE_REGEX = "(?<=\\G..)";
    private static final Pattern EXPIRY_DATE_PATTERN = Pattern.compile(EXPIRY_DATE_REGEX);
    private ModelService modelService;
    private CheckoutCustomerStrategy checkoutCustomerStrategy;
    private WorldlineCustomerAccountService worldlineCustomerAccountService;
    private WorldlinePaymentService worldlinePaymentService;
    private Converter<WorldlinePaymentInfoModel, WorldlinePaymentInfoData> worldlinePaymentInfoConverter;
    private CustomerAccountService customerAccountService;
    @Override
    public List<WorldlinePaymentInfoData> getWorldlinePaymentInfos(boolean saved) {
        final CustomerModel currentCustomer = checkoutCustomerStrategy.getCurrentUserForCheckout();
        final List<WorldlinePaymentInfoModel> worldlinePaymentInfos = worldlineCustomerAccountService.getWorldlinePaymentInfos(currentCustomer, saved);
        final List<WorldlinePaymentInfoData> worldlinePaymentInfoDataList = new ArrayList<>();

        for (final WorldlinePaymentInfoModel worldlinePaymentInfoModel : worldlinePaymentInfos) {
            final WorldlinePaymentInfoData paymentInfoData = worldlinePaymentInfoConverter.convert(worldlinePaymentInfoModel);
            paymentInfoData.setDefaultPayment(BooleanUtils.isTrue(worldlinePaymentInfoModel.equals(currentCustomer.getDefaultPaymentInfo())));
            worldlinePaymentInfoDataList.add(paymentInfoData);
        }
        return worldlinePaymentInfoDataList;
    }

    @Override
    public List<WorldlinePaymentInfoData> getWorldlinePaymentInfosForPaymentProducts(List<PaymentProduct> paymentProducts, boolean saved) {
        final CustomerModel currentCustomer = checkoutCustomerStrategy.getCurrentUserForCheckout();
        final List<WorldlinePaymentInfoModel> worldlinePaymentInfos = worldlineCustomerAccountService.getWorldlinePaymentInfos(currentCustomer, saved);
        final List<WorldlinePaymentInfoData> worldlinePaymentInfoDataList = new ArrayList<>();

        for (final WorldlinePaymentInfoModel worldlinePaymentInfoModel : worldlinePaymentInfos) {
            Optional<PaymentProduct> optionalPaymentProduct = paymentProducts.stream().filter(paymentProduct -> paymentProduct.getId().equals(worldlinePaymentInfoModel.getId())).findFirst();
            if (optionalPaymentProduct.isPresent()) {
                final WorldlinePaymentInfoData paymentInfoData = worldlinePaymentInfoConverter.convert(worldlinePaymentInfoModel);
                paymentInfoData.setPaymentMethodImageUrl(optionalPaymentProduct.get().getDisplayHints().getLogo());
                worldlinePaymentInfoDataList.add(paymentInfoData);
            }
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
    public WorldlinePaymentInfoData getWorldlinePaymentInfoByCode(String code) {
        final CustomerModel currentCustomer = checkoutCustomerStrategy.getCurrentUserForCheckout();
        final WorldlinePaymentInfoModel worldlinePaymentInfoById = worldlineCustomerAccountService.getWorldlinePaymentInfoByCode(currentCustomer, code);
        final PaymentInfoModel defaultPaymentInfoModel = currentCustomer.getDefaultPaymentInfo();

        if (worldlinePaymentInfoById != null) {
            WorldlinePaymentInfoData worldlinePaymentInfoData = worldlinePaymentInfoConverter.convert(worldlinePaymentInfoById);
            worldlinePaymentInfoData.setDefaultPayment(BooleanUtils.isTrue(worldlinePaymentInfoById.equals(defaultPaymentInfoModel)));
            return worldlinePaymentInfoData;
        }
        return null;
    }

    @Override
    public void saveWorldlinePaymentInfo(WorldlineCheckoutTypesEnum checkoutType, TokenResponse tokenResponse, PaymentProduct paymentProduct) {
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
        worldlinePaymentInfoByToken.setId(tokenResponse.getPaymentProductId());
        worldlinePaymentInfoByToken.setSaved(true);
        worldlinePaymentInfoByToken.setWorldlineCheckoutType(checkoutType);
        worldlinePaymentInfoByToken.setPaymentMethod(paymentProduct.getPaymentMethod());
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

    @Override
    public void setDefaultPaymentInfo(WorldlinePaymentInfoData paymentInfoData) {
        validateParameterNotNullStandardMessage("paymentInfoData", paymentInfoData);
        final CustomerModel currentCustomer = checkoutCustomerStrategy.getCurrentUserForCheckout();;
        final WorldlinePaymentInfoModel worldlinePaymentInfoModel =  worldlineCustomerAccountService.getWorldlinePaymentInfoByCode(currentCustomer, paymentInfoData.getCode());
        if (worldlinePaymentInfoModel != null)
        {
             customerAccountService.setDefaultPaymentInfo(currentCustomer, worldlinePaymentInfoModel);
        }
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

    public void setCustomerAccountService(CustomerAccountService customerAccountService) {
        this.customerAccountService = customerAccountService;
    }

    public void setModelService(ModelService modelService) {
        this.modelService = modelService;
    }
}
