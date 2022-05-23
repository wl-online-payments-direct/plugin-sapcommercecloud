package com.worldline.direct.facade.impl;

import com.ingenico.direct.ApiException;
import com.ingenico.direct.domain.*;
import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import com.worldline.direct.enums.WorldlineCheckoutTypesEnum;
import com.worldline.direct.exception.WorldlineNonAuthorizedPaymentException;
import com.worldline.direct.exception.WorldlineNonValidPaymentProductException;
import com.worldline.direct.exception.WorldlineNonValidReturnMACException;
import com.worldline.direct.facade.WorldlineCheckoutFacade;
import com.worldline.direct.facade.WorldlineUserFacade;
import com.worldline.direct.order.data.BrowserData;
import com.worldline.direct.order.data.WorldlineHostedTokenizationData;
import com.worldline.direct.order.data.WorldlinePaymentInfoData;
import com.worldline.direct.service.*;
import com.worldline.direct.util.WorldlineUrlUtils;
import de.hybris.platform.commercefacades.order.CheckoutFacade;
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
import de.hybris.platform.core.model.c2l.LanguageModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.payment.PaymentInfoModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.order.CartService;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.payment.enums.PaymentTransactionType;
import de.hybris.platform.payment.model.PaymentTransactionModel;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.store.BaseStoreModel;
import de.hybris.platform.store.services.BaseStoreService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class WorldlineCheckoutFacadeImpl implements WorldlineCheckoutFacade {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldlineCheckoutFacadeImpl.class);

    private CommonI18NService commonI18NService;
    private ModelService modelService;

    private Converter<AddressData, AddressModel> addressReverseConverter;
    private Converter<OrderModel, OrderData> orderConverter;

    private CartService cartService;
    private CheckoutFacade checkoutFacade;
    private CommerceCheckoutService commerceCheckoutService;
    private BaseStoreService baseStoreService;
    private CustomerAccountService customerAccountService;
    private WorldlineCustomerAccountService worldlineCustomerAccountService;
    private CheckoutCustomerStrategy checkoutCustomerStrategy;

    private WorldlineUserFacade worldlineUserFacade;
    private WorldlinePaymentService worldlinePaymentService;
    private WorldlineTransactionService worldlineTransactionService;
    private WorldlineBusinessProcessService worldlineBusinessProcessService;

    @Override
    public List<PaymentProduct> getAvailablePaymentMethods() {
        final CartData cartData = checkoutFacade.getCheckoutCart();

        final PriceData totalPrice = cartData.getTotalPrice();
        List<PaymentProduct> paymentProducts = worldlinePaymentService.getPaymentProducts(totalPrice.getValue(),
                totalPrice.getCurrencyIso(),
                getCountryCode(cartData),
                getShopperLocale());

        return paymentProducts;
    }


    @Override
    public PaymentProduct getPaymentMethodById(int paymentId) {
        if (paymentId == WorldlinedirectcoreConstants.PAYMENT_METHOD_HTP) {
            return createHtpGroupedCardPaymentProduct();
        } else if (paymentId == WorldlinedirectcoreConstants.PAYMENT_METHOD_HCP) {
            return createHcpGroupedCardPaymentProduct();
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
        final CreateHostedTokenizationResponse hostedTokenization = worldlinePaymentService.createHostedTokenization(getShopperLocale(), savedTokens);
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
                                             int paymentId,
                                             String paymentDirId,
                                             String hostedTokenizationId, String hostedCheckoutToken) throws WorldlineNonValidPaymentProductException {
        if (StringUtils.isNotEmpty(hostedCheckoutToken) && paymentId == WorldlinedirectcoreConstants.PAYMENT_METHOD_HCP) {
            final CustomerModel currentCustomer = checkoutCustomerStrategy.getCurrentUserForCheckout();
            final WorldlinePaymentInfoModel savedPaymentInfo = worldlineCustomerAccountService.getWorldlinePaymentInfoByToken(currentCustomer, hostedCheckoutToken, true);
            worldlinePaymentInfoData.setOriginal(savedPaymentInfo);
        } else {
            final PaymentProduct paymentProduct = getPaymentMethodById(paymentId);
            if (isValidPaymentMethod(paymentProduct)) {
                worldlinePaymentInfoData.setId(paymentProduct.getId());
                worldlinePaymentInfoData.setPaymentProductDirectoryId(paymentDirId);
                worldlinePaymentInfoData.setPaymentMethod(paymentProduct.getPaymentMethod());
                if (paymentId == WorldlinedirectcoreConstants.PAYMENT_METHOD_HTP || paymentId == WorldlinedirectcoreConstants.PAYMENT_METHOD_IDEAL) {
		            worldlinePaymentInfoData.setPaymentProductDirectoryId(paymentDirId);
                    worldlinePaymentInfoData.setHostedTokenizationId(hostedTokenizationId);
                    worldlinePaymentInfoData.setWorldlineCheckoutType(WorldlineCheckoutTypesEnum.HOSTED_TOKENIZATION);
                } else {
                    worldlinePaymentInfoData.setWorldlineCheckoutType(WorldlineCheckoutTypesEnum.HOSTED_CHECKOUT);
                }
            } else {
                throw new WorldlineNonValidPaymentProductException(paymentId);
            }
        }
    }

    @Override
    public OrderData authorisePaymentForHostedTokenization(String orderCode, WorldlineHostedTokenizationData worldlineHostedTokenizationData) throws WorldlineNonAuthorizedPaymentException, InvalidCartException {
        final OrderModel orderForCode = customerAccountService.getOrderForCode(orderCode, baseStoreService.getCurrentBaseStore());
        final CreatePaymentResponse paymentForHostedTokenization = worldlinePaymentService.createPaymentForHostedTokenization(orderForCode, worldlineHostedTokenizationData);

        cleanHostedCheckoutId();
        final PaymentResponse payment = paymentForHostedTokenization.getPayment();
        savePaymentTokenIfNeeded(payment, true);
        if (paymentForHostedTokenization.getMerchantAction() != null) {
            storeReturnMac(orderForCode, paymentForHostedTokenization.getMerchantAction().getRedirectData().getRETURNMAC());
            throw new WorldlineNonAuthorizedPaymentException(payment,
                    paymentForHostedTokenization.getMerchantAction(),
                    WorldlinedirectcoreConstants.UNAUTHORIZED_REASON.NEED_3DS);
        }
        return handlePaymentResponse(orderForCode, payment);

    }


    public OrderData handle3dsResponse(String orderCode, String paymentId) throws WorldlineNonAuthorizedPaymentException, InvalidCartException {
        final OrderModel orderForCode = customerAccountService.getOrderForCode(orderCode, baseStoreService.getCurrentBaseStore());
        final PaymentResponse payment = worldlinePaymentService.getPayment(paymentId);
        return handlePaymentResponse(orderForCode, payment);
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
    public OrderData authorisePaymentForHostedCheckout(String orderCode, String hostedCheckoutId) throws WorldlineNonAuthorizedPaymentException, InvalidCartException {
        GetHostedCheckoutResponse hostedCheckoutData = worldlinePaymentService.getHostedCheckout(hostedCheckoutId);
        final OrderModel orderForCode = customerAccountService.getOrderForCode(orderCode, baseStoreService.getCurrentBaseStore());
        switch (WorldlinedirectcoreConstants.HOSTED_CHECKOUT_STATUS_ENUM.valueOf(hostedCheckoutData.getStatus())) {
            case CANCELLED_BY_CONSUMER:
            case CLIENT_NOT_ELIGIBLE_FOR_SELECTED_PAYMENT_PRODUCT:
                orderForCode.setPaymentStatus(PaymentStatus.WORLDLINE_CANCELED);
                modelService.save(orderForCode);
                modelService.refresh(orderForCode);
                worldlineBusinessProcessService.triggerOrderProcessEvent(orderForCode, WorldlinedirectcoreConstants.WORLDLINE_EVENT_PAYMENT);
                throw new WorldlineNonAuthorizedPaymentException(WorldlinedirectcoreConstants.UNAUTHORIZED_REASON.CANCELLED);
            case IN_PROGRESS:
                throw new WorldlineNonAuthorizedPaymentException(WorldlinedirectcoreConstants.UNAUTHORIZED_REASON.IN_PROGRESS);
            case PAYMENT_CREATED:
                savePaymentTokenIfNeeded(hostedCheckoutData.getCreatedPaymentOutput().getPayment(), false);
                return handlePaymentResponse(orderForCode, hostedCheckoutData.getCreatedPaymentOutput().getPayment());
            default:
                LOGGER.error("Unexpected HostedCheckout Status value: " + hostedCheckoutData.getStatus());
                throw new IllegalStateException("Unexpected HostedCheckout Status value: " + hostedCheckoutData.getStatus());
        }
    }

    @Override
    public void validateReturnMAC(OrderData orderDetails, String returnMAC) throws WorldlineNonValidReturnMACException {
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
        parameter.setPaymentInfo(updateOrCreatePaymentInfo(cartModel, worldlinePaymentInfoData));

        commerceCheckoutService.setPaymentInfo(parameter);
    }


    protected OrderData handlePaymentResponse(OrderModel orderModel, PaymentResponse paymentResponse) throws WorldlineNonAuthorizedPaymentException, InvalidCartException {
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
                return updateOrderFromPaymentResponse(orderModel, paymentResponse, PaymentTransactionType.AUTHORIZATION);
            case CAPTURED:
                return updateOrderFromPaymentResponse(orderModel, paymentResponse, PaymentTransactionType.CAPTURE);
            default:
                LOGGER.warn("Unexpected value: " + paymentResponse.getStatus());
                throw new IllegalStateException("Unexpected value: " + paymentResponse.getStatus());
        }
    }

    protected OrderData updateOrderFromPaymentResponse(OrderModel orderModel, final PaymentResponse paymentResponse, PaymentTransactionType paymentTransactionType) throws InvalidCartException {
        updatePaymentInfoIfNeeded(orderModel, paymentResponse);
        final PaymentTransactionModel paymentTransaction = worldlineTransactionService.getOrCreatePaymentTransaction(orderModel,
                paymentResponse.getPaymentOutput().getReferences().getMerchantReference(),
                paymentResponse.getId());

        worldlineTransactionService.updatePaymentTransaction(
                paymentTransaction,
                paymentResponse.getId(),
                paymentResponse.getStatus(),
                paymentResponse.getPaymentOutput().getAmountOfMoney(),
                paymentTransactionType
        );
        cartService.removeSessionCart();
        modelService.refresh(orderModel);
        return orderConverter.convert(orderModel);
    }


    @Override
    public WorldlineCheckoutTypesEnum getWorldlineCheckoutType() {
        final BaseStoreModel currentBaseStore = baseStoreService.getCurrentBaseStore();
        if (currentBaseStore != null) {
            return currentBaseStore.getWorldlineCheckoutType();
        }
        return null;
    }

    protected PaymentInfoModel updateOrCreatePaymentInfo(final CartModel cartModel, WorldlinePaymentInfoData worldlinePaymentInfoData) {
        WorldlinePaymentInfoModel paymentInfo;
        if (Objects.nonNull(worldlinePaymentInfoData.getOriginal())) {
            paymentInfo = worldlinePaymentInfoData.getOriginal();
            AddressModel billingAddress = convertToAddressModel(worldlinePaymentInfoData.getBillingAddress());
            paymentInfo.setBillingAddress(billingAddress);
            billingAddress.setOwner(paymentInfo);

        } else {
            if (cartModel.getPaymentInfo() instanceof WorldlinePaymentInfoModel && BooleanUtils.isFalse(cartModel.getPaymentInfo().isSaved())) {
                paymentInfo = (WorldlinePaymentInfoModel) cartModel.getPaymentInfo();
            } else {
                paymentInfo = modelService.create(WorldlinePaymentInfoModel.class);
                paymentInfo.setCode(generatePaymentInfoCode(cartModel));
                paymentInfo.setUser(cartModel.getUser());
                paymentInfo.setSaved(false);
            }


            paymentInfo.setId(worldlinePaymentInfoData.getId());
            paymentInfo.setPaymentMethod(worldlinePaymentInfoData.getPaymentMethod());
            paymentInfo.setPaymentProductDirectoryId(worldlinePaymentInfoData.getPaymentProductDirectoryId());
            paymentInfo.setHostedTokenizationId(worldlinePaymentInfoData.getHostedTokenizationId());
            paymentInfo.setToken(worldlinePaymentInfoData.getToken());
            paymentInfo.setWorldlineCheckoutType(worldlinePaymentInfoData.getWorldlineCheckoutType());
            AddressModel billingAddress = convertToAddressModel(worldlinePaymentInfoData.getBillingAddress());
            paymentInfo.setBillingAddress(billingAddress);
            billingAddress.setOwner(paymentInfo);

            modelService.save(paymentInfo);
        }
        return paymentInfo;
    }

    protected void updatePaymentInfoIfNeeded(final OrderModel orderModel, PaymentResponse paymentResponse) {
        if (orderModel.getPaymentInfo() instanceof WorldlinePaymentInfoModel) {
            final WorldlinePaymentInfoModel paymentInfo = (WorldlinePaymentInfoModel) orderModel.getPaymentInfo();
            final PaymentOutput paymentOutput = paymentResponse.getPaymentOutput();
            if (paymentOutput.getCardPaymentMethodSpecificOutput() != null && paymentInfo.getId().equals(WorldlinedirectcoreConstants.PAYMENT_METHOD_HTP)) {
                paymentInfo.setId(paymentOutput.getCardPaymentMethodSpecificOutput().getPaymentProductId());
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

    private void savePaymentTokenIfNeeded(PaymentResponse paymentResponse, boolean isHTP) {

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
        }

        if (token == null) {
            LOGGER.debug("[WORLDLINE] no token to save!");
            return;
        }
        final TokenResponse tokenResponse = worldlinePaymentService.getToken(token);
        if (tokenResponse != null && (!isHTP || BooleanUtils.isFalse(tokenResponse.getIsTemporary()))) {
            final PaymentProduct paymentProduct = getPaymentMethodById(tokenResponse.getPaymentProductId());
            worldlineUserFacade.saveWorldlinePaymentInfo(tokenResponse, paymentProduct);

        }
    }

    private PaymentProduct createHtpGroupedCardPaymentProduct() {
        PaymentProduct paymentProduct = new PaymentProduct();
        paymentProduct.setId(WorldlinedirectcoreConstants.PAYMENT_METHOD_HTP);
        paymentProduct.setPaymentMethod(WorldlinedirectcoreConstants.PAYMENT_METHOD_TYPE.CARD.getValue());
        paymentProduct.setDisplayHints(new PaymentProductDisplayHints());
        paymentProduct.getDisplayHints().setLabel("Grouped Cards");
        return paymentProduct;
    }

    private PaymentProduct createHcpGroupedCardPaymentProduct() {
        PaymentProduct paymentProduct = new PaymentProduct();
        paymentProduct.setId(WorldlinedirectcoreConstants.PAYMENT_METHOD_HCP);
        paymentProduct.setPaymentMethod(WorldlinedirectcoreConstants.PAYMENT_METHOD_TYPE.CARD.getValue());
        paymentProduct.setDisplayHints(new PaymentProductDisplayHints());
        paymentProduct.getDisplayHints().setLabel("Grouped Cards");
        return paymentProduct;
    }

    private Boolean isValidPaymentMethod(PaymentProduct paymentProduct) {
        final WorldlineCheckoutTypesEnum worldlineCheckoutType = getWorldlineCheckoutType();
        if (WorldlineCheckoutTypesEnum.HOSTED_CHECKOUT.equals(worldlineCheckoutType)) {
            return paymentProduct.getId() >= 0 || paymentProduct.getId() == -1;
        }
        return true;
    }

    private void storeReturnMac(OrderModel orderForCode, String returnMAC) throws InvalidCartException {
        final WorldlinePaymentInfoModel paymentInfo = (WorldlinePaymentInfoModel) orderForCode.getPaymentInfo();//
        paymentInfo.setReturnMAC(returnMAC);
        modelService.save(paymentInfo);
    }


    private void cleanHostedCheckoutId() throws InvalidCartException {
        final CartModel cart = getCart();
        if (cart == null || !(cart.getPaymentInfo() instanceof WorldlinePaymentInfoModel)) {
            throw new InvalidCartException("Invalid cart while storing ReturnMAC");
        }
        final WorldlinePaymentInfoModel paymentInfo = (WorldlinePaymentInfoModel) cart.getPaymentInfo();//
        paymentInfo.setHostedTokenizationId(null);
        modelService.save(paymentInfo);
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
}
