package com.worldline.direct.facade.impl;

import com.onlinepayments.ApiException;
import com.onlinepayments.domain.*;
import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import com.worldline.direct.enums.WorldlineCheckoutTypesEnum;
import com.worldline.direct.enums.WorldlineRecurringPaymentStatus;
import com.worldline.direct.enums.WorldlineRecurringType;
import com.worldline.direct.enums.WorldlineReplenishmentOccurrenceEnum;
import com.worldline.direct.exception.WorldlineNonAuthorizedPaymentException;
import com.worldline.direct.exception.WorldlineNonValidPaymentProductException;
import com.worldline.direct.exception.WorldlineNonValidReturnMACException;
import com.worldline.direct.facade.WorldlineCheckoutFacade;
import com.worldline.direct.facade.WorldlineUserFacade;
import com.worldline.direct.model.WorldlineConfigurationModel;
import com.worldline.direct.model.WorldlineMandateModel;
import com.worldline.direct.order.data.BrowserData;
import com.worldline.direct.order.data.WorldlineHostedTokenizationData;
import com.worldline.direct.order.data.WorldlinePaymentInfoData;
import com.worldline.direct.service.*;
import com.worldline.direct.util.WorldlinePaymentProductUtils;
import com.worldline.direct.util.WorldlineUrlUtils;
import de.hybris.platform.b2bacceleratorfacades.checkout.data.PlaceOrderData;
import de.hybris.platform.commercefacades.order.CheckoutFacade;
import de.hybris.platform.commercefacades.order.data.AbstractOrderData;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commercefacades.order.data.PickupOrderEntryGroupData;
import de.hybris.platform.commercefacades.product.data.PriceData;
import de.hybris.platform.commercefacades.storelocator.data.PointOfServiceData;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.commercefacades.user.data.CountryData;
import de.hybris.platform.commerceservices.customer.CustomerAccountService;
import de.hybris.platform.commerceservices.order.CommerceCheckoutService;
import de.hybris.platform.commerceservices.service.data.CommerceCheckoutParameter;
import de.hybris.platform.commerceservices.strategies.CheckoutCustomerStrategy;
import de.hybris.platform.core.enums.PaymentStatus;
import de.hybris.platform.core.model.c2l.C2LItemModel;
import de.hybris.platform.core.model.c2l.CountryModel;
import de.hybris.platform.core.model.c2l.LanguageModel;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.payment.PaymentInfoModel;
import de.hybris.platform.core.model.order.payment.PaymentModeModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.core.model.user.TitleModel;
import de.hybris.platform.cronjob.enums.DayOfWeek;
import de.hybris.platform.order.CartService;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.order.PaymentModeService;
import de.hybris.platform.payment.enums.PaymentTransactionType;
import de.hybris.platform.payment.model.PaymentTransactionModel;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.user.UserService;
import de.hybris.platform.store.BaseStoreModel;
import de.hybris.platform.store.services.BaseStoreService;
import de.hybris.platform.util.localization.Localization;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.bouncycastle.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import java.util.*;
import java.util.stream.Collectors;

import static com.worldline.direct.constants.WorldlinedirectcoreConstants.PAYMENT_METHOD_GROUP_CARDS;
import static com.worldline.direct.constants.WorldlinedirectcoreConstants.PAYMENT_METHOD_HTP;

public class WorldlineCheckoutFacadeImpl implements WorldlineCheckoutFacade {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldlineCheckoutFacadeImpl.class);

    protected CommonI18NService commonI18NService;
    protected ModelService modelService;
    private List<String> virtualPaymentModes=new ArrayList<>();

    protected Converter<AddressData, AddressModel> addressReverseConverter;
    protected Converter<OrderModel, OrderData> orderConverter;
    protected Converter<CartModel, PlaceOrderData> worldlinePlaceOrderConverter;

    protected CartService cartService;
    protected UserService userService;
    protected CheckoutFacade checkoutFacade;
    protected CommerceCheckoutService commerceCheckoutService;
    protected BaseStoreService baseStoreService;
    protected CustomerAccountService customerAccountService;
    protected WorldlineCustomerAccountService worldlineCustomerAccountService;
    protected CheckoutCustomerStrategy checkoutCustomerStrategy;
    private PaymentModeService paymentModeService;

    protected WorldlineUserFacade worldlineUserFacade;
    protected WorldlinePaymentService worldlinePaymentService;
    protected WorldlineTransactionService worldlineTransactionService;
    protected WorldlineScheduleOrderService worldlineScheduleOrderService;
    protected WorldlineBusinessProcessService worldlineBusinessProcessService;

    protected WorldlineConfigurationService worldlineConfigurationService;

    @Override
    public List<PaymentProduct> getAvailablePaymentMethods() {
        final CartData cartData = checkoutFacade.getCheckoutCart();

        final PriceData totalPrice = cartData.getTotalPrice();
        List<PaymentProduct> paymentProducts = worldlinePaymentService.getPaymentProducts(totalPrice.getValue(),
                totalPrice.getCurrencyIso(),
                getCountryCode(cartData),
                getShopperLocale(),
                cartData.isReplenishmentOrder());

        return paymentProducts;
    }
    @Override
    public Boolean checkForCardPaymentMethods(List<PaymentProduct> paymentProducts) {
        for (PaymentProduct paymentProduct : paymentProducts) {
            if (StringUtils.equals(paymentProduct.getPaymentMethod(), WorldlinedirectcoreConstants.PAYMENT_METHOD_TYPE.CARD.getValue())) {
                return Boolean.TRUE;
            }
        }

        return Boolean.FALSE;
    }

    @Override
    public PaymentProduct getPaymentMethodById(int paymentId) {
        if (paymentId == PAYMENT_METHOD_HTP) {
            return createHtpGroupedCardPaymentProduct();
        } else if (paymentId == WorldlinedirectcoreConstants.PAYMENT_METHOD_HCP) {
            return createHcpGroupedCardPaymentProduct();
        } else if (paymentId== WorldlinedirectcoreConstants.PAYMENT_METHOD_GROUP_CARDS)
        {
            return createGroupCartPaymentProduct();
        }
        final CartData cartData = checkoutFacade.getCheckoutCart();
        final PriceData totalPrice = cartData.getTotalPrice();

        return worldlinePaymentService.getPaymentProduct(paymentId,
                totalPrice.getValue(),
                totalPrice.getCurrencyIso(),
                getCountryCode(cartData),
                getShopperLocale());
    }

    @Override
    public CreateHostedTokenizationResponse createHostedTokenization() {
        final List<WorldlinePaymentInfoData> worldlinePaymentInfos = worldlineUserFacade.getWorldlinePaymentInfos(true);
        final List<String> savedTokens = worldlinePaymentInfos.stream().map(WorldlinePaymentInfoData::getToken).collect(Collectors.toList());
        final CreateHostedTokenizationResponse hostedTokenization = worldlinePaymentService.createHostedTokenization(getShopperLocale(), savedTokens, userService.isAnonymousUser(userService.getCurrentUser()));
        hostedTokenization.setPartialRedirectUrl(WorldlineUrlUtils.buildFullURL(hostedTokenization.getPartialRedirectUrl()));
        if (CollectionUtils.isNotEmpty(hostedTokenization.getInvalidTokens())) {
            LOGGER.warn("[ WORLDLINE ] invalid tokens : {}", hostedTokenization.getInvalidTokens());
            worldlinePaymentInfos.stream()
                    .filter(infoData -> hostedTokenization.getInvalidTokens().contains(infoData.getToken()))
                    .map(WorldlinePaymentInfoData::getCode)
                    .forEach(code -> worldlineUserFacade.deleteSavedWorldlinePaymentInfo(code));
        }
        return hostedTokenization;
    }

    @Override
    public List<DirectoryEntry> getIdealIssuers(List<PaymentProduct> paymentProducts) {
        final boolean isIdealPresent = paymentProducts.stream()
                .anyMatch(paymentProduct -> WorldlinedirectcoreConstants.PAYMENT_METHOD_IDEAL == paymentProduct.getId());

        if (isIdealPresent) {
            final CartData cartData = checkoutFacade.getCheckoutCart();
            try {
                return worldlinePaymentService.getProductDirectoryEntries(WorldlinedirectcoreConstants.PAYMENT_METHOD_IDEAL, cartData.getTotalPrice().getCurrencyIso(), WorldlinedirectcoreConstants.PAYMENT_METHOD_IDEAL_COUNTRY);
            } catch (ApiException e) {
                LOGGER.info("[ WORLDLINE ] No ProductDirectory found! reason : {}", e.getResponseBody());
            }
        }
        return Collections.emptyList();
    }

    @Override
    public void fillWorldlinePaymentInfoData(final WorldlinePaymentInfoData worldlinePaymentInfoData,
                                             String savedPaymentCode,
                                             Integer paymentId,
                                             String paymentDirId,
                                             String hostedTokenizationId) throws WorldlineNonValidPaymentProductException {

        final PaymentProduct paymentProduct = getPaymentMethodById(paymentId);
        if (BooleanUtils.isTrue(isValidPaymentMethod(paymentProduct))) {
            worldlinePaymentInfoData.setSavedPayment(StringUtils.defaultString(savedPaymentCode,StringUtils.EMPTY));
            worldlinePaymentInfoData.setId(paymentProduct.getId());
            worldlinePaymentInfoData.setPaymentMethod(paymentProduct.getPaymentMethod());
            if (paymentId == PAYMENT_METHOD_HTP) {
                worldlinePaymentInfoData.setHostedTokenizationId(hostedTokenizationId);
                worldlinePaymentInfoData.setWorldlineCheckoutType(WorldlineCheckoutTypesEnum.HOSTED_TOKENIZATION);
                worldlinePaymentInfoData.setPaymentProductDirectoryId(StringUtils.EMPTY);
            } else if (paymentId == WorldlinedirectcoreConstants.PAYMENT_METHOD_IDEAL) {
                worldlinePaymentInfoData.setPaymentProductDirectoryId(paymentDirId);
                worldlinePaymentInfoData.setHostedTokenizationId(StringUtils.EMPTY);
                worldlinePaymentInfoData.setWorldlineCheckoutType(WorldlineCheckoutTypesEnum.HOSTED_TOKENIZATION);
            } else {
                worldlinePaymentInfoData.setHostedTokenizationId(StringUtils.EMPTY);
                worldlinePaymentInfoData.setPaymentProductDirectoryId(StringUtils.EMPTY);
                worldlinePaymentInfoData.setWorldlineCheckoutType(WorldlineCheckoutTypesEnum.HOSTED_CHECKOUT);
            }
        } else {
            throw new WorldlineNonValidPaymentProductException(paymentId);
        }
    }

    @Override
    public void authorisePaymentForHostedTokenization(String orderCode, WorldlineHostedTokenizationData worldlineHostedTokenizationData) throws WorldlineNonAuthorizedPaymentException, InvalidCartException {
        final OrderModel orderForCode = customerAccountService.getOrderForCode(orderCode, baseStoreService.getCurrentBaseStore());
        final CreatePaymentResponse paymentForHostedTokenization = worldlinePaymentService.createPaymentForHostedTokenization(orderForCode, worldlineHostedTokenizationData);

        cleanHostedCheckoutId();
        final PaymentResponse payment = paymentForHostedTokenization.getPayment();
        savePaymentTokenIfNeeded(WorldlineCheckoutTypesEnum.HOSTED_TOKENIZATION, payment);
        if (paymentForHostedTokenization.getMerchantAction() != null) {
            storeReturnMac(orderForCode, paymentForHostedTokenization.getMerchantAction().getRedirectData().getRETURNMAC());
            throw new WorldlineNonAuthorizedPaymentException(payment,
                    paymentForHostedTokenization.getMerchantAction(),
                    WorldlinedirectcoreConstants.UNAUTHORIZED_REASON.NEED_3DS);
        }
        handlePaymentResponse(orderForCode, payment);

    }


    public void handle3dsResponse(String orderCode, String paymentId, Boolean isRecurring) throws WorldlineNonAuthorizedPaymentException, InvalidCartException {
        final OrderModel orderForCode = customerAccountService.getOrderForCode(orderCode, baseStoreService.getCurrentBaseStore());
        final PaymentResponse payment = worldlinePaymentService.getPayment(paymentId);

        if (isRecurring) {
            saveSurchargeData(orderForCode, payment);
            savePaymentToken(orderForCode, payment, Boolean.TRUE, orderForCode.getSchedulingCronJob().getCode());
            saveMandateIfNeeded(orderForCode.getStore().getUid(), (WorldlinePaymentInfoModel) orderForCode.getSchedulingCronJob().getPaymentInfo(), payment);
        }
        handlePaymentResponse(orderForCode, payment);
    }

    @Override
    public CreateHostedCheckoutResponse createHostedCheckout(String orderCode, BrowserData browserData) throws InvalidCartException {
        final OrderModel orderForCode = customerAccountService.getOrderForCode(orderCode, baseStoreService.getCurrentBaseStore());
        final CreateHostedCheckoutResponse hostedCheckout = worldlinePaymentService.createHostedCheckout(orderForCode, browserData);
        storeReturnMac(orderForCode, hostedCheckout.getRETURNMAC());
        hostedCheckout.setPartialRedirectUrl(WorldlineUrlUtils.buildFullURL(hostedCheckout.getPartialRedirectUrl()));
        return hostedCheckout;
    }


    @Override
    public void authorisePaymentForHostedCheckout(String orderCode, String hostedCheckoutId, Boolean isRecurring) throws WorldlineNonAuthorizedPaymentException, InvalidCartException {
        GetHostedCheckoutResponse hostedCheckoutData = worldlinePaymentService.getHostedCheckout(hostedCheckoutId);
        final OrderModel orderForCode = customerAccountService.getOrderForCode(orderCode, baseStoreService.getCurrentBaseStore());
        switch (WorldlinedirectcoreConstants.HOSTED_CHECKOUT_STATUS_ENUM.valueOf(hostedCheckoutData.getStatus())) {
            case CANCELLED_BY_CONSUMER:
            case CLIENT_NOT_ELIGIBLE_FOR_SELECTED_PAYMENT_PRODUCT:
                cancelOrder(orderForCode);
                throw new WorldlineNonAuthorizedPaymentException(WorldlinedirectcoreConstants.UNAUTHORIZED_REASON.CANCELLED);
            case IN_PROGRESS:
                throw new WorldlineNonAuthorizedPaymentException(WorldlinedirectcoreConstants.UNAUTHORIZED_REASON.IN_PROGRESS);
            case PAYMENT_CREATED:
                if (hostedCheckoutData.getCreatedPaymentOutput().getPayment().getStatusOutput().getStatusCode() == 55) { // there was partial payment of the order
                    cancelOrder(orderForCode);
                    throw new WorldlineNonAuthorizedPaymentException(WorldlinedirectcoreConstants.UNAUTHORIZED_REASON.CANCELLED);
                }
                saveSurchargeData(orderForCode.getSchedulingCronJob() != null ? orderForCode.getSchedulingCronJob().getCart() : orderForCode, hostedCheckoutData.getCreatedPaymentOutput().getPayment());
                savePaymentToken(orderForCode, hostedCheckoutData.getCreatedPaymentOutput().getPayment(), isRecurring, isRecurring ? orderForCode.getSchedulingCronJob().getCode() : StringUtils.EMPTY);
                saveMandateIfNeeded(orderForCode.getStore().getUid(),(WorldlinePaymentInfoModel) orderForCode.getPaymentInfo(),hostedCheckoutData.getCreatedPaymentOutput().getPayment());
                handlePaymentResponse(orderForCode, hostedCheckoutData.getCreatedPaymentOutput().getPayment());

                break;
            default:
                LOGGER.error(String.format("Unexpected HostedCheckout Status value: %s", hostedCheckoutData.getStatus()));
                throw new IllegalStateException(String.format("Unexpected HostedCheckout Status value: %s", hostedCheckoutData.getStatus()));
        }
    }

    protected void savePaymentToken(AbstractOrderModel orderModel, PaymentResponse paymentData, Boolean isRecurring, String cronjobId) {
        WorldlinePaymentInfoModel paymentInfoModel = (WorldlinePaymentInfoModel) orderModel.getPaymentInfo();
        if (paymentData.getPaymentOutput().getCardPaymentMethodSpecificOutput() != null) {
            if (isRecurring) {
                final TokenResponse tokenResponse = worldlinePaymentService.getToken(
                    paymentData.getPaymentOutput().getCardPaymentMethodSpecificOutput().getToken());
                worldlineUserFacade.updateWorldlinePaymentInfo(paymentInfoModel, tokenResponse, cronjobId, baseStoreService.getCurrentBaseStore().getUid());
                modelService.refresh(orderModel);
            } else {
                savePaymentTokenIfNeeded(WorldlineCheckoutTypesEnum.HOSTED_CHECKOUT, paymentData);
            }

        }
    }

    protected void saveSurchargeData(AbstractOrderModel orderModel, PaymentResponse paymentResponse) {
        if (paymentResponse.getPaymentOutput().getSurchargeSpecificOutput() != null) {
            SurchargeSpecificOutput surchargeSpecificOutput = paymentResponse.getPaymentOutput().getSurchargeSpecificOutput();
            worldlineTransactionService.savePaymentCost(orderModel, surchargeSpecificOutput.getSurchargeAmount());
        }
    }

    protected void saveMandateIfNeeded(String storeId, WorldlinePaymentInfoModel worldlinePaymentInfoModel, PaymentResponse paymentResponse) {
        SepaDirectDebitPaymentMethodSpecificOutput sepaDirectDebitPaymentMethodSpecificOutput = paymentResponse.getPaymentOutput().getSepaDirectDebitPaymentMethodSpecificOutput();
        if (WorldlinePaymentProductUtils.isPaymentBySepaDirectDebit(worldlinePaymentInfoModel)  && sepaDirectDebitPaymentMethodSpecificOutput != null && StringUtils.isNotEmpty(sepaDirectDebitPaymentMethodSpecificOutput.getPaymentProduct771SpecificOutput().getMandateReference())){
            GetMandateResponse mandate = worldlinePaymentService.getMandate(sepaDirectDebitPaymentMethodSpecificOutput.getPaymentProduct771SpecificOutput().getMandateReference());
            if (mandate != null) {
                worldlinePaymentInfoModel.setMandateDetail(createMandate(mandate.getMandate(),storeId));
                MandatePersonalName personalName = mandate.getMandate().getCustomer().getPersonalInformation().getName();
                worldlinePaymentInfoModel.setCardholderName(personalName.getFirstName() + " " + personalName.getSurname());
                modelService.save(worldlinePaymentInfoModel);
            }
        }
    }

    private void cancelOrder(AbstractOrderModel orderForCode) {
        orderForCode.setPaymentStatus(PaymentStatus.WORLDLINE_CANCELED);
        modelService.save(orderForCode);
        modelService.refresh(orderForCode);
        worldlineBusinessProcessService.triggerOrderProcessEvent(orderForCode, WorldlinedirectcoreConstants.WORLDLINE_EVENT_PAYMENT);
    }

    private WorldlineMandateModel createMandate(MandateResponse mandateResponse, String storeId) {
        WorldlineMandateModel worldlineMandateModel = modelService.create(WorldlineMandateModel.class);
        try {
            WorldlineRecurringPaymentStatus worldlineRecurringPaymentStatus = WorldlineRecurringPaymentStatus.valueOf(mandateResponse.getStatus());
            worldlineMandateModel.setStatus(worldlineRecurringPaymentStatus);
        } catch (IllegalArgumentException e) {
            worldlineMandateModel.setStatus(WorldlineRecurringPaymentStatus.UNKNOWN);
        }
        worldlineMandateModel.setAlias(mandateResponse.getAlias());
        worldlineMandateModel.setStoreId(storeId);
        worldlineMandateModel.setUniqueMandateReference(mandateResponse.getUniqueMandateReference());
        worldlineMandateModel.setCustomerReference(mandateResponse.getCustomerReference());
        try {
            WorldlineRecurringType worldlineRecurringType = WorldlineRecurringType.valueOf(mandateResponse.getRecurrenceType());
            worldlineMandateModel.setRecurrenceType(worldlineRecurringType);
        } catch (IllegalArgumentException e) {
            worldlineMandateModel.setRecurrenceType(WorldlineRecurringType.UNKNOWN);
        }
        MandateCustomer customer = mandateResponse.getCustomer();
        if (customer != null) {
            worldlineMandateModel.setCompanyName(customer.getCompanyName());
            if (customer.getPersonalInformation() != null) {

                if (StringUtils.isNotEmpty(customer.getPersonalInformation().getTitle())) {
                    try {
                        TitleModel title = userService.getTitleForCode(Strings.toLowerCase(customer.getPersonalInformation().getTitle()));
                        worldlineMandateModel.setTitle(title);
                    } catch (Exception e) {
                        LOGGER.error(String.format("no title found for %s", customer.getPersonalInformation().getTitle()));
                    }
                }
                if (customer.getPersonalInformation().getName() != null) {
                    worldlineMandateModel.setFirstName(customer.getPersonalInformation().getName().getFirstName());
                    worldlineMandateModel.setLastName(customer.getPersonalInformation().getName().getSurname());
                }

            }
            if (customer.getBankAccountIban() != null) {
                worldlineMandateModel.setIban(customer.getBankAccountIban().getIban());
            }
            if (customer.getContactDetails() != null) {
                worldlineMandateModel.setEmailAddress(customer.getContactDetails().getEmailAddress());
            }
            if (customer.getMandateAddress() != null) {
                MandateAddress mandateAddress = customer.getMandateAddress();
                worldlineMandateModel.setCity(mandateAddress.getCity());
                worldlineMandateModel.setStreet(mandateAddress.getStreet());
                worldlineMandateModel.setZip(mandateAddress.getZip());
                worldlineMandateModel.setHouseNumber(mandateAddress.getHouseNumber());
                if (StringUtils.isNotEmpty(mandateAddress.getCountryCode())) {
                    try {
                        CountryModel country = commonI18NService.getCountry(mandateAddress.getCountryCode());
                        worldlineMandateModel.setCountry(country);
                    } catch (UnknownIdentifierException e) {
                        LOGGER.error("no country found for code :" + mandateAddress.getCountryCode());
                    }
                }
            }
        }
        worldlineMandateModel.setCustomer(checkoutCustomerStrategy.getCurrentUserForCheckout());
        modelService.save(worldlineMandateModel);
        return worldlineMandateModel;
    }

    @Override
    public void validateReturnMAC(AbstractOrderData orderDetails, String returnMAC) throws WorldlineNonValidReturnMACException {
        if (orderDetails == null || orderDetails.getWorldlinePaymentInfo() == null
                || !StringUtils.equals(returnMAC, orderDetails.getWorldlinePaymentInfo().getReturnMAC())) {
            throw new WorldlineNonValidReturnMACException(returnMAC);
        }
    }


    @Override
    public void handlePaymentInfo(WorldlinePaymentInfoData worldlinePaymentInfoData) {

        final CartModel cartModel = getCart();
        final CommerceCheckoutParameter parameter = new CommerceCheckoutParameter();
        parameter.setEnableHooks(Boolean.TRUE);
        parameter.setCart(cartModel);
        if (WorldlineCheckoutTypesEnum.HOSTED_TOKENIZATION.equals(getWorldlineCheckoutType())) {
            if (cartModel.isWorldlineReplenishmentOrder()) {
                if (worldlineConfigurationService.getCurrentWorldlineConfiguration().isFirstRecurringPayment()) {
                    calculateSurcharge(cartModel, worldlinePaymentInfoData.getHostedTokenizationId(), StringUtils.EMPTY, worldlinePaymentInfoData.getSavedPayment(), worldlinePaymentInfoData.getPaymentMethod());
                }
            } else {
                calculateSurcharge(cartModel, worldlinePaymentInfoData.getHostedTokenizationId(), StringUtils.EMPTY, worldlinePaymentInfoData.getSavedPayment(), worldlinePaymentInfoData.getPaymentMethod());
            }
        }
        parameter.setPaymentInfo(updateOrCreatePaymentInfo(cartModel, worldlinePaymentInfoData));

        commerceCheckoutService.setPaymentInfo(parameter);
    }

    @Override
    public boolean isTemporaryToken(String hostedTokenizationID) {
        GetHostedTokenizationResponse hostedTokenizationResponse = worldlinePaymentService.getHostedTokenization(hostedTokenizationID);
        return hostedTokenizationResponse.getToken().getIsTemporary();
    }

    public void calculateSurcharge(AbstractOrderModel cartModel, String hostedTokenizationID, String token, String savedPaymentInfoId, String paymentMethodType) {
        final WorldlineConfigurationModel currentWorldlineConfiguration = worldlineConfigurationService.getCurrentWorldlineConfiguration();
        if (currentWorldlineConfiguration.isApplySurcharge() &&
              StringUtils.equals( WorldlinedirectcoreConstants.PAYMENT_METHOD_TYPE.CARD.getValue(), paymentMethodType)) {
            CalculateSurchargeResponse surchargeResponse;
            if (StringUtils.isNotEmpty(savedPaymentInfoId)) {
                final CustomerModel currentCustomer = checkoutCustomerStrategy.getCurrentUserForCheckout();
                WorldlinePaymentInfoModel savedPaymentInfo = worldlineCustomerAccountService.getWorldlinePaymentInfoByCode(currentCustomer, savedPaymentInfoId);
                surchargeResponse = worldlinePaymentService.calculateSurcharge(hostedTokenizationID, savedPaymentInfo.getToken(), cartModel);
            } else {
                surchargeResponse = worldlinePaymentService.calculateSurcharge(hostedTokenizationID, token, cartModel);
            }
            AmountOfMoney surcharge = surchargeResponse.getSurcharges().get(0).getSurchargeAmount();
            worldlineTransactionService.savePaymentCost(cartModel, surcharge);
        }
    }


    public void handlePaymentResponse(OrderModel orderModel, PaymentResponse paymentResponse) throws WorldlineNonAuthorizedPaymentException, InvalidCartException {
        switch (WorldlinedirectcoreConstants.PAYMENT_STATUS_ENUM.valueOf(paymentResponse.getStatus())) {
            case CREATED:
            case REJECTED:
            case REJECTED_CAPTURE:
                worldlineTransactionService.createAuthorizationPaymentTransaction(orderModel,
                        paymentResponse.getPaymentOutput().getReferences().getMerchantReference(),
                        paymentResponse.getId(),
                        paymentResponse.getStatus(),
                        paymentResponse.getPaymentOutput().getAmountOfMoney());
                throw new WorldlineNonAuthorizedPaymentException(paymentResponse, WorldlinedirectcoreConstants.UNAUTHORIZED_REASON.REJECTED);
            case CANCELLED:
                worldlineTransactionService.createAuthorizationPaymentTransaction(orderModel,
                        paymentResponse.getPaymentOutput().getReferences().getMerchantReference(),
                        paymentResponse.getId(),
                        paymentResponse.getStatus(),
                        paymentResponse.getPaymentOutput().getAmountOfMoney());
                throw new WorldlineNonAuthorizedPaymentException(WorldlinedirectcoreConstants.UNAUTHORIZED_REASON.CANCELLED);
            case REDIRECTED:
            case PENDING_PAYMENT:
            case PENDING_COMPLETION:
            case PENDING_CAPTURE:
            case AUTHORIZATION_REQUESTED:
            case CAPTURE_REQUESTED:
                updateOrderFromPaymentResponse(orderModel, paymentResponse, PaymentTransactionType.AUTHORIZATION);
                break;
            case CAPTURED:
                updateOrderFromPaymentResponse(orderModel, paymentResponse, PaymentTransactionType.CAPTURE);
                break;
            default:
                LOGGER.warn(String.format("Unexpected value: %s", paymentResponse.getStatus()));
                throw new IllegalStateException("Unexpected value: " + paymentResponse.getStatus());
        }
    }

    protected void updateOrderFromPaymentResponse(AbstractOrderModel orderModel, final PaymentResponse paymentResponse, PaymentTransactionType paymentTransactionType) {
        updatePaymentInfoIfNeeded(orderModel, paymentResponse);
        if (paymentResponse.getPaymentOutput().getCardPaymentMethodSpecificOutput() != null) {
            updatePaymentMode(paymentResponse.getPaymentOutput().getCardPaymentMethodSpecificOutput().getPaymentProductId().toString(), orderModel);
        }
        AmountOfMoney transactionAmount = paymentResponse.getPaymentOutput().getAcquiredAmount() != null ? paymentResponse.getPaymentOutput().getAcquiredAmount() : paymentResponse.getPaymentOutput().getAmountOfMoney();
        if (paymentResponse.getPaymentOutput().getSurchargeSpecificOutput() != null) {
            SurchargeSpecificOutput surchargeSpecificOutput = paymentResponse.getPaymentOutput().getSurchargeSpecificOutput();
            worldlineTransactionService.saveSurchargeData(orderModel, surchargeSpecificOutput);
        }


        final PaymentTransactionModel paymentTransaction = worldlineTransactionService.getOrCreatePaymentTransaction(orderModel,
                paymentResponse.getPaymentOutput().getReferences().getMerchantReference(),
                paymentResponse.getId());

        worldlineTransactionService.updatePaymentTransaction(
                paymentTransaction,
                paymentResponse.getId(),
                paymentResponse.getStatus(),
                transactionAmount,
                paymentTransactionType
        );

        cartService.removeSessionCart();
        cartService.getSessionCart();
        modelService.refresh(orderModel);
    }


    @Override
    public WorldlineCheckoutTypesEnum getWorldlineCheckoutType() {
        final BaseStoreModel currentBaseStore = baseStoreService.getCurrentBaseStore();
        if (currentBaseStore != null) {
            return currentBaseStore.getWorldlineCheckoutType();
        }
        return null;
    }

    @Override
    public void saveReplenishmentData(boolean replenishmentOrder, String replenishmentStartDate, String replenishmentEndDate, String nDays, String nWeeks,
          String nMonths, String nthDayOfMonth, List<String> nDaysOfWeek, String replenishmentRecurrence) {
        saveReplenishmentData(replenishmentOrder,
              StringUtils.isNotEmpty(replenishmentStartDate) ? new Date(replenishmentStartDate) : null,
              StringUtils.isNotEmpty(replenishmentEndDate) ? new Date(replenishmentEndDate) : null,
              nDays,
              nWeeks,
              nMonths,
              nthDayOfMonth,
              nDaysOfWeek,
              replenishmentRecurrence);
    }

    @Override
    public void saveReplenishmentData(boolean replenishmentOrder, Date replenishmentStartDate, Date replenishmentEndDate, String nDays, String nWeeks,
          String nMonths, String nthDayOfMonth, List<String> nDaysOfWeek, String replenishmentRecurrence) {
        CartModel cartModel = getCart();
        cartModel.setWorldlineReplenishmentOrder(replenishmentOrder);
        if (replenishmentOrder) {
            WorldlineReplenishmentOccurrenceEnum replenishmentOccurrenceEnum = WorldlineReplenishmentOccurrenceEnum.valueOf(replenishmentRecurrence);
            cartModel.setWorldlineReplenishmentStartDate(replenishmentStartDate);
            cartModel.setWorldlineReplenishmentEndDate(replenishmentEndDate);
            cartModel.setWorldlineReplenishmentRecurrence(replenishmentOccurrenceEnum);
            cartModel.setWorldlineNDays(nDays);
            cartModel.setWorldlineNWeeks(nWeeks);
            cartModel.setWorldlineNDaysOfWeek(replenishmentOrderDaysOfWeek(nDaysOfWeek));
            cartModel.setWorldlineNMonths(nMonths);
            cartModel.setWorldlineNthDayOfMonth(nthDayOfMonth);
        }
        if (cartModel.getPaymentInfo() instanceof WorldlinePaymentInfoModel) {
            WorldlinePaymentInfoModel paymentInfo = (WorldlinePaymentInfoModel) cartModel.getPaymentInfo();
            paymentInfo.setRecurringToken(replenishmentOrder);
            modelService.save(paymentInfo);
        }

        modelService.save(cartModel);
    }

    @Override
    public PlaceOrderData prepareOrderPlacementData() {
        return worldlinePlaceOrderConverter.convert(getCart());
    }

    private List<DayOfWeek> replenishmentOrderDaysOfWeek(List<String> nDaysOfWeek) {
        List<DayOfWeek> result = new ArrayList<>();
        if (nDaysOfWeek.size() > 0) {
            for (String dayOfWeek : nDaysOfWeek) {
                result.add(DayOfWeek.valueOf(dayOfWeek));
            }
        }
        return result;
    }

    protected PaymentInfoModel updateOrCreatePaymentInfo(final CartModel cartModel, WorldlinePaymentInfoData worldlinePaymentInfoData) {
        WorldlinePaymentInfoModel paymentInfo;
        if (cartModel.getPaymentInfo() instanceof WorldlinePaymentInfoModel) {
            paymentInfo = (WorldlinePaymentInfoModel) cartModel.getPaymentInfo();
        } else {
            paymentInfo = modelService.create(WorldlinePaymentInfoModel.class);
            paymentInfo.setCode(generatePaymentInfoCode(cartModel));
            paymentInfo.setUser(cartModel.getUser());
            paymentInfo.setSaved(false);
        }
            paymentInfo.setPaymentMethod(worldlinePaymentInfoData.getPaymentMethod());
            paymentInfo.setPaymentProductDirectoryId(worldlinePaymentInfoData.getPaymentProductDirectoryId());
            paymentInfo.setHostedTokenizationId(worldlinePaymentInfoData.getHostedTokenizationId());
            paymentInfo.setWorldlineCheckoutType(worldlinePaymentInfoData.getWorldlineCheckoutType());
            AddressModel billingAddress = convertToAddressModel(worldlinePaymentInfoData.getBillingAddress());
            paymentInfo.setBillingAddress(billingAddress);
            billingAddress.setOwner(paymentInfo);
            String paymentModeCode = StringUtils.EMPTY;
        if (StringUtils.isNotBlank(worldlinePaymentInfoData.getSavedPayment()))
        {
            final CustomerModel currentCustomer = checkoutCustomerStrategy.getCurrentUserForCheckout();
            final WorldlinePaymentInfoModel savedPaymentInfo = worldlineCustomerAccountService.getWorldlinePaymentInfoByCode(currentCustomer, worldlinePaymentInfoData.getSavedPayment());
            paymentInfo.setUsedSavedPayment(savedPaymentInfo);
            paymentInfo.setId(savedPaymentInfo.getId());
            paymentInfo.setToken(savedPaymentInfo.getToken());
            paymentModeCode = String.valueOf(savedPaymentInfo.getId());
        } else {
            paymentInfo.setUsedSavedPayment(null);
            paymentInfo.setToken(null);
            paymentInfo.setId(worldlinePaymentInfoData.getId());
            paymentModeCode = String.valueOf(worldlinePaymentInfoData.getId());
        }
        updatePaymentMode(paymentModeCode, cartModel);
        modelService.save(paymentInfo);
        return paymentInfo;
    }

    private void updatePaymentMode(String paymentModeCode, AbstractOrderModel abstractOrderModel) {
        if (!virtualPaymentModes.contains(paymentModeCode)) {

            Double keepPaymentCost = abstractOrderModel.getPaymentCost();
            PaymentModeModel paymentMode = paymentModeService.getPaymentModeForCode(paymentModeCode);
            abstractOrderModel.setPaymentMode(paymentMode);
            modelService.save(abstractOrderModel);
            if (worldlineConfigurationService.getCurrentWorldlineConfiguration().isApplySurcharge() || abstractOrderModel instanceof OrderModel) {
                worldlineTransactionService.savePaymentCost(abstractOrderModel, keepPaymentCost);
            }
        }
    }

    protected void updatePaymentInfoIfNeeded(final AbstractOrderModel orderModel, PaymentResponse paymentResponse) {
        if (orderModel.getPaymentInfo() instanceof WorldlinePaymentInfoModel) {
            final WorldlinePaymentInfoModel paymentInfo = (WorldlinePaymentInfoModel) orderModel.getPaymentInfo();
            final PaymentOutput paymentOutput = paymentResponse.getPaymentOutput();
            if (paymentOutput.getCardPaymentMethodSpecificOutput() != null && (paymentInfo.getId().equals(PAYMENT_METHOD_HTP) || paymentInfo.getId().equals(PAYMENT_METHOD_GROUP_CARDS))) {
                paymentInfo.setId(paymentOutput.getCardPaymentMethodSpecificOutput().getPaymentProductId());
                if (paymentInfo.isRecurringToken()) {
                    // update cart so the recurring payments for subscription made by HTP to have valid payment method
                    AbstractOrderModel cartModel = cartService.getSessionCart();
                    ((WorldlinePaymentInfoModel)cartModel.getPaymentInfo()).setId(paymentOutput.getCardPaymentMethodSpecificOutput().getPaymentProductId());
                    modelService.save(cartModel);
                }
                modelService.save(paymentInfo);
                modelService.refresh(orderModel);
            }
        }
    }

    protected AddressModel convertToAddressModel(AddressData addressData) {
        final AddressModel addressModel = modelService.create(AddressModel.class);
        addressReverseConverter.convert(addressData, addressModel);

        return addressModel;
    }

    protected String generatePaymentInfoCode(CartModel cartModel) {
        return cartModel.getCode() + "|" + UUID.randomUUID();
    }

    protected String getShopperLocale() {
        final LanguageModel currentLanguage = commonI18NService.getCurrentLanguage();
        if (currentLanguage != null) {
            return commonI18NService.getLocaleForLanguage(currentLanguage).toString();
        }
        return Locale.ENGLISH.toString();
    }

    protected String getCountryCode(CartData cartData) {
        AddressData deliveryAddress = cartData.getDeliveryAddress();
        if (deliveryAddress != null) {
            CountryData deliveryCountry = deliveryAddress.getCountry();
            if (deliveryCountry != null) {
                return deliveryCountry.getIsocode();
            }
        }
        if (cartData.getPickupItemsQuantity() > 0) {
            final List<PickupOrderEntryGroupData> pickupOrderGroups = cartData.getPickupOrderGroups();
            final Optional<String> pickupCountry = pickupOrderGroups.stream()
                    .map(PickupOrderEntryGroupData::getDeliveryPointOfService)
                    .filter(Objects::nonNull)
                    .map(PointOfServiceData::getAddress)
                    .filter(Objects::nonNull)
                    .map(AddressData::getCountry)
                    .filter(Objects::nonNull)
                    .map(CountryData::getIsocode)
                    .findFirst();
            if (pickupCountry.isPresent()) {
                return pickupCountry.get();
            }
        }
        final BaseStoreModel currentBaseStore = baseStoreService.getCurrentBaseStore();
        if (currentBaseStore != null) {
            final Optional<String> firstBillingCountry = currentBaseStore.getBillingCountries()
                    .stream()
                    .map(C2LItemModel::getIsocode)
                    .findFirst();
            if (firstBillingCountry.isPresent()) {
                return firstBillingCountry.get();
            }
        }

        return null;
    }

    protected CartModel getCart() {
        return cartService.hasSessionCart() ? cartService.getSessionCart() : null;
    }

    protected void savePaymentTokenIfNeeded(WorldlineCheckoutTypesEnum checkoutType, PaymentResponse paymentResponse) {

        String token = null;
        final String paymentMethod = paymentResponse.getPaymentOutput().getPaymentMethod();
        switch (WorldlinedirectcoreConstants.PAYMENT_METHOD_TYPE.fromString(paymentMethod)) {
            case CARD:
                final CardPaymentMethodSpecificOutput cardPaymentMethodSpecificOutput = paymentResponse.getPaymentOutput().getCardPaymentMethodSpecificOutput();
                token = cardPaymentMethodSpecificOutput.getToken();
                break;
            case REDIRECT:
                final RedirectPaymentMethodSpecificOutput redirectPaymentMethodSpecificOutput = paymentResponse.getPaymentOutput().getRedirectPaymentMethodSpecificOutput();
                token = redirectPaymentMethodSpecificOutput.getToken();
                break;
            case DIRECT_DEBIT:
            default:
                return;
        }

        if (token == null) {
            LOGGER.debug("[WORLDLINE] no token to save!");
            return;
        }
        final TokenResponse tokenResponse = worldlinePaymentService.getToken(token);
        if (tokenResponse != null && (!WorldlineCheckoutTypesEnum.HOSTED_TOKENIZATION.equals(checkoutType) || BooleanUtils.isFalse(tokenResponse.getIsTemporary()))) {
            final PaymentProduct paymentProduct = getPaymentMethodById(tokenResponse.getPaymentProductId());
            worldlineUserFacade.saveWorldlinePaymentInfo(checkoutType, tokenResponse, paymentProduct);

        }
    }

    private PaymentProduct createHtpGroupedCardPaymentProduct() {
        PaymentProduct paymentProduct = new PaymentProduct();
        paymentProduct.setId(PAYMENT_METHOD_HTP);
        paymentProduct.setPaymentMethod(WorldlinedirectcoreConstants.PAYMENT_METHOD_TYPE.CARD.getValue());
        paymentProduct.setDisplayHints(new PaymentProductDisplayHints());
        paymentProduct.getDisplayHints().setLabel(Localization.getLocalizedString("type.payment.byCard"));
        return paymentProduct;
    }

    private PaymentProduct createHcpGroupedCardPaymentProduct() {
        PaymentProduct paymentProduct = new PaymentProduct();
        paymentProduct.setId(WorldlinedirectcoreConstants.PAYMENT_METHOD_HCP);
        paymentProduct.setPaymentMethod(WorldlinedirectcoreConstants.PAYMENT_METHOD_TYPE.CARD.getValue());
        paymentProduct.setDisplayHints(new PaymentProductDisplayHints());
        paymentProduct.getDisplayHints().setLabel("");
        return paymentProduct;
    }

    private PaymentProduct createGroupCartPaymentProduct() {
        PaymentProduct paymentProduct = new PaymentProduct();
        paymentProduct.setId(WorldlinedirectcoreConstants.PAYMENT_METHOD_GROUP_CARDS);
        paymentProduct.setPaymentMethod(WorldlinedirectcoreConstants.PAYMENT_METHOD_TYPE.CARD.getValue());
        paymentProduct.setDisplayHints(new PaymentProductDisplayHints());
        paymentProduct.getDisplayHints().setLabel(Localization.getLocalizedString("type.payment.groupedCards"));
        WorldlineConfigurationModel configuration = worldlineConfigurationService.getCurrentWorldlineConfiguration();
        if (configuration.getGroupCardsLogo() != null) {
            paymentProduct.getDisplayHints().setLogo(configuration.getGroupCardsLogo().getUrl());
        }
        return paymentProduct;
    }


    private Boolean isValidPaymentMethod(PaymentProduct paymentProduct) {
        final WorldlineCheckoutTypesEnum worldlineCheckoutType = getWorldlineCheckoutType();
        if (WorldlineCheckoutTypesEnum.HOSTED_TOKENIZATION.equals(worldlineCheckoutType)) {
            return paymentProduct.getId() >= 0 || paymentProduct.getId() == PAYMENT_METHOD_HTP;
        }
        return true;
    }

    protected void storeReturnMac(AbstractOrderModel abstractOrderModel, String returnMAC) {
        final WorldlinePaymentInfoModel paymentInfo = (WorldlinePaymentInfoModel) abstractOrderModel.getPaymentInfo();//
        paymentInfo.setReturnMAC(returnMAC);
        modelService.save(paymentInfo);
    }


    protected void cleanHostedCheckoutId() throws InvalidCartException {
        final CartModel cart = getCart();
        if (cart == null || !(cart.getPaymentInfo() instanceof WorldlinePaymentInfoModel)) {
            throw new InvalidCartException("Invalid cart while storing ReturnMAC");
        }
        final WorldlinePaymentInfoModel paymentInfo = (WorldlinePaymentInfoModel) cart.getPaymentInfo();//
        paymentInfo.setHostedTokenizationId(null);
        modelService.save(paymentInfo);
    }

    public void setWorldlinePlaceOrderConverter(Converter<CartModel, PlaceOrderData> worldlinePlaceOrderConverter) {
        this.worldlinePlaceOrderConverter = worldlinePlaceOrderConverter;
    }

    public void setCommonI18NService(CommonI18NService commonI18NService) {
        this.commonI18NService = commonI18NService;
    }

    public void setCheckoutFacade(CheckoutFacade checkoutFacade) {
        this.checkoutFacade = checkoutFacade;
    }

    public void setWorldlinePaymentService(WorldlinePaymentService worldlinePaymentService) {
        this.worldlinePaymentService = worldlinePaymentService;
    }

    public void setCommerceCheckoutService(CommerceCheckoutService commerceCheckoutService) {
        this.commerceCheckoutService = commerceCheckoutService;
    }

    public void setCartService(CartService cartService) {
        this.cartService = cartService;
    }

    public void setModelService(ModelService modelService) {
        this.modelService = modelService;
    }

    public void setAddressReverseConverter(Converter<AddressData, AddressModel> addressReverseConverter) {
        this.addressReverseConverter = addressReverseConverter;
    }

    public void setBaseStoreService(BaseStoreService baseStoreService) {
        this.baseStoreService = baseStoreService;
    }

    public void setWorldlineUserFacade(WorldlineUserFacade worldlineUserFacade) {
        this.worldlineUserFacade = worldlineUserFacade;
    }

    public void setWorldlineTransactionService(WorldlineTransactionService worldlineTransactionService) {
        this.worldlineTransactionService = worldlineTransactionService;
    }

    public void setCustomerAccountService(CustomerAccountService customerAccountService) {
        this.customerAccountService = customerAccountService;
    }

    public void setOrderConverter(Converter<OrderModel, OrderData> orderConverter) {
        this.orderConverter = orderConverter;
    }

    public void setWorldlineBusinessProcessService(WorldlineBusinessProcessService worldlineBusinessProcessService) {
        this.worldlineBusinessProcessService = worldlineBusinessProcessService;
    }


    public void setWorldlineCustomerAccountService(WorldlineCustomerAccountService worldlineCustomerAccountService) {
        this.worldlineCustomerAccountService = worldlineCustomerAccountService;
    }

    public void setCheckoutCustomerStrategy(CheckoutCustomerStrategy checkoutCustomerStrategy) {
        this.checkoutCustomerStrategy = checkoutCustomerStrategy;
    }

    public void setPaymentModeService(PaymentModeService paymentModeService) {
        this.paymentModeService = paymentModeService;
    }

    @Required
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public void setVirtualPaymentModes(List<String> virtualPaymentModes) {
        this.virtualPaymentModes = virtualPaymentModes;
    }

    public void setWorldlineConfigurationService(WorldlineConfigurationService worldlineConfigurationService) {
        this.worldlineConfigurationService = worldlineConfigurationService;
    }

    public void setWorldlineScheduleOrderService(WorldlineScheduleOrderService worldlineScheduleOrderService) {
        this.worldlineScheduleOrderService = worldlineScheduleOrderService;
    }
}
