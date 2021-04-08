package com.ingenico.ogone.direct.service.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import com.ingenico.direct.ReferenceException;
import com.ingenico.direct.domain.CardPaymentMethodSpecificInput;
import com.ingenico.direct.domain.CreateHostedCheckoutRequest;
import com.ingenico.direct.domain.CreateHostedCheckoutResponse;
import com.ingenico.direct.domain.HostedCheckoutSpecificInput;
import com.ingenico.direct.domain.PaymentProduct5100SpecificInput;
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

public class IngenicoPaymentServiceImpl implements IngenicoPaymentService {

    private final static Logger LOGGER = LoggerFactory.getLogger(IngenicoPaymentServiceImpl.class);

    private IngenicoAmountUtils ingenicoAmountUtils;
    private IngenicoClientFactory ingenicoClientFactory;

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
    public CreateHostedCheckoutResponse createHostedCheckout() {
        try (Client client = ingenicoClientFactory.getClient()) {
            CreateHostedCheckoutRequest request = new CreateHostedCheckoutRequest();
            CardPaymentMethodSpecificInput cardPaymentMethodSpecificInput = new CardPaymentMethodSpecificInput();
//            cardPaymentMethodSpecificInput.setAuthorizationMode();
//            cardPaymentMethodSpecificInput.setPaymentProductId();
//            PaymentProduct5100SpecificInput cPayspecificData = new PaymentProduct5100SpecificInput();
//            cPayspecificData.setBrand();
//
//            cardPaymentMethodSpecificInput.setPaymentProduct5100SpecificInput(cPayspecificData);
//
//            HostedCheckoutSpecificInput hostedCheckoutSpecificInput = new HostedCheckoutSpecificInput();
//            hostedCheckoutSpecificInput.setLocale();
//            hostedCheckoutSpecificInput.setReturnUrl();
//
//            request.setCardPaymentMethodSpecificInput();
            final CreateHostedCheckoutResponse hostedCheckout = client.merchant(getMerchantId()).hostedCheckout().createHostedCheckout(request);

            return hostedCheckout;

        } catch (IOException e) {
            LOGGER.error("[ INGENICO ] Errors during getting createHostedCheckout ", e);
            //TODO Throw Logical Exception
            return null;
        }
    }

    public void setIngenicoAmountUtils(IngenicoAmountUtils ingenicoAmountUtils) {
        this.ingenicoAmountUtils = ingenicoAmountUtils;
    }

    public void setIngenicoClientFactory(IngenicoClientFactory ingenicoClientFactory) {
        this.ingenicoClientFactory = ingenicoClientFactory;
    }
}
