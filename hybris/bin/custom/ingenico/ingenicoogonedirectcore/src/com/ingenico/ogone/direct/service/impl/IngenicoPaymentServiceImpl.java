package com.ingenico.ogone.direct.service.impl;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;
import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNullStandardMessage;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.order.CartService;
import de.hybris.platform.servicelayer.dto.converter.Converter;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;

import com.ingenico.direct.ApiException;
import com.ingenico.direct.Client;
import com.ingenico.direct.DeclinedPaymentException;
import com.ingenico.direct.domain.AmountOfMoney;
import com.ingenico.direct.domain.BrowserData;
import com.ingenico.direct.domain.CancelPaymentResponse;
import com.ingenico.direct.domain.Capture;
import com.ingenico.direct.domain.CapturePaymentRequest;
import com.ingenico.direct.domain.CaptureResponse;
import com.ingenico.direct.domain.CapturesResponse;
import com.ingenico.direct.domain.CreateHostedCheckoutRequest;
import com.ingenico.direct.domain.CreateHostedCheckoutResponse;
import com.ingenico.direct.domain.CreateHostedTokenizationRequest;
import com.ingenico.direct.domain.CreateHostedTokenizationResponse;
import com.ingenico.direct.domain.CreatePaymentRequest;
import com.ingenico.direct.domain.CreatePaymentResponse;
import com.ingenico.direct.domain.CustomerDevice;
import com.ingenico.direct.domain.DirectoryEntry;
import com.ingenico.direct.domain.GetHostedCheckoutResponse;
import com.ingenico.direct.domain.GetHostedTokenizationResponse;
import com.ingenico.direct.domain.GetPaymentProductsResponse;
import com.ingenico.direct.domain.PaymentProduct;
import com.ingenico.direct.domain.PaymentResponse;
import com.ingenico.direct.domain.ProductDirectory;
import com.ingenico.direct.domain.RefundRequest;
import com.ingenico.direct.domain.RefundResponse;
import com.ingenico.direct.domain.TokenResponse;
import com.ingenico.direct.merchant.products.GetPaymentProductParams;
import com.ingenico.direct.merchant.products.GetPaymentProductsParams;
import com.ingenico.direct.merchant.products.GetProductDirectoryParams;
import com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants;
import com.ingenico.ogone.direct.factory.IngenicoClientFactory;
import com.ingenico.ogone.direct.model.IngenicoConfigurationModel;
import com.ingenico.ogone.direct.order.data.IngenicoHostedTokenizationData;
import com.ingenico.ogone.direct.service.IngenicoConfigurationService;
import com.ingenico.ogone.direct.service.IngenicoPaymentService;
import com.ingenico.ogone.direct.util.IngenicoAmountUtils;
import com.ingenico.ogone.direct.util.IngenicoLogUtils;

public class IngenicoPaymentServiceImpl implements IngenicoPaymentService {

    private final static Logger LOGGER = LoggerFactory.getLogger(IngenicoPaymentServiceImpl.class);

    private CartService cartService;
    private IngenicoConfigurationService ingenicoConfigurationService;
    private IngenicoAmountUtils ingenicoAmountUtils;
    private IngenicoClientFactory ingenicoClientFactory;
    private Converter<AbstractOrderModel, CreatePaymentRequest> ingenicoHostedTokenizationParamConverter;
    private Converter<AbstractOrderModel, CreateHostedCheckoutRequest> ingenicoHostedCheckoutParamConverter;

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
            params.setHide(Collections.singletonList("fields"));
            final GetPaymentProductsResponse paymentProducts = client.merchant(getMerchantId()).products().getPaymentProducts(params);

            IngenicoLogUtils.logAction(LOGGER, "getPaymentProducts", params, paymentProducts);

            return paymentProducts;
        } catch (IOException e) {
            LOGGER.error("[ INGENICO ] Errors during getting PaymentProducts ", e);
            //TODO Throw Logical Exception
            return null;
        }
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
    @Cacheable(value = "productDirectory", key = "T(com.ingenico.ogone.direct.cache.IngenicoCacheKeyGenerator).generateKey(true,'directory',#id,#currency,#countryCode)")
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
    @Cacheable(value = "productDirectory", key = "T(com.ingenico.ogone.direct.cache.IngenicoCacheKeyGenerator).generateKey(true,'entry',#id,#currency,#countryCode)")
    public List<DirectoryEntry> getProductDirectoryEntries(Integer id, String currency, String countryCode) {
        return getProductDirectory(id, currency, countryCode).getEntries();
    }

    @Override
    public CreateHostedTokenizationResponse createHostedTokenization(String shopperLocale, List<String> savedTokens) {
        validateParameterNotNull(shopperLocale, "shopperLocale cannot be null");
        try (Client client = ingenicoClientFactory.getClient()) {
            final IngenicoConfigurationModel currentIngenicoConfiguration = ingenicoConfigurationService.getCurrentIngenicoConfiguration();
            CreateHostedTokenizationRequest params = new CreateHostedTokenizationRequest();
            params.setLocale(shopperLocale);
            if (StringUtils.isNotBlank(currentIngenicoConfiguration.getVariant())) {
                params.setVariant(currentIngenicoConfiguration.getVariant());
            }
            params.setAskConsumerConsent(BooleanUtils.isTrue(currentIngenicoConfiguration.getAskConsumerConsent()));
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
    public CreatePaymentResponse createPaymentForHostedTokenization(OrderModel orderForCode, IngenicoHostedTokenizationData ingenicoHostedTokenizationData, GetHostedTokenizationResponse tokenizationResponse) {
        validateParameterNotNull(tokenizationResponse, "tokenizationResponse cannot be null");
        validateParameterNotNull(orderForCode, "order cannot be null");
        try (Client client = ingenicoClientFactory.getClient()) {

            final CreatePaymentRequest params = ingenicoHostedTokenizationParamConverter.convert(orderForCode);
            params.getCardPaymentMethodSpecificInput()
                    .setToken(tokenizationResponse.getToken().getId());
            params.getCardPaymentMethodSpecificInput()
                    .setPaymentProductId(tokenizationResponse.getToken().getPaymentProductId());
            params.getOrder().getCustomer().setDevice(getBrowserInfo(ingenicoHostedTokenizationData.getBrowserData()));
            final CreatePaymentResponse payment = client.merchant(getMerchantId()).payments().createPayment(params);

            IngenicoLogUtils.logAction(LOGGER, "createPaymentForHostedTokenization", params, payment);

            return payment;
        } catch (DeclinedPaymentException e) {
            LOGGER.error("[ INGENICO ] Errors during getting createPayment ", e);
        } catch (IOException e) {
            LOGGER.error("[ INGENICO ] Errors during getting createPayment ", e);
            //TODO Throw Logical Exception
        }
        return null;
    }

    @Override
    @SuppressWarnings("all")
    public CreateHostedCheckoutResponse createHostedCheckout(OrderModel orderForCode, com.ingenico.ogone.direct.order.data.BrowserData browserData) {
        validateParameterNotNullStandardMessage("browserData", browserData);
        validateParameterNotNull(orderForCode, "order cannot be null");
        try (Client client = ingenicoClientFactory.getClient()) {
            final CreateHostedCheckoutRequest params = ingenicoHostedCheckoutParamConverter.convert(orderForCode);
            params.getOrder().getCustomer().setDevice(getBrowserInfo(browserData));
            final CreateHostedCheckoutResponse hostedCheckout = client.merchant(getMerchantId()).hostedCheckout().createHostedCheckout(params);

            IngenicoLogUtils.logAction(LOGGER, "createHostedCheckout", params, hostedCheckout);

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

            IngenicoLogUtils.logAction(LOGGER, "getHostedCheckout", hostedCheckoutId, hostedCheckoutResponse);

            return hostedCheckoutResponse;
        } catch (IOException e) {
            LOGGER.error("[ INGENICO ] Errors during getting createHostedCheckout ", e);
            //TODO Throw Logical Exception
            return null;
        }

    }

    @Override
    public TokenResponse getToken(String tokenId) {
        validateParameterNotNullStandardMessage("tokenId", tokenId);

        try (Client client = ingenicoClientFactory.getClient()) {
            final TokenResponse tokenResponse = client.merchant(getMerchantId()).tokens().getToken(tokenId);

            IngenicoLogUtils.logAction(LOGGER, "getToken", tokenId, tokenResponse);

            return tokenResponse;
        } catch (IOException e) {
            LOGGER.error("[ INGENICO ] Errors during getToken", e);
            return null;
        } catch (ApiException e) {
            LOGGER.info("[ INGENICO ] token not found!", e);
            return null;
        }
    }

    @Override
    public void deleteToken(String tokenId) {
        validateParameterNotNullStandardMessage("tokenId", tokenId);

        try (Client client = ingenicoClientFactory.getClient()) {
            client.merchant(getMerchantId()).tokens().deleteToken(tokenId);
            IngenicoLogUtils.logAction(LOGGER, "deleteToken", tokenId, "Token deleted!");
        } catch (IOException e) {
            LOGGER.error("[ INGENICO ] Errors during deleteToken", e);
        }
    }

    @Override
    public PaymentResponse getPayment(String paymentId) {
        validateParameterNotNull(paymentId, "paymentId cannot be null");
        try (Client client = ingenicoClientFactory.getClient()) {

            final PaymentResponse payment = client.merchant(getMerchantId()).payments().getPayment(paymentId);

            IngenicoLogUtils.logAction(LOGGER, "getPayment", paymentId, payment);

            return payment;
        } catch (IOException e) {
            LOGGER.error("[ INGENICO ] Errors during getting getPayment", e);
            //TODO Throw Logical Exception
            return null;
        }
    }

    @Override
    public CapturesResponse getCaptures(IngenicoConfigurationModel ingenicoConfigurationModel, String paymentId) {
        validateParameterNotNull(paymentId, "paymentId cannot be null");
        try (Client client = ingenicoClientFactory.getClient(ingenicoConfigurationModel)) {

            final CapturesResponse captures = client.merchant(ingenicoConfigurationModel.getMerchantID()).payments().getCaptures(paymentId);

            IngenicoLogUtils.logAction(LOGGER, "getCaptures", paymentId, captures);

            return captures;
        } catch (IOException e) {
            LOGGER.error("[ INGENICO ] Errors during getting getPayment", e);
            //TODO Throw Logical Exception
            return null;
        }
    }

    @Override
    public CaptureResponse capturePayment(IngenicoConfigurationModel ingenicoConfigurationModel, String paymentId, BigDecimal amountToCapture, String currencyISOcode, Boolean isFinal) {

        try (Client client = ingenicoClientFactory.getClient(ingenicoConfigurationModel)) {
            CapturePaymentRequest capturePaymentRequest = new CapturePaymentRequest();
            capturePaymentRequest.setAmount(ingenicoAmountUtils.createAmount(amountToCapture, currencyISOcode));
            capturePaymentRequest.setIsFinal(isFinal);

            CaptureResponse captureResponse =
                    client.merchant(ingenicoConfigurationModel.getMerchantID()).payments().capturePayment(paymentId, capturePaymentRequest);

            IngenicoLogUtils.logAction(LOGGER, "capturePayment", paymentId, captureResponse);

            return captureResponse;
        } catch (IOException e) {
            LOGGER.error("[ INGENICO ] Errors during getting capturePayment", e);
            //TODO Throw Logical Exception
            return null;
        }
    }

    @Override
    public Long getNonCapturedAmount(IngenicoConfigurationModel ingenicoConfigurationModel, String paymentId, BigDecimal plannedAmount, String currencyISOcode) {
        //Find if there was amount that was captured before performing the capture action
        CapturesResponse capturesResponse = getCaptures(ingenicoConfigurationModel, paymentId);
        return getNonCapturedAmount(ingenicoConfigurationModel, capturesResponse, plannedAmount, currencyISOcode);
    }

    @Override
    public Long getNonCapturedAmount(IngenicoConfigurationModel ingenicoConfigurationModel, CapturesResponse capturesResponse, BigDecimal plannedAmount, String currencyISOcode) {
        final long fullAmount = ingenicoAmountUtils.createAmount(plannedAmount, currencyISOcode);
        if (CollectionUtils.isEmpty(capturesResponse.getCaptures())) {
            return fullAmount;
        }

        Long amountPaid = 0L;
        for (Capture capture : capturesResponse.getCaptures()) {
            if (IngenicoogonedirectcoreConstants.PAYMENT_STATUS_ENUM.CAPTURED.getValue().equals(capture.getStatus()) ||
                    IngenicoogonedirectcoreConstants.PAYMENT_STATUS_ENUM.CAPTURE_REQUESTED.getValue().equals(capture.getStatus())) {
                amountPaid += capture.getCaptureOutput().getAmountOfMoney().getAmount();
            }
        }
        return fullAmount - amountPaid;
    }

    @Override
    public CancelPaymentResponse cancelPayment(IngenicoConfigurationModel ingenicoConfigurationModel, String paymentId) {
        try (Client client = ingenicoClientFactory.getClient(ingenicoConfigurationModel)) {

            CancelPaymentResponse cancelPaymentResponse =
                    client.merchant(ingenicoConfigurationModel.getMerchantID()).payments().cancelPayment(paymentId);

            IngenicoLogUtils.logAction(LOGGER, "cancelPayment", paymentId, cancelPaymentResponse);

            return cancelPaymentResponse;
        } catch (IOException e) {
            LOGGER.error("[ INGENICO ] Errors during getting cancelPayment", e);
            //TODO Throw Logical Exception
            return null;
        }
    }

    @Override
    public RefundResponse refundPayment(IngenicoConfigurationModel ingenicoConfigurationModel, String paymentId,  BigDecimal returnAmount, String currencyISOCode) {
        try (Client client = ingenicoClientFactory.getClient(ingenicoConfigurationModel)) {

            RefundRequest refundRequest = new RefundRequest();
            AmountOfMoney amountOfMoney = new AmountOfMoney();
            amountOfMoney.setCurrencyCode(currencyISOCode);
            amountOfMoney.setAmount(ingenicoAmountUtils.createAmount(returnAmount, currencyISOCode));
            refundRequest.setAmountOfMoney(amountOfMoney);
            RefundResponse refundResponse =
                  client.merchant(ingenicoConfigurationModel.getMerchantID()).payments().refundPayment(paymentId, refundRequest);
            IngenicoLogUtils.logAction(LOGGER, "refundPayment", paymentId, refundResponse);

            return refundResponse;
        } catch (IOException e) {
            LOGGER.error("[ INGENICO ] Errors during getting refundPayment", e);
            //TODO Throw Logical Exception
            return null;
        }
    }

    private CustomerDevice getBrowserInfo(com.ingenico.ogone.direct.order.data.BrowserData internalBrowserData) {
        BrowserData browserData = new BrowserData();
        browserData.setColorDepth(internalBrowserData.getColorDepth());
        browserData.setJavaEnabled(internalBrowserData.getNavigatorJavaEnabled());
        browserData.setJavaScriptEnabled(internalBrowserData.getNavigatorJavaScriptEnabled());
        browserData.setScreenHeight(internalBrowserData.getScreenHeight());
        browserData.setScreenWidth(internalBrowserData.getScreenWidth());

        CustomerDevice browserInfo = new CustomerDevice();
        browserInfo.setAcceptHeader(internalBrowserData.getAcceptHeader());
        browserInfo.setUserAgent(internalBrowserData.getUserAgent());
        browserInfo.setLocale(internalBrowserData.getLocale());
        browserInfo.setIpAddress(internalBrowserData.getIpAddress());
        browserInfo.setTimezoneOffsetUtcMinutes(internalBrowserData.getTimezoneOffsetUtcMinutes());
        browserInfo.setBrowserData(browserData);

        return browserInfo;
    }

    private String getMerchantId() {
        return ingenicoConfigurationService.getCurrentMerchantId();
    }

    private CartModel getSessionCart() {
        if (cartService.hasSessionCart()) {
            return cartService.getSessionCart();
        }
        return null;
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

    public void setIngenicoConfigurationService(IngenicoConfigurationService ingenicoConfigurationService) {
        this.ingenicoConfigurationService = ingenicoConfigurationService;
    }

    public void setIngenicoHostedTokenizationParamConverter(Converter<AbstractOrderModel, CreatePaymentRequest> ingenicoHostedTokenizationParamConverter) {
        this.ingenicoHostedTokenizationParamConverter = ingenicoHostedTokenizationParamConverter;
    }

    public void setIngenicoHostedCheckoutParamConverter(Converter<AbstractOrderModel, CreateHostedCheckoutRequest> ingenicoHostedCheckoutParamConverter) {
        this.ingenicoHostedCheckoutParamConverter = ingenicoHostedCheckoutParamConverter;
    }
}
