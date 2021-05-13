package com.ingenico.ogone.direct.facade.impl;

import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_METHOD_IDEAL;
import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_METHOD_IDEAL_COUNTRY;
import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_STATUS_ENUM;
import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.UNAUTHORIZED_REASON;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import bsh.StringUtil;
import de.hybris.platform.acceleratorservices.urlresolver.SiteBaseUrlResolutionService;
import de.hybris.platform.basecommerce.model.site.BaseSiteModel;
import de.hybris.platform.commercefacades.order.CheckoutFacade;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commercefacades.product.data.PriceData;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.commercefacades.user.data.CountryData;
import de.hybris.platform.commerceservices.customer.CustomerAccountService;
import de.hybris.platform.commerceservices.order.CommerceCheckoutService;
import de.hybris.platform.commerceservices.service.data.CommerceCheckoutParameter;
import de.hybris.platform.core.model.c2l.LanguageModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.payment.IngenicoPaymentInfoModel;
import de.hybris.platform.core.model.order.payment.PaymentInfoModel;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.order.CartService;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.payment.enums.PaymentTransactionType;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.site.BaseSiteService;
import de.hybris.platform.store.BaseStoreModel;
import de.hybris.platform.store.services.BaseStoreService;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingenico.direct.ApiException;
import com.ingenico.direct.domain.CreateHostedCheckoutResponse;
import com.ingenico.direct.domain.CreateHostedTokenizationResponse;
import com.ingenico.direct.domain.CreatePaymentResponse;
import com.ingenico.direct.domain.DirectoryEntry;
import com.ingenico.direct.domain.GetHostedCheckoutResponse;
import com.ingenico.direct.domain.GetHostedTokenizationResponse;
import com.ingenico.direct.domain.PaymentProduct;
import com.ingenico.direct.domain.PaymentProductDisplayHints;
import com.ingenico.direct.domain.PaymentResponse;
import com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.HOSTED_CHECKOUT_STATUS_ENUM;
import com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_METHOD_TYPE;
import com.ingenico.ogone.direct.enums.IngenicoCheckoutTypesEnum;
import com.ingenico.ogone.direct.exception.IngenicoNonAuthorizedPaymentException;
import com.ingenico.ogone.direct.exception.IngenicoNonValidPaymentProductException;
import com.ingenico.ogone.direct.facade.IngenicoCheckoutFacade;
import com.ingenico.ogone.direct.facade.IngenicoUserFacade;
import com.ingenico.ogone.direct.order.data.IngenicoHostedTokenizationData;
import com.ingenico.ogone.direct.order.data.IngenicoPaymentInfoData;
import com.ingenico.ogone.direct.service.IngenicoPaymentService;
import com.ingenico.ogone.direct.service.IngenicoTransactionService;
import com.ingenico.ogone.direct.util.IngenicoUrlUtils;

public class IngenicoCheckoutFacadeImpl implements IngenicoCheckoutFacade {
    private static final Logger LOGGER = LoggerFactory.getLogger(IngenicoCheckoutFacadeImpl.class);
    private static final int GROUPED_CARDS_ID = -1;

    private CommonI18NService commonI18NService;
    private ModelService modelService;

    private Converter<AddressData, AddressModel> addressReverseConverter;

    private CartService cartService;
    private CheckoutFacade checkoutFacade;
    private CommerceCheckoutService commerceCheckoutService;
    private BaseStoreService baseStoreService;
    private CustomerAccountService customerAccountService;
    private BaseSiteService baseSiteService;
    private SiteBaseUrlResolutionService siteBaseUrlResolutionService;

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
        if (paymentId == GROUPED_CARDS_ID) {
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
    public void fillIngenicoPaymentInfoData(final IngenicoPaymentInfoData ingenicoPaymentInfoData, int paymentId, String paymentDirId) throws IngenicoNonValidPaymentProductException {
        final PaymentProduct paymentProduct = getPaymentMethodById(paymentId);
        if (isValidPaymentMethod(paymentProduct)) {
            ingenicoPaymentInfoData.setId(paymentProduct.getId());
            ingenicoPaymentInfoData.setPaymentProductDirectoryId(paymentDirId);
            ingenicoPaymentInfoData.setPaymentMethod(paymentProduct.getPaymentMethod());
            if (paymentId == GROUPED_CARDS_ID) {
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

        if (paymentForHostedTokenization.getMerchantAction() != null) {
            ingenicoTransactionService.createPaymentTransaction(cartService.getSessionCart(), paymentForHostedTokenization.getPayment(), PaymentTransactionType.AUTHORIZATION);
            throw new IngenicoNonAuthorizedPaymentException(paymentForHostedTokenization.getPayment(),
                    paymentForHostedTokenization.getMerchantAction(),
                    UNAUTHORIZED_REASON.NEED_3DS);
        }
        keepPaymentToken(paymentForHostedTokenization.getPayment()); //token exists if customer checks "Save payment details" btn on HOP
        return handlePaymentResponse(paymentForHostedTokenization.getPayment());

    }


    public OrderData handle3dsResponse(String ref, String returnMAC, String paymentId) throws IngenicoNonAuthorizedPaymentException, InvalidCartException {
        final PaymentResponse payment = ingenicoPaymentService.getPayment(paymentId);
        return handlePaymentResponse(payment);
    }

    @Override
    public CreateHostedCheckoutResponse createHostedCheckout() {
        final CreateHostedCheckoutResponse hostedCheckout = ingenicoPaymentService.createHostedCheckout();
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
                keepPaymentToken(hostedCheckoutData.getCreatedPaymentOutput().getPayment()); //token exists if customer checks "Save payment details" btn on HOP
                return handlePaymentResponse(hostedCheckoutData.getCreatedPaymentOutput().getPayment());
            default:
                LOGGER.error("Unexpected HostedCheckout Status value: " + hostedCheckoutData.getStatus());
                throw new IllegalStateException("Unexpected HostedCheckout Status value: " + hostedCheckoutData.getStatus());
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
                ingenicoTransactionService.createPaymentTransaction(cartService.getSessionCart(),
                        paymentResponse,
                        PaymentTransactionType.AUTHORIZATION);
                throw new IngenicoNonAuthorizedPaymentException(paymentResponse, UNAUTHORIZED_REASON.REJECTED);
            case CANCELLED:
                ingenicoTransactionService.createPaymentTransaction(cartService.getSessionCart(),
                        paymentResponse,
                        PaymentTransactionType.AUTHORIZATION);
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

        OrderData orderData = checkoutFacade.placeOrder();
        final BaseStoreModel baseStoreModel = baseStoreService.getBaseStoreForUid(orderData.getStore());
        final OrderModel orderModel = customerAccountService.getOrderForCode(orderData.getCode(), baseStoreModel);

        ingenicoTransactionService.createPaymentTransaction(orderModel, paymentResponse, paymentTransactionType);
        return orderData;
    }

    protected List<PaymentProduct> filterByCheckoutType(List<PaymentProduct> paymentProducts) {
        final IngenicoCheckoutTypesEnum ingenicoCheckoutType = getIngenicoCheckoutType();
        if (ingenicoCheckoutType == null) {
            // TODO Create a logic for that case
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

                paymentProducts = paymentProducts.stream()
                        .filter(paymentProduct -> PAYMENT_METHOD_TYPE.REDIRECT.getValue().equals(paymentProduct.getPaymentMethod()))
                        .collect(Collectors.toList());

                if (isCardsPresent) {
                    paymentProducts.add(0, createGroupedCardPaymentProduct());
                }
                break;
            default:
                // Happy Sonar
                break;
        }
        return paymentProducts;
    }

    protected IngenicoCheckoutTypesEnum getIngenicoCheckoutType() {
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
        paymentInfo.setIngenicoCheckoutType(ingenicoPaymentInfoData.getIngenicoCheckoutType());

        if (StringUtils.isNotEmpty(ingenicoPaymentInfoData.getToken())) {
            paymentInfo.setSaved(true);
            paymentInfo.setToken(ingenicoPaymentInfoData.getToken());
            paymentInfo.setAlias(ingenicoPaymentInfoData.getAlias());
            paymentInfo.setExpiryDate(ingenicoPaymentInfoData.getExpiryDate());
        }

        AddressModel billingAddress = convertToAddressModel(ingenicoPaymentInfoData.getBillingAddress());
        paymentInfo.setBillingAddress(billingAddress);
        billingAddress.setOwner(paymentInfo);

        modelService.save(paymentInfo);
        return paymentInfo;
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

    private void keepPaymentToken(PaymentResponse paymentResponse) {

        String token = "";
        String cardNumber = "";
        String expDate = "";
        if (paymentResponse.getPaymentOutput().getCardPaymentMethodSpecificOutput() != null) {
            token = paymentResponse.getPaymentOutput().getCardPaymentMethodSpecificOutput().getToken();
            cardNumber = paymentResponse.getPaymentOutput().getCardPaymentMethodSpecificOutput().getCard().getCardNumber();
            expDate = paymentResponse.getPaymentOutput().getCardPaymentMethodSpecificOutput().getCard().getExpiryDate();
        } else if (paymentResponse.getPaymentOutput().getRedirectPaymentMethodSpecificOutput() != null) {
            token = paymentResponse.getPaymentOutput().getRedirectPaymentMethodSpecificOutput().getToken();
        }

        if (StringUtils.isNotEmpty(token)) {
            final CartData cartData = checkoutFacade.getCheckoutCart();
            cartData.getIngenicoPaymentInfo().setToken(token);
            cartData.getIngenicoPaymentInfo().setExpiryDate(expDate);
            cartData.getIngenicoPaymentInfo().setAlias(cardNumber); //in tokenization response the alias==cardNumber
            handlePaymentInfo(cartData.getIngenicoPaymentInfo());
        }
    }

    private PaymentProduct createGroupedCardPaymentProduct() {
        PaymentProduct paymentProduct = new PaymentProduct();
        paymentProduct.setId(GROUPED_CARDS_ID);
        paymentProduct.setPaymentMethod(PAYMENT_METHOD_TYPE.CARD.getValue());
        paymentProduct.setDisplayHints(new PaymentProductDisplayHints());
        paymentProduct.getDisplayHints().setLabel("Grouped Cards");
        return paymentProduct;
    }

    private Boolean isValidPaymentMethod(PaymentProduct paymentProduct) {
        final IngenicoCheckoutTypesEnum ingenicoCheckoutType = getIngenicoCheckoutType();
        switch (ingenicoCheckoutType) {
            case HOSTED_CHECKOUT:
                if (PAYMENT_METHOD_TYPE.MOBILE.getValue().equals(paymentProduct.getPaymentMethod())) {
                    return false;
                }
                break;
            case HOSTED_TOKENIZATION:
                if (PAYMENT_METHOD_TYPE.MOBILE.getValue().equals(paymentProduct.getPaymentMethod())) {
                    return false;
                }
                break;
            default:
                // Happy Sonar
                break;
        }
        return true;
    }

    protected String getFullResponseUrl(final String responseUrl, final boolean isSecure, String cartId) {
        final BaseSiteModel currentBaseSite = baseSiteService.getCurrentBaseSite();
        //TODO remove clue cartId
        final String queryParams = "cartId=" + cartId;

        final String fullResponseUrl = siteBaseUrlResolutionService.getWebsiteUrlForSite(currentBaseSite, isSecure,
                responseUrl, queryParams);

        return fullResponseUrl == null ? "" : fullResponseUrl;
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

    public void setBaseSiteService(BaseSiteService baseSiteService) {
        this.baseSiteService = baseSiteService;
    }

    public void setSiteBaseUrlResolutionService(SiteBaseUrlResolutionService siteBaseUrlResolutionService) {
        this.siteBaseUrlResolutionService = siteBaseUrlResolutionService;
    }

    public void setCustomerAccountService(CustomerAccountService customerAccountService) {
        this.customerAccountService = customerAccountService;
    }

    public void setIngenicoTransactionService(IngenicoTransactionService ingenicoTransactionService) {
        this.ingenicoTransactionService = ingenicoTransactionService;
    }
}
