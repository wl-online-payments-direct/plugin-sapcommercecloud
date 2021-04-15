package com.ingenico.ogone.direct.service.impl;

import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_METHOD_IDEAL;
import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_METHOD_PAYPAL;
import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import de.hybris.platform.acceleratorservices.urlresolver.SiteBaseUrlResolutionService;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.order.CartService;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.site.BaseSiteService;
import de.hybris.platform.store.BaseStoreModel;
import de.hybris.platform.store.services.BaseStoreService;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;

import com.ingenico.direct.Client;
import com.ingenico.direct.DeclinedPaymentException;
import com.ingenico.direct.domain.AmountOfMoney;
import com.ingenico.direct.domain.BrowserData;
import com.ingenico.direct.domain.Card;
import com.ingenico.direct.domain.CardPaymentMethodSpecificInput;
import com.ingenico.direct.domain.CardPaymentMethodSpecificInputBase;
import com.ingenico.direct.domain.CreateHostedCheckoutRequest;
import com.ingenico.direct.domain.CreateHostedCheckoutResponse;
import com.ingenico.direct.domain.CreateHostedTokenizationRequest;
import com.ingenico.direct.domain.CreateHostedTokenizationResponse;
import com.ingenico.direct.domain.CreatePaymentRequest;
import com.ingenico.direct.domain.CreatePaymentResponse;
import com.ingenico.direct.domain.Customer;
import com.ingenico.direct.domain.CustomerDevice;
import com.ingenico.direct.domain.DirectoryEntry;
import com.ingenico.direct.domain.GetHostedCheckoutResponse;
import com.ingenico.direct.domain.GetHostedTokenizationResponse;
import com.ingenico.direct.domain.GetPaymentProductsResponse;
import com.ingenico.direct.domain.HostedCheckoutSpecificInput;
import com.ingenico.direct.domain.Order;
import com.ingenico.direct.domain.PaymentProduct;
import com.ingenico.direct.domain.ProductDirectory;
import com.ingenico.direct.domain.RedirectPaymentMethodSpecificInput;
import com.ingenico.direct.domain.RedirectPaymentProduct840SpecificInput;
import com.ingenico.direct.domain.RedirectionData;
import com.ingenico.direct.domain.ThreeDSecure;
import com.ingenico.direct.merchant.products.GetPaymentProductParams;
import com.ingenico.direct.merchant.products.GetPaymentProductsParams;
import com.ingenico.direct.merchant.products.GetProductDirectoryParams;
import com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_METHOD_TYPE;
import com.ingenico.ogone.direct.enums.OperationCodesEnum;
import com.ingenico.ogone.direct.factory.IngenicoClientFactory;
import com.ingenico.ogone.direct.order.data.IngenicoHostedTokenizationData;
import com.ingenico.ogone.direct.service.IngenicoPaymentService;
import com.ingenico.ogone.direct.util.IngenicoAmountUtils;
import com.ingenico.ogone.direct.util.IngenicoLogUtils;

public class IngenicoPaymentServiceImpl implements IngenicoPaymentService {

    private final static Logger LOGGER = LoggerFactory.getLogger(IngenicoPaymentServiceImpl.class);
    private static final String ECOMMERCE = "ECOMMERCE";
    private static final String CHECKOUT_MULTI_INGENICO_PAYMENT_HANDLE_3_DS = "/checkout/multi/ingenico/payment/handle3ds";

    private CartService cartService;
    private BaseSiteService baseSiteService;
    private BaseStoreService baseStoreService;
    private SiteBaseUrlResolutionService siteBaseUrlResolutionService;
    private IngenicoAmountUtils ingenicoAmountUtils;
    private IngenicoClientFactory ingenicoClientFactory;
    private Converter<CartModel, CreatePaymentRequest> ingenicoPaymentRequestConverter;


    @Override
    public GetPaymentProductsResponse getPaymentProductsResponse(BigDecimal amount, String currency, String countryCode, String shopperLocale) {
        validateParameterNotNull(amount, "amount cannot be null");
        validateParameterNotNull(currency, "currency cannot be null");
        validateParameterNotNull(countryCode, "countryCode cannot be null");
        validateParameterNotNull(shopperLocale, "shopperLocale cannot be null");

        try (Client client = ingenicoClientFactory.getClient()) {
            final GetPaymentProductsParams params = new GetPaymentProductsParams();
            params.setCurrencyCode(currency);
            params.setAmount(ingenicoAmountUtils.createAmount(amount, currency));
            params.setCountryCode(countryCode);
            params.setLocale(shopperLocale);

            final GetPaymentProductsResponse paymentProducts = client.merchant(getMerchantId()).products().getPaymentProducts(params);

            IngenicoLogUtils.logAction(LOGGER, "getPaymentProducts", params, paymentProducts);

            return paymentProducts;
        } catch (IOException e) {
            LOGGER.error("[ INGENICO ] Errors during getting PaymentProducts ", e);
            //TODO Throw Logical Exception
            return null;
        }
    }

    // TODO Getting MerchantID using the configuration on BaseStore
    private String getMerchantId() {
        final BaseStoreModel currentBaseStore = baseStoreService.getCurrentBaseStore();
//        return currentBaseStore.getIngenicoConfiguration().getMerchantID();
        return "greenlightcommerce1";
    }

    @Override
    public List<PaymentProduct> getPaymentProducts(BigDecimal amount, String currency, String countryCode, String shopperLocale) {
        final GetPaymentProductsResponse getPaymentProductsResponse = getPaymentProductsResponse(amount, currency, countryCode, shopperLocale);
        return getPaymentProductsResponse.getPaymentProducts();
    }

    @Override
    public PaymentProduct getPaymentProduct(Integer id, BigDecimal amount, String currency, String countryCode, String shopperLocale) {
        validateParameterNotNull(id, "id cannot be null");
        validateParameterNotNull(amount, "amount cannot be null");
        validateParameterNotNull(currency, "currency cannot be null");
        validateParameterNotNull(countryCode, "countryCode cannot be null");
        validateParameterNotNull(shopperLocale, "shopperLocale cannot be null");

        try (Client client = ingenicoClientFactory.getClient()) {

            final GetPaymentProductParams params = new GetPaymentProductParams();
            params.setCurrencyCode(currency);
            params.setAmount(ingenicoAmountUtils.createAmount(amount, currency));
            params.setCountryCode(countryCode);
            params.setLocale(shopperLocale);

            final PaymentProduct paymentProduct = client.merchant(getMerchantId()).products().getPaymentProduct(id, params);

            IngenicoLogUtils.logAction(LOGGER, "getPaymentProduct", params, paymentProduct);

            return paymentProduct;
        } catch (IOException e) {
            LOGGER.error("[ INGENICO ] Errors during getting PaymentProduct ", e);
            //TODO Throw Logical Exception
            return null;
        }
    }

    @Override
    @Cacheable(value = "productDirectory", key = "#id+'_cur_'+#currency+'_country_'+#countryCode")
    public ProductDirectory getProductDirectory(Integer id, String currency, String countryCode) {
        validateParameterNotNull(id, "id cannot be null");
        validateParameterNotNull(currency, "currency cannot be null");
        validateParameterNotNull(countryCode, "countryCode cannot be null");

        try (Client client = ingenicoClientFactory.getClient()) {

            final GetProductDirectoryParams params = new GetProductDirectoryParams();
            params.setCurrencyCode(currency);
            params.setCountryCode(countryCode);

            final ProductDirectory productDirectory = client.merchant(getMerchantId()).products().getProductDirectory(id, params);

            IngenicoLogUtils.logAction(LOGGER, "getProductDirectory", params, productDirectory);

            return productDirectory;
        } catch (IOException e) {
            LOGGER.error("[ INGENICO ] Errors during getting productDirectory ", e);
            //TODO Throw Logical Exception
            return null;
        }
    }

    @Override
    public List<DirectoryEntry> getProductDirectoryEntries(Integer id, String currency, String countryCode) {
        return getProductDirectory(id, currency, countryCode).getEntries();
    }

    @Override
    public CreateHostedTokenizationResponse createHostedTokenization(String shopperLocale, List<String> savedTokens) {
        validateParameterNotNull(shopperLocale, "shopperLocale cannot be null");
        try (Client client = ingenicoClientFactory.getClient()) {

            CreateHostedTokenizationRequest params = new CreateHostedTokenizationRequest();
            params.setLocale(shopperLocale);
            params.setAskConsumerConsent(true);
            if (CollectionUtils.isNotEmpty(savedTokens)) {
                params.setTokens(String.join(",", savedTokens));
            }
            final CreateHostedTokenizationResponse hostedTokenization = client.merchant(getMerchantId()).hostedTokenization().createHostedTokenization(params);

            IngenicoLogUtils.logAction(LOGGER, "createHostedTokenization", params, hostedTokenization);

            return hostedTokenization;
        } catch (IOException e) {
            LOGGER.error("[ INGENICO ] Errors during getting createHostedTokenization ", e);
            //TODO Throw Logical Exception
            return null;
        }
    }

    @Override
    public GetHostedTokenizationResponse getHostedTokenization(String hostedTokenizationId) {
        validateParameterNotNull(hostedTokenizationId, "hostedTokenizationId cannot be null");
        try (Client client = ingenicoClientFactory.getClient()) {

            final GetHostedTokenizationResponse hostedTokenization = client.merchant(getMerchantId()).hostedTokenization().getHostedTokenization(hostedTokenizationId);

            IngenicoLogUtils.logAction(LOGGER, "getHostedTokenization", hostedTokenizationId, hostedTokenization);

            return hostedTokenization;
        } catch (IOException e) {
            LOGGER.error("[ INGENICO ] Errors during getting getHostedTokenization ", e);
            //TODO Throw Logical Exception
            return null;
        }
    }


    @Override
    @SuppressWarnings("all")
    public CreatePaymentResponse createPaymentForHostedTokenization(IngenicoHostedTokenizationData ingenicoHostedTokenizationData, GetHostedTokenizationResponse tokenizationResponse) {
        validateParameterNotNull(tokenizationResponse, "tokenizationResponse cannot be null");
        final CartModel sessionCart = getSessionCart();
        validateParameterNotNull(sessionCart, "sessionCart cannot be null");
        try (Client client = ingenicoClientFactory.getClient()) {

            final CreatePaymentRequest params = ingenicoPaymentRequestConverter.convert(sessionCart);

            params.setCardPaymentMethodSpecificInput(buildCardPaymentMethodSpecificInput(tokenizationResponse));
            params.getOrder().getCustomer().setDevice(getBrowserInfo(ingenicoHostedTokenizationData));
            final CreatePaymentResponse payment = client.merchant(getMerchantId()).payments().createPayment(params);

            IngenicoLogUtils.logAction(LOGGER, "getHostedTokenization", params, payment);

            return payment;
        } catch (DeclinedPaymentException e) {
            LOGGER.error("[ INGENICO ] Errors during getting createPayment ", e);
        } catch (IOException e) {
            LOGGER.error("[ INGENICO ] Errors during getting createPayment ", e);
            //TODO Throw Logical Exception
        }
        return null;
    }

    private CardPaymentMethodSpecificInput buildCardPaymentMethodSpecificInput(GetHostedTokenizationResponse tokenizationResponse) {
        CardPaymentMethodSpecificInput cardPaymentMethodSpecificInput = new CardPaymentMethodSpecificInput();
        cardPaymentMethodSpecificInput.setPaymentProductId(tokenizationResponse.getToken().getPaymentProductId());
//        cardPaymentMethodSpecificInput.setToken(tokenizationResponse.getToken().getId());
        // TODO to remove later
//        cardPaymentMethodSpecificInput.setIsRecurring(true);
//        cardPaymentMethodSpecificInput.setRecurring(new CardRecurrenceDetails());
//        cardPaymentMethodSpecificInput.getRecurring().setRecurringPaymentSequenceIndicator("recurring");

        cardPaymentMethodSpecificInput.setSkipAuthentication(false);
        cardPaymentMethodSpecificInput.setTransactionChannel(ECOMMERCE);
        cardPaymentMethodSpecificInput.setThreeDSecure(new ThreeDSecure());
        cardPaymentMethodSpecificInput.getThreeDSecure().setRedirectionData(new RedirectionData());
        cardPaymentMethodSpecificInput.getThreeDSecure().getRedirectionData().setReturnUrl(getHostedTokenizationReturnUrl());

        //TODO to replace by Token
        final Card card = new Card();
        card.setCardNumber("4000000000000002");
        card.setCardholderName("Marouane");
        card.setCvv("123");
        card.setExpiryDate("1230");
        cardPaymentMethodSpecificInput.setCard(card);


        return cardPaymentMethodSpecificInput;
    }

    private CustomerDevice getBrowserInfo(IngenicoHostedTokenizationData ingenicoHostedTokenizationData) {
        BrowserData browserData = new BrowserData();
        browserData.setColorDepth(ingenicoHostedTokenizationData.getColorDepth());
        browserData.setJavaEnabled(ingenicoHostedTokenizationData.getNavigatorJavaEnabled());
        browserData.setScreenHeight(ingenicoHostedTokenizationData.getScreenHeight());
        browserData.setScreenWidth(ingenicoHostedTokenizationData.getScreenWidth());

        CustomerDevice browserInfo = new CustomerDevice();
        browserInfo.setAcceptHeader(ingenicoHostedTokenizationData.getAcceptHeader());
        browserInfo.setUserAgent(ingenicoHostedTokenizationData.getUserAgent());
        browserInfo.setLocale(ingenicoHostedTokenizationData.getLocale());
        browserInfo.setIpAddress(ingenicoHostedTokenizationData.getIpAddress());
        browserInfo.setTimezoneOffsetUtcMinutes(ingenicoHostedTokenizationData.getTimezoneOffsetUtcMinutes());
        browserInfo.setBrowserData(browserData);

        return browserInfo;
    }

    private String getHostedTokenizationReturnUrl() {
        return siteBaseUrlResolutionService.getWebsiteUrlForSite(baseSiteService.getCurrentBaseSite(), true,
                CHECKOUT_MULTI_INGENICO_PAYMENT_HANDLE_3_DS);
    }

    private CartModel getSessionCart() {
        if (cartService.hasSessionCart()) {
            return cartService.getSessionCart();
        }
        return null;
    }


    @Override
    public CreateHostedCheckoutResponse createHostedCheckout(String fullResponseUrl, String paymentMethod, Integer paymentProductId, BigDecimal amount, String currency, String shopperLocale) {
        try (Client client = ingenicoClientFactory.getClient()) {
            CreateHostedCheckoutRequest request = new CreateHostedCheckoutRequest();

            switch (PAYMENT_METHOD_TYPE.valueOf(paymentMethod.toUpperCase())) {
                case CARD:
                    // TODO take authorizationCode from paymentMode
                    request.setCardPaymentMethodSpecificInput(prepareCardPaymentInputData(OperationCodesEnum.SALE.getCode(), paymentProductId));
                    break;
                case REDIRECT:
                    request.setRedirectPaymentMethodSpecificInput(prepareRedirectPaymentInputData(fullResponseUrl, paymentProductId));
                    break;
                default:
                    break;
            }

            request.setHostedCheckoutSpecificInput(prepareHostedCheckoutInputData(shopperLocale, fullResponseUrl));
            request.setOrder(prepareOrderDetailsInputData(currency, amount, shopperLocale));

            final CreateHostedCheckoutResponse hostedCheckout = client.merchant(getMerchantId()).hostedCheckout().createHostedCheckout(request);

            return hostedCheckout;

        } catch (IOException e) {
            LOGGER.error("[ INGENICO ] Errors during getting createHostedCheckout ", e);
            //TODO Throw Logical Exception
            return null;
        }
    }

    @Override
    public GetHostedCheckoutResponse getHostedCheckout(String hostedCheckoutId) {
        try (Client client = ingenicoClientFactory.getClient()) {
            final GetHostedCheckoutResponse hostedCheckoutResponse = client.merchant(getMerchantId()).hostedCheckout().getHostedCheckout(hostedCheckoutId);
            return hostedCheckoutResponse;
        } catch (IOException e) {
            LOGGER.error("[ INGENICO ] Errors during getting createHostedCheckout ", e);
            //TODO Throw Logical Exception
            return null;
        }

    }

    private CardPaymentMethodSpecificInputBase prepareCardPaymentInputData(String authorizationMode, Integer paymentProductId) {
        CardPaymentMethodSpecificInputBase cardPaymentMethodSpecificInput = new CardPaymentMethodSpecificInputBase();
        cardPaymentMethodSpecificInput.setAuthorizationMode(authorizationMode);
        cardPaymentMethodSpecificInput.setPaymentProductId(paymentProductId);
//                    PaymentProduct5100SpecificInput cPayspecificData = new PaymentProduct5100SpecificInput();
//                    cPayspecificData.setBrand();

//                    cardPaymentMethodSpecificInput.setPaymentProduct5100SpecificInput(cPayspecificData);

        return cardPaymentMethodSpecificInput;
    }

    private HostedCheckoutSpecificInput prepareHostedCheckoutInputData(String shopperLocale, String fullResponseUrl) {
        HostedCheckoutSpecificInput hostedCheckoutSpecificInput = new HostedCheckoutSpecificInput();
        hostedCheckoutSpecificInput.setLocale(shopperLocale);
        hostedCheckoutSpecificInput.setReturnUrl(fullResponseUrl);
        return hostedCheckoutSpecificInput;
    }

    private RedirectPaymentMethodSpecificInput prepareRedirectPaymentInputData(String fullReturnUrl, Integer paymentProductId) {
        RedirectPaymentMethodSpecificInput redirectPaymentMethodSpecificInput = new RedirectPaymentMethodSpecificInput();
        redirectPaymentMethodSpecificInput.setPaymentProductId(paymentProductId);
        if (paymentProductId == PAYMENT_METHOD_IDEAL) {
            // TODO fill data for iDeal
            //        RedirectPaymentProduct809SpecificInput iDealSpecificInfo = new RedirectPaymentProduct809SpecificInput();
            //        iDealSpecificInfo.setIssuerId();
            //        redirectPaymentMethodSpecificInput.setPaymentProduct809SpecificInput(iDealSpecificInfo);
        } else if (paymentProductId == PAYMENT_METHOD_PAYPAL) {
            // TODO needs configuration field; default is false
            RedirectPaymentProduct840SpecificInput redirectPaymentProduct840SpecificInput = new RedirectPaymentProduct840SpecificInput();
            redirectPaymentProduct840SpecificInput.setAddressSelectionAtPayPal(Boolean.FALSE);
            redirectPaymentMethodSpecificInput.setPaymentProduct840SpecificInput(redirectPaymentProduct840SpecificInput);
        }
        RedirectionData redirectionData = new RedirectionData();
        redirectionData.setReturnUrl(fullReturnUrl);
        redirectPaymentMethodSpecificInput.setRedirectionData(redirectionData);

        return redirectPaymentMethodSpecificInput;

    }

    private Order prepareOrderDetailsInputData(String currencyCode, BigDecimal amount, String shopperLocale) {
        Order order = new Order();

        AmountOfMoney amountOfMoney = new AmountOfMoney();
        amountOfMoney.setAmount(ingenicoAmountUtils.createAmount(amount, currencyCode));
        amountOfMoney.setCurrencyCode(currencyCode);
        order.setAmountOfMoney(amountOfMoney);

        Customer customer = new Customer();
        customer.setLocale(shopperLocale);

//        ShoppingCart cart = new ShoppingCart();
//        cart.setAmountBreakdown();
        return order;
    }

    public void setIngenicoAmountUtils(IngenicoAmountUtils ingenicoAmountUtils) {
        this.ingenicoAmountUtils = ingenicoAmountUtils;
    }

    public void setIngenicoClientFactory(IngenicoClientFactory ingenicoClientFactory) {
        this.ingenicoClientFactory = ingenicoClientFactory;
    }

    public void setCartService(CartService cartService) {
        this.cartService = cartService;
    }

    public void setIngenicoPaymentRequestConverter(Converter<CartModel, CreatePaymentRequest> ingenicoPaymentRequestConverter) {
        this.ingenicoPaymentRequestConverter = ingenicoPaymentRequestConverter;
    }

    public void setBaseSiteService(BaseSiteService baseSiteService) {
        this.baseSiteService = baseSiteService;
    }

    public void setSiteBaseUrlResolutionService(SiteBaseUrlResolutionService siteBaseUrlResolutionService) {
        this.siteBaseUrlResolutionService = siteBaseUrlResolutionService;
    }

    public void setBaseStoreService(BaseStoreService baseStoreService) {
        this.baseStoreService = baseStoreService;
    }
}
