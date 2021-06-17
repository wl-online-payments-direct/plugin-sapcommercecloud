package com.ingenico.ogone.direct.facade.impl;

import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_METHOD_BCC;
import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_METHOD_HTP;
import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_METHOD_IDEAL;
import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_METHOD_IDEAL_COUNTRY;
import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_STATUS_ENUM;
import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.UNAUTHORIZED_REASON;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import de.hybris.platform.commercefacades.order.CheckoutFacade;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commercefacades.product.data.PriceData;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.commercefacades.user.data.CountryData;
import de.hybris.platform.commerceservices.order.CommerceCheckoutService;
import de.hybris.platform.commerceservices.service.data.CommerceCheckoutParameter;
import de.hybris.platform.core.model.c2l.LanguageModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.payment.IngenicoPaymentInfoModel;
import de.hybris.platform.core.model.order.payment.PaymentInfoModel;
import de.hybris.platform.core.model.user.AddressModel;
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

import com.ingenico.direct.ApiException;
import com.ingenico.direct.domain.CardPaymentMethodSpecificOutput;
import com.ingenico.direct.domain.CreateHostedCheckoutResponse;
import com.ingenico.direct.domain.CreateHostedTokenizationResponse;
import com.ingenico.direct.domain.CreatePaymentResponse;
import com.ingenico.direct.domain.DirectoryEntry;
import com.ingenico.direct.domain.GetHostedCheckoutResponse;
import com.ingenico.direct.domain.GetHostedTokenizationResponse;
import com.ingenico.direct.domain.PaymentOutput;
import com.ingenico.direct.domain.PaymentProduct;
import com.ingenico.direct.domain.PaymentProductDisplayHints;
import com.ingenico.direct.domain.PaymentResponse;
import com.ingenico.direct.domain.RedirectPaymentMethodSpecificOutput;
import com.ingenico.direct.domain.TokenResponse;
import com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.HOSTED_CHECKOUT_STATUS_ENUM;
import com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_METHOD_TYPE;
import com.ingenico.ogone.direct.enums.IngenicoCheckoutTypesEnum;
import com.ingenico.ogone.direct.exception.IngenicoNonAuthorizedPaymentException;
import com.ingenico.ogone.direct.exception.IngenicoNonValidPaymentProductException;
import com.ingenico.ogone.direct.exception.IngenicoNonValidReturnMACException;
import com.ingenico.ogone.direct.facade.IngenicoCheckoutFacade;
import com.ingenico.ogone.direct.facade.IngenicoUserFacade;
import com.ingenico.ogone.direct.order.data.BrowserData;
import com.ingenico.ogone.direct.order.data.IngenicoHostedTokenizationData;
import com.ingenico.ogone.direct.order.data.IngenicoPaymentInfoData;
import com.ingenico.ogone.direct.service.IngenicoPaymentService;
import com.ingenico.ogone.direct.service.IngenicoTransactionService;
import com.ingenico.ogone.direct.util.IngenicoUrlUtils;

public class IngenicoCheckoutFacadeImpl implements IngenicoCheckoutFacade {
    private static final Logger LOGGER = LoggerFactory.getLogger(IngenicoCheckoutFacadeImpl.class);

    private CommonI18NService commonI18NService;
    private ModelService modelService;

    private Converter<AddressData, AddressModel> addressReverseConverter;

    private CartService cartService;
    private CheckoutFacade checkoutFacade;
    private CommerceCheckoutService commerceCheckoutService;
    private BaseStoreService baseStoreService;


    private IngenicoUserFacade ingenicoUserFacade;
    private IngenicoPaymentService ingenicoPaymentService;
    private IngenicoTransactionService ingenicoTransactionService;

    @Override
    public List<PaymentProduct> getAvailablePaymentMethods() {
        final CartData cartData = checkoutFacade.getCheckoutCart();

        final PriceData totalPrice = cartData.getTotalPrice();
        List<PaymentProduct> paymentProducts = ingenicoPaymentService.getPaymentProducts(totalPrice.getValue(),
                totalPrice.getCurrencyIso(),
                getCountryCode(cartData),
                getShopperLocale());

        paymentProducts = filterByCheckoutType(paymentProducts);

        return paymentProducts;
    }

    @Override
    public PaymentProduct getPaymentMethodById(int paymentId) {
        if (paymentId == PAYMENT_METHOD_HTP) {
            return createGroupedCardPaymentProduct();
        }
        final CartData cartData = checkoutFacade.getCheckoutCart();
        final PriceData totalPrice = cartData.getTotalPrice();

        return ingenicoPaymentService.getPaymentProduct(paymentId,
                totalPrice.getValue(),
                totalPrice.getCurrencyIso(),
                getCountryCode(cartData),
                getShopperLocale());
    }

    @Override
    public CreateHostedTokenizationResponse createHostedTokenization() {
        final List<IngenicoPaymentInfoData> ingenicoPaymentInfos = ingenicoUserFacade.getIngenicoPaymentInfos(true);
        final List<String> savedTokens = ingenicoPaymentInfos.stream().map(IngenicoPaymentInfoData::getToken).collect(Collectors.toList());
        final CreateHostedTokenizationResponse hostedTokenization = ingenicoPaymentService.createHostedTokenization(getShopperLocale(), savedTokens);
        hostedTokenization.setPartialRedirectUrl(IngenicoUrlUtils.buildFullURL(hostedTokenization.getPartialRedirectUrl()));
        if (CollectionUtils.isNotEmpty(hostedTokenization.getInvalidTokens())) {
            LOGGER.warn("[ INGENICO ] invalid tokens : {}", hostedTokenization.getInvalidTokens());
            ingenicoPaymentInfos.stream()
                    .filter(infoData -> hostedTokenization.getInvalidTokens().contains(infoData.getToken()))
                    .map(IngenicoPaymentInfoData::getCode)
                    .forEach(code -> ingenicoUserFacade.deleteSavedIngenicoPaymentInfo(code));
        }
        return hostedTokenization;
    }

    @Override
    public List<DirectoryEntry> getIdealIssuers(List<PaymentProduct> paymentProducts) {
        final boolean isIdealPresent = paymentProducts.stream()
                .anyMatch(paymentProduct -> PAYMENT_METHOD_IDEAL == paymentProduct.getId());

        if (isIdealPresent) {
            final CartData cartData = checkoutFacade.getCheckoutCart();
            try {
                return ingenicoPaymentService.getProductDirectoryEntries(PAYMENT_METHOD_IDEAL, cartData.getTotalPrice().getCurrencyIso(), PAYMENT_METHOD_IDEAL_COUNTRY);
            } catch (ApiException e) {
                LOGGER.info("[ INGENICO ] No ProductDirectory found! reason : {}", e.getResponseBody());
            }
        }
        return Collections.emptyList();
    }

    @Override
    public void fillIngenicoPaymentInfoData(final IngenicoPaymentInfoData ingenicoPaymentInfoData,
                                            int paymentId,
                                            String paymentDirId,
                                            String hostedTokenizationId) throws IngenicoNonValidPaymentProductException {
        final PaymentProduct paymentProduct = getPaymentMethodById(paymentId);
        if (isValidPaymentMethod(paymentProduct)) {
            ingenicoPaymentInfoData.setId(paymentProduct.getId());
            ingenicoPaymentInfoData.setPaymentProductDirectoryId(paymentDirId);
            ingenicoPaymentInfoData.setPaymentMethod(paymentProduct.getPaymentMethod());
            if (paymentId == PAYMENT_METHOD_HTP) {
                ingenicoPaymentInfoData.setHostedTokenizationId(hostedTokenizationId);
                ingenicoPaymentInfoData.setIngenicoCheckoutType(IngenicoCheckoutTypesEnum.HOSTED_TOKENIZATION);
            } else {
                ingenicoPaymentInfoData.setIngenicoCheckoutType(IngenicoCheckoutTypesEnum.HOSTED_CHECKOUT);
            }
        } else {
            throw new IngenicoNonValidPaymentProductException(paymentId);
        }

    }

    @Override
    public OrderData authorisePaymentForHostedTokenization(IngenicoHostedTokenizationData ingenicoHostedTokenizationData) throws IngenicoNonAuthorizedPaymentException, InvalidCartException {

        final GetHostedTokenizationResponse hostedTokenization = ingenicoPaymentService.getHostedTokenization(ingenicoHostedTokenizationData.getHostedTokenizationId());
        final CreatePaymentResponse paymentForHostedTokenization = ingenicoPaymentService.createPaymentForHostedTokenization(ingenicoHostedTokenizationData, hostedTokenization);
        cleanHostedCheckoutId();
        final PaymentResponse payment = paymentForHostedTokenization.getPayment();
        savePaymentTokenIfNeeded(payment, true);
        if (paymentForHostedTokenization.getMerchantAction() != null) {
            storeReturnMac(paymentForHostedTokenization.getMerchantAction().getRedirectData().getRETURNMAC());
            ingenicoTransactionService.createAuthorizationPaymentTransaction(cartService.getSessionCart(),
                    payment.getPaymentOutput().getReferences().getMerchantReference(),
                    payment.getId(),
                    payment.getStatus(),
                    payment.getPaymentOutput().getAmountOfMoney());
            throw new IngenicoNonAuthorizedPaymentException(payment,
                    paymentForHostedTokenization.getMerchantAction(),
                    UNAUTHORIZED_REASON.NEED_3DS);
        }
        return handlePaymentResponse(payment);

    }


    public OrderData handle3dsResponse(String ref, String returnMAC, String paymentId) throws IngenicoNonAuthorizedPaymentException, InvalidCartException {
        final PaymentResponse payment = ingenicoPaymentService.getPayment(paymentId);
        return handlePaymentResponse(payment);
    }

    @Override
    public CreateHostedCheckoutResponse createHostedCheckout(BrowserData browserData) throws InvalidCartException {
        final CreateHostedCheckoutResponse hostedCheckout = ingenicoPaymentService.createHostedCheckout(browserData);
        storeReturnMac(hostedCheckout.getRETURNMAC());
        hostedCheckout.setPartialRedirectUrl(IngenicoUrlUtils.buildFullURL(hostedCheckout.getPartialRedirectUrl()));
        return hostedCheckout;
    }



    @Override
    public OrderData authorisePaymentForHostedCheckout(String hostedCheckoutId) throws IngenicoNonAuthorizedPaymentException, InvalidCartException {
        GetHostedCheckoutResponse hostedCheckoutData = ingenicoPaymentService.getHostedCheckout(hostedCheckoutId);

        switch (HOSTED_CHECKOUT_STATUS_ENUM.valueOf(hostedCheckoutData.getStatus())) {
            case CANCELLED_BY_CONSUMER:

            case CLIENT_NOT_ELIGIBLE_FOR_SELECTED_PAYMENT_PRODUCT:
                throw new IngenicoNonAuthorizedPaymentException(UNAUTHORIZED_REASON.CANCELLED);
            case IN_PROGRESS:
                throw new IngenicoNonAuthorizedPaymentException(UNAUTHORIZED_REASON.IN_PROGRESS);
            case PAYMENT_CREATED:
                savePaymentTokenIfNeeded(hostedCheckoutData.getCreatedPaymentOutput().getPayment(), false);
                return handlePaymentResponse(hostedCheckoutData.getCreatedPaymentOutput().getPayment());
            default:
                LOGGER.error("Unexpected HostedCheckout Status value: " + hostedCheckoutData.getStatus());
                throw new IllegalStateException("Unexpected HostedCheckout Status value: " + hostedCheckoutData.getStatus());
        }
    }

    @Override
    public void validateReturnMAC(String returnMAC) throws IngenicoNonValidReturnMACException {
        final CartModel cart = getCart();
        if (cart == null || !(cart.getPaymentInfo() instanceof IngenicoPaymentInfoModel)
                || !StringUtils.equals(returnMAC, ((IngenicoPaymentInfoModel) cart.getPaymentInfo()).getReturnMAC())) {
            throw new IngenicoNonValidReturnMACException(returnMAC);
        }
    }


    @Override
    public void handlePaymentInfo(IngenicoPaymentInfoData ingenicoPaymentInfoData) {

        final CartModel cartModel = getCart();
        final CommerceCheckoutParameter parameter = new CommerceCheckoutParameter();
        parameter.setEnableHooks(Boolean.TRUE);
        parameter.setCart(cartModel);
        parameter.setPaymentInfo(updateOrCreatePaymentInfo(cartModel, ingenicoPaymentInfoData));

        commerceCheckoutService.setPaymentInfo(parameter);
    }


    protected OrderData handlePaymentResponse(PaymentResponse paymentResponse) throws IngenicoNonAuthorizedPaymentException, InvalidCartException {
        switch (PAYMENT_STATUS_ENUM.valueOf(paymentResponse.getStatus())) {
            case CREATED:
            case REJECTED:
            case REJECTED_CAPTURE:
                ingenicoTransactionService.createAuthorizationPaymentTransaction(cartService.getSessionCart(),
                        paymentResponse.getPaymentOutput().getReferences().getMerchantReference(),
                        paymentResponse.getId(),
                        paymentResponse.getStatus(),
                        paymentResponse.getPaymentOutput().getAmountOfMoney());
                throw new IngenicoNonAuthorizedPaymentException(paymentResponse, UNAUTHORIZED_REASON.REJECTED);
            case CANCELLED:
                ingenicoTransactionService.createAuthorizationPaymentTransaction(cartService.getSessionCart(),
                        paymentResponse.getPaymentOutput().getReferences().getMerchantReference(),
                        paymentResponse.getId(),
                        paymentResponse.getStatus(),
                        paymentResponse.getPaymentOutput().getAmountOfMoney());
                throw new IngenicoNonAuthorizedPaymentException(UNAUTHORIZED_REASON.CANCELLED);
            case REDIRECTED:
            case PENDING_PAYMENT:
            case PENDING_COMPLETION:
            case PENDING_CAPTURE:
            case AUTHORIZATION_REQUESTED:
            case CAPTURE_REQUESTED:
                return createOrderFromPaymentResponse(paymentResponse, PaymentTransactionType.AUTHORIZATION);
            case CAPTURED:
                return createOrderFromPaymentResponse(paymentResponse, PaymentTransactionType.CAPTURE);
            default:
                LOGGER.warn("Unexpected value: " + paymentResponse.getStatus());
                throw new IllegalStateException("Unexpected value: " + paymentResponse.getStatus());
        }
    }

    protected OrderData createOrderFromPaymentResponse(final PaymentResponse paymentResponse, PaymentTransactionType paymentTransactionType) throws InvalidCartException {
        final CartModel sessionCart = cartService.getSessionCart();
        updatePaymentInfoIfNeeded(sessionCart, paymentResponse);
        final PaymentTransactionModel paymentTransaction = ingenicoTransactionService.getOrCreatePaymentTransaction(sessionCart,
                paymentResponse.getPaymentOutput().getReferences().getMerchantReference(),
                paymentResponse.getId());

        ingenicoTransactionService.updatePaymentTransaction(
                paymentTransaction,
                paymentResponse.getId(),
                paymentResponse.getStatus(),
                paymentResponse.getPaymentOutput().getAmountOfMoney(),
                paymentTransactionType
        );

        return checkoutFacade.placeOrder();
    }

    protected List<PaymentProduct> filterByCheckoutType(List<PaymentProduct> paymentProducts) {
        final IngenicoCheckoutTypesEnum ingenicoCheckoutType = getIngenicoCheckoutType();
        if (ingenicoCheckoutType == null) {
            return paymentProducts;
        }
        switch (ingenicoCheckoutType) {
            case HOSTED_CHECKOUT:
                paymentProducts = paymentProducts.stream()
                        .filter(paymentProduct -> !PAYMENT_METHOD_TYPE.MOBILE.getValue().equals(paymentProduct.getPaymentMethod()))
                        .collect(Collectors.toList());
                break;
            case HOSTED_TOKENIZATION:
                final boolean isCardsPresent = paymentProducts.stream()
                        .anyMatch(paymentProduct -> PAYMENT_METHOD_TYPE.CARD.getValue().equals(paymentProduct.getPaymentMethod()));

                final Predicate<PaymentProduct> isCard = paymentProduct -> PAYMENT_METHOD_TYPE.CARD.getValue().equals(paymentProduct.getPaymentMethod());
                final Predicate<PaymentProduct> isBCMC = paymentProduct -> PAYMENT_METHOD_BCC == paymentProduct.getId();
                paymentProducts = paymentProducts.stream()
                        .filter(isCard.negate().or(isBCMC))
                        .collect(Collectors.toList());

                if (isCardsPresent) {
                    paymentProducts.add(0, createGroupedCardPaymentProduct());
                }
                break;
        }
        return paymentProducts;
    }

    @Override
    public IngenicoCheckoutTypesEnum getIngenicoCheckoutType() {
        final BaseStoreModel currentBaseStore = baseStoreService.getCurrentBaseStore();
        if (currentBaseStore != null) {
            return currentBaseStore.getIngenicoCheckoutType();
        }
        return null;
    }

    protected PaymentInfoModel updateOrCreatePaymentInfo(final CartModel cartModel, IngenicoPaymentInfoData ingenicoPaymentInfoData) {
        IngenicoPaymentInfoModel paymentInfo;
        if (cartModel.getPaymentInfo() instanceof IngenicoPaymentInfoModel) {
            paymentInfo = (IngenicoPaymentInfoModel) cartModel.getPaymentInfo();
        } else {
            paymentInfo = modelService.create(IngenicoPaymentInfoModel.class);
            paymentInfo.setCode(generatePaymentInfoCode(cartModel));
            paymentInfo.setUser(cartModel.getUser());
            paymentInfo.setSaved(false);
        }

        paymentInfo.setId(ingenicoPaymentInfoData.getId());
        paymentInfo.setPaymentMethod(ingenicoPaymentInfoData.getPaymentMethod());
        paymentInfo.setPaymentProductDirectoryId(ingenicoPaymentInfoData.getPaymentProductDirectoryId());
        paymentInfo.setHostedTokenizationId(ingenicoPaymentInfoData.getHostedTokenizationId());
        paymentInfo.setIngenicoCheckoutType(ingenicoPaymentInfoData.getIngenicoCheckoutType());

        AddressModel billingAddress = convertToAddressModel(ingenicoPaymentInfoData.getBillingAddress());
        paymentInfo.setBillingAddress(billingAddress);
        billingAddress.setOwner(paymentInfo);

        modelService.save(paymentInfo);
        return paymentInfo;
    }

    protected void updatePaymentInfoIfNeeded(final CartModel cartModel, PaymentResponse paymentResponse) {
        if (cartModel.getPaymentInfo() instanceof IngenicoPaymentInfoModel) {
            final IngenicoPaymentInfoModel paymentInfo = (IngenicoPaymentInfoModel) cartModel.getPaymentInfo();
            final PaymentOutput paymentOutput = paymentResponse.getPaymentOutput();
            if (paymentOutput.getCardPaymentMethodSpecificOutput() != null && paymentInfo.getId().equals(PAYMENT_METHOD_HTP)) {
                paymentInfo.setId(paymentOutput.getCardPaymentMethodSpecificOutput().getPaymentProductId());
                modelService.save(paymentInfo);
                modelService.refresh(cartModel);
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
        return null;
    }

    protected CartModel getCart() {
        return cartService.hasSessionCart() ? cartService.getSessionCart() : null;
    }

    private void savePaymentTokenIfNeeded(PaymentResponse paymentResponse, boolean isHTP) {

        String token = null;
        final String paymentMethod = paymentResponse.getPaymentOutput().getPaymentMethod();
        switch (PAYMENT_METHOD_TYPE.fromString(paymentMethod)) {
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
            LOGGER.debug("[INGENICO] no token to save!");
            return;
        }
        final TokenResponse tokenResponse = ingenicoPaymentService.getToken(token);
        if (tokenResponse != null && (!isHTP || BooleanUtils.isFalse(tokenResponse.getIsTemporary()))) {
            final PaymentProduct paymentProduct = getPaymentMethodById(tokenResponse.getPaymentProductId());
            ingenicoUserFacade.saveIngenicoPaymentInfo(tokenResponse, paymentProduct);

        }
    }

    private PaymentProduct createGroupedCardPaymentProduct() {
        PaymentProduct paymentProduct = new PaymentProduct();
        paymentProduct.setId(PAYMENT_METHOD_HTP);
        paymentProduct.setPaymentMethod(PAYMENT_METHOD_TYPE.CARD.getValue());
        paymentProduct.setDisplayHints(new PaymentProductDisplayHints());
        paymentProduct.getDisplayHints().setLabel("Grouped Cards");
        return paymentProduct;
    }

    private Boolean isValidPaymentMethod(PaymentProduct paymentProduct) {
        final IngenicoCheckoutTypesEnum ingenicoCheckoutType = getIngenicoCheckoutType();
        if (IngenicoCheckoutTypesEnum.HOSTED_CHECKOUT.equals(ingenicoCheckoutType)) {
            return paymentProduct.getId() >= 0;
        }
        return true;
    }

    private void storeReturnMac(String returnMAC) throws InvalidCartException {
        final CartModel cart = getCart();
        if (cart == null || !(cart.getPaymentInfo() instanceof IngenicoPaymentInfoModel)) {
            throw new InvalidCartException("Invalid cart while storing ReturnMAC");
        }
        final IngenicoPaymentInfoModel paymentInfo = (IngenicoPaymentInfoModel) cart.getPaymentInfo();//
        paymentInfo.setReturnMAC(returnMAC);
        modelService.save(paymentInfo);
    }


    private void cleanHostedCheckoutId() throws InvalidCartException {
        final CartModel cart = getCart();
        if (cart == null || !(cart.getPaymentInfo() instanceof IngenicoPaymentInfoModel)) {
            throw new InvalidCartException("Invalid cart while storing ReturnMAC");
        }
        final IngenicoPaymentInfoModel paymentInfo = (IngenicoPaymentInfoModel) cart.getPaymentInfo();//
        paymentInfo.setHostedTokenizationId(null);
        modelService.save(paymentInfo);
    }



    public void setCommonI18NService(CommonI18NService commonI18NService) {
        this.commonI18NService = commonI18NService;
    }

    public void setCheckoutFacade(CheckoutFacade checkoutFacade) {
        this.checkoutFacade = checkoutFacade;
    }

    public void setIngenicoPaymentService(IngenicoPaymentService ingenicoPaymentService) {
        this.ingenicoPaymentService = ingenicoPaymentService;
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

    public void setIngenicoUserFacade(IngenicoUserFacade ingenicoUserFacade) {
        this.ingenicoUserFacade = ingenicoUserFacade;
    }

    public void setIngenicoTransactionService(IngenicoTransactionService ingenicoTransactionService) {
        this.ingenicoTransactionService = ingenicoTransactionService;
    }
}
