package com.ingenico.ogone.direct.facade.impl;

import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_METHOD_IDEAL;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import de.hybris.platform.commercefacades.order.CheckoutFacade;
import de.hybris.platform.commercefacades.order.data.CartData;
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
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.store.BaseStoreModel;
import de.hybris.platform.store.services.BaseStoreService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingenico.direct.domain.CreateHostedTokenizationResponse;
import com.ingenico.direct.domain.DirectoryEntry;
import com.ingenico.direct.domain.PaymentProduct;
import com.ingenico.direct.domain.PaymentProductDisplayHints;
import com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_METHOD_TYPE;
import com.ingenico.ogone.direct.enums.IngenicoCheckoutTypesEnum;
import com.ingenico.ogone.direct.facade.IngenicoCheckoutFacade;
import com.ingenico.ogone.direct.order.data.IngenicoPaymentInfoData;
import com.ingenico.ogone.direct.service.IngenicoPaymentService;
import org.springframework.util.CollectionUtils;
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

    private IngenicoPaymentService ingenicoPaymentService;


    @Override
    public List<PaymentProduct> getAvailablePaymentMethods() {
        final CartData cartData = checkoutFacade.getCheckoutCart();

        final PriceData totalPrice = cartData.getTotalPrice();
        List<PaymentProduct> paymentProducts = ingenicoPaymentService.getPaymentProducts(totalPrice.getValue(),
                totalPrice.getCurrencyIso(),
                getCountryCode(cartData),
                getShopperLocale());

        paymentProducts = filterByCheckoutType(paymentProducts);
        paymentProducts = removeIDealWhenNoIssuers(paymentProducts);

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
        final CreateHostedTokenizationResponse hostedTokenization = ingenicoPaymentService.createHostedTokenization(getShopperLocale());
        hostedTokenization.setPartialRedirectUrl(IngenicoUrlUtils.buildFullURL(hostedTokenization.getPartialRedirectUrl()));
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
    public void fillIngenicoPaymentInfoData(final IngenicoPaymentInfoData ingenicoPaymentInfoData, int paymentId) {
        PaymentProduct paymentProduct;
        if (paymentId == GROUPED_CARDS_ID) {
            paymentProduct = createGroupedCardPaymentProduct();
        } else {
            paymentProduct = getPaymentMethodById(paymentId);
        }

        if (isValidPaymentMethod(paymentProduct)) {
            ingenicoPaymentInfoData.setId(paymentProduct.getId());
            ingenicoPaymentInfoData.setPaymentMethod(paymentProduct.getPaymentMethod());
            ingenicoPaymentInfoData.setIngenicoCheckoutType(getIngenicoCheckoutType());
        }
    }

    @Override
    public void handlePaymentInfo(IngenicoPaymentInfoData ingenicoPaymentInfoData) {

        final CartModel cartModel = getCart();
        final CommerceCheckoutParameter parameter = new CommerceCheckoutParameter();
        parameter.setEnableHooks(Boolean.TRUE);
        parameter.setCart(cartModel);
        parameter.setPaymentInfo(createPaymentInfo(cartModel, ingenicoPaymentInfoData));

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

    protected PaymentInfoModel createPaymentInfo(final CartModel cartModel, IngenicoPaymentInfoData ingenicoPaymentInfoData) {
        final IngenicoPaymentInfoModel paymentInfo = modelService.create(IngenicoPaymentInfoModel.class);
        paymentInfo.setCode(generatePaymentInfoCode(cartModel));

        paymentInfo.setUser(cartModel.getUser());
        paymentInfo.setSaved(false);
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
        paymentProduct.setPaymentMethod("card");
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
                if (PAYMENT_METHOD_TYPE.CARD.getValue().equals(paymentProduct.getPaymentMethod()) || PAYMENT_METHOD_TYPE.REDIRECT.getValue().equals(paymentProduct.getPaymentMethod())) {
                    return false;
                }
                break;
            default:
                // Happy Sonar
                break;
        }
        return true;
    }

    private List<PaymentProduct> removeIDealWhenNoIssuers(List<PaymentProduct> paymentProducts) {
//        Collection<DirectoryEntry> iDealIssuers = getIdealIssuers(paymentProducts);
//        if (CollectionUtils.isEmpty(iDealIssuers)) {
            paymentProducts =
                  paymentProducts.stream().filter(paymentProduct -> !PAYMENT_METHOD_IDEAL.equals(paymentProduct.getId())).collect(Collectors.toList());
//        }
        return paymentProducts;
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
}
