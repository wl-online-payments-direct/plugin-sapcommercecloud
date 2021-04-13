package com.ingenico.ogone.direct.service.impl;

import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_METHOD_IDEAL;
import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_METHOD_PAYPAL;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import com.ingenico.direct.ReferenceException;
import com.ingenico.direct.domain.AmountBreakdown;
import com.ingenico.direct.domain.AmountOfMoney;
import com.ingenico.direct.domain.CardPaymentMethodSpecificInput;
import com.ingenico.direct.domain.CardPaymentMethodSpecificInputBase;
import com.ingenico.direct.domain.CreateHostedCheckoutRequest;
import com.ingenico.direct.domain.CreateHostedCheckoutResponse;
import com.ingenico.direct.domain.Customer;
import com.ingenico.direct.domain.GetHostedCheckoutResponse;
import com.ingenico.direct.domain.HostedCheckoutSpecificInput;
import com.ingenico.direct.domain.Order;
import com.ingenico.direct.domain.PaymentProduct5100SpecificInput;
import com.ingenico.direct.domain.RedirectPaymentMethodSpecificInput;
import com.ingenico.direct.domain.RedirectPaymentProduct809SpecificInput;
import com.ingenico.direct.domain.RedirectPaymentProduct840SpecificInput;
import com.ingenico.direct.domain.RedirectionData;
import com.ingenico.direct.domain.ShoppingCart;
import com.ingenico.ogone.direct.enums.OperationCodesEnum;
import de.hybris.platform.store.BaseStoreModel;
import de.hybris.platform.store.services.BaseStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;

import com.ingenico.direct.Client;
import com.ingenico.direct.domain.CreateHostedTokenizationRequest;
import com.ingenico.direct.domain.CreateHostedTokenizationResponse;
import com.ingenico.direct.domain.DirectoryEntry;
import com.ingenico.direct.domain.GetPaymentProductsResponse;
import com.ingenico.direct.domain.PaymentProduct;
import com.ingenico.direct.domain.ProductDirectory;
import com.ingenico.direct.merchant.products.GetPaymentProductParams;
import com.ingenico.direct.merchant.products.GetPaymentProductsParams;
import com.ingenico.direct.merchant.products.GetProductDirectoryParams;
import com.ingenico.ogone.direct.factory.IngenicoClientFactory;
import com.ingenico.ogone.direct.service.IngenicoPaymentService;
import com.ingenico.ogone.direct.util.IngenicoAmountUtils;
import com.ingenico.ogone.direct.util.IngenicoLogUtils;
import com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_METHOD_TYPE;

public class IngenicoPaymentServiceImpl implements IngenicoPaymentService {

    private final static Logger LOGGER = LoggerFactory.getLogger(IngenicoPaymentServiceImpl.class);

    private IngenicoAmountUtils ingenicoAmountUtils;
    private IngenicoClientFactory ingenicoClientFactory;
    private BaseStoreService baseStoreService;

    @Override
    public GetPaymentProductsResponse getPaymentProductsResponse(BigDecimal amount, String currency, String countryCode, String shopperLocale) {
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
    public CreateHostedTokenizationResponse createHostedTokenization(String shopperLocale) {
        try (Client client = ingenicoClientFactory.getClient()) {

            CreateHostedTokenizationRequest params = new CreateHostedTokenizationRequest();
            params.setLocale(shopperLocale);
            params.setAskConsumerConsent(true);
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

    public void setBaseStoreService(BaseStoreService baseStoreService) {
        this.baseStoreService = baseStoreService;
    }
}
