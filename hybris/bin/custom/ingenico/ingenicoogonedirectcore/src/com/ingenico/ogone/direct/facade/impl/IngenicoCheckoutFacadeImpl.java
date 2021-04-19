package com.ingenico.ogone.direct.facade.impl;

import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_METHOD_IDEAL;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import com.ingenico.direct.domain.CreateHostedCheckoutResponse;
import com.ingenico.direct.domain.GetHostedCheckoutResponse;
import com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants;
import de.hybris.platform.acceleratorservices.urlresolver.SiteBaseUrlResolutionService;
import de.hybris.platform.basecommerce.model.site.BaseSiteModel;
import de.hybris.platform.commercefacades.order.CheckoutFacade;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commercefacades.product.data.PriceData;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.commercefacades.user.data.CountryData;
import de.hybris.platform.commerceservices.order.CommerceCheckoutService;
import de.hybris.platform.commerceservices.service.data.CommerceCheckoutParameter;
import de.hybris.platform.commerceservices.strategies.CheckoutCustomerStrategy;
import de.hybris.platform.core.model.c2l.LanguageModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.payment.IngenicoPaymentInfoModel;
import de.hybris.platform.core.model.order.payment.PaymentInfoModel;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.order.CartService;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.payment.dto.TransactionStatus;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.payment.model.PaymentTransactionModel;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.site.BaseSiteService;
import de.hybris.platform.store.BaseStoreModel;
import de.hybris.platform.store.services.BaseStoreService;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingenico.direct.domain.CreateHostedTokenizationResponse;
import com.ingenico.direct.domain.CreatePaymentResponse;
import com.ingenico.direct.domain.DirectoryEntry;
import com.ingenico.direct.domain.GetHostedTokenizationResponse;
import com.ingenico.direct.domain.PaymentProduct;
import com.ingenico.direct.domain.PaymentProductDisplayHints;
import com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_METHOD_TYPE;
import com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.HOSTED_CHECKOUT_STATUS_ENUM;
import com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_STATUS_CATEGORY_ENUM;
import com.ingenico.ogone.direct.enums.IngenicoCheckoutTypesEnum;
import com.ingenico.ogone.direct.exception.IngenicoNonValidPaymentProductException;
import com.ingenico.ogone.direct.facade.IngenicoCheckoutFacade;
import com.ingenico.ogone.direct.facade.IngenicoUserFacade;
import com.ingenico.ogone.direct.order.data.IngenicoHostedTokenizationData;
import com.ingenico.ogone.direct.order.data.IngenicoPaymentInfoData;
import com.ingenico.ogone.direct.service.IngenicoPaymentService;
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
    private CheckoutCustomerStrategy checkoutCustomerStrategy;

    private IngenicoUserFacade ingenicoUserFacade;
    private IngenicoPaymentService ingenicoPaymentService;

    private BaseSiteService baseSiteService;
    private SiteBaseUrlResolutionService siteBaseUrlResolutionService;

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
                .anyMatch(paymentProduct -> PAYMENT_METHOD_IDEAL.equals(paymentProduct.getId()));

        if (isIdealPresent) {
            final CartData cartData = checkoutFacade.getCheckoutCart();
            return ingenicoPaymentService.getProductDirectoryEntries(PAYMENT_METHOD_IDEAL, cartData.getTotalPrice().getCurrencyIso(), getCountryCode(cartData));
        }
        return Collections.emptyList();
    }

    @Override
    public void fillIngenicoPaymentInfoData(final IngenicoPaymentInfoData ingenicoPaymentInfoData, int paymentId) throws IngenicoNonValidPaymentProductException {
        final PaymentProduct paymentProduct = getPaymentMethodById(paymentId);
        if (isValidPaymentMethod(paymentProduct)) {
            ingenicoPaymentInfoData.setId(paymentProduct.getId());
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
    public CreatePaymentResponse authorisePayment(IngenicoHostedTokenizationData ingenicoHostedTokenizationData) {

        final GetHostedTokenizationResponse hostedTokenization = ingenicoPaymentService.getHostedTokenization(ingenicoHostedTokenizationData.getHostedTokenizationId());

        return ingenicoPaymentService.createPaymentForHostedTokenization(ingenicoHostedTokenizationData, hostedTokenization);

    }

    @Override
    public CreateHostedCheckoutResponse createHostedCheckout() {
        final CartData cartData = checkoutFacade.getCheckoutCart();
        final PriceData totalPrice = cartData.getTotalPrice();

        String fullReturnUrl = getFullResponseUrl("/checkout/multi/ingenico/hosted-checkout/response", true, cartData.getCode());
        return ingenicoPaymentService.createHostedCheckout(fullReturnUrl, cartData.getIngenicoPaymentInfo().getPaymentMethod(),
              cartData.getIngenicoPaymentInfo().getId(),
              totalPrice.getValue(),
              totalPrice.getCurrencyIso(),
              getShopperLocale());
    }

    @Override
    public boolean validatePaymentForHostedCheckoutResponse(String hostedCheckoutId) {
        GetHostedCheckoutResponse hostedCheckoutData = ingenicoPaymentService.getHostedCheckout(hostedCheckoutId);

        if (hostedCheckoutData.getStatus().equals(HOSTED_CHECKOUT_STATUS_ENUM.PAYMENT_CREATED.getValue())) { // hosted checkout was created and processed
            if (hostedCheckoutData.getCreatedPaymentOutput().getPaymentStatusCategory().equals(PAYMENT_STATUS_CATEGORY_ENUM.SUCCESSFUL.getValue())) { //payment was created and captured
                return true;
            }
        }
        return false;
    }

    @Override public boolean authorisePaymentHostedCheckout(String requestId) {
        CustomerModel currentCustomer = checkoutCustomerStrategy.getCurrentUserForCheckout();
        CartModel cartModel = getCart();
        PaymentTransactionEntryModel paymentTransactionModel = ingenicoPaymentService.authorisePayment(cartModel, currentCustomer, requestId);

        return paymentTransactionModel != null
              && (TransactionStatus.ACCEPTED.name().equals(paymentTransactionModel.getTransactionStatus())
              || TransactionStatus.REVIEW.name().equals(paymentTransactionModel.getTransactionStatus()));
    }

    @Override public String startOrderCreationProcess() {
        final OrderData orderData;
        String orderCode = "";
        try
        {
            orderData = checkoutFacade.placeOrder();
            orderCode = orderData.getCode();
        } catch (final Exception e)
        {
            LOGGER.error("Failed to place Order", e);
        }
        return orderCode;
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
        paymentInfo.setIngenicoCheckoutType(ingenicoPaymentInfoData.getIngenicoCheckoutType());

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

    public void setCheckoutCustomerStrategy(CheckoutCustomerStrategy checkoutCustomerStrategy) {
        this.checkoutCustomerStrategy = checkoutCustomerStrategy;
    }
}
