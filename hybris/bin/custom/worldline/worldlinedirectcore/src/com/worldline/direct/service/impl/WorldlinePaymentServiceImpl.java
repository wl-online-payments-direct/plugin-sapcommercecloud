package com.worldline.direct.service.impl;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;
import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNullStandardMessage;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import com.ingenico.direct.domain.*;
import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import com.worldline.direct.exception.WorldlineNonAuthorizedPaymentException;
import com.worldline.direct.factory.WorldlineClientFactory;
import com.worldline.direct.service.WorldlineConfigurationService;
import com.worldline.direct.service.WorldlinePaymentService;
import com.worldline.direct.util.WorldlineAmountUtils;
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
import com.ingenico.direct.merchant.products.GetPaymentProductParams;
import com.ingenico.direct.merchant.products.GetPaymentProductsParams;
import com.ingenico.direct.merchant.products.GetProductDirectoryParams;
import com.worldline.direct.model.WorldlineConfigurationModel;
import com.worldline.direct.order.data.WorldlineHostedTokenizationData;
import com.worldline.direct.util.WorldlineLogUtils;

public class WorldlinePaymentServiceImpl implements WorldlinePaymentService {

    private final static Logger LOGGER = LoggerFactory.getLogger(WorldlinePaymentServiceImpl.class);

    private CartService cartService;
    private WorldlineConfigurationService worldlineConfigurationService;
    private WorldlineAmountUtils worldlineAmountUtils;
    private WorldlineClientFactory worldlineClientFactory;
    private Converter<AbstractOrderModel, CreatePaymentRequest> worldlineHostedTokenizationParamConverter;
    private Converter<AbstractOrderModel, CreateHostedCheckoutRequest> worldlineHostedCheckoutParamConverter;

    @Override
    public GetPaymentProductsResponse getPaymentProductsResponse(BigDecimal amount, String currency, String countryCode, String shopperLocale) {
        validateParameterNotNull(amount, "amount cannot be null");
        validateParameterNotNull(currency, "currency cannot be null");
        validateParameterNotNull(countryCode, "countryCode cannot be null");
        validateParameterNotNull(shopperLocale, "shopperLocale cannot be null");

        try (Client client = worldlineClientFactory.getClient()) {
            final GetPaymentProductsParams params = new GetPaymentProductsParams();
            params.setCurrencyCode(currency);
            params.setAmount(worldlineAmountUtils.createAmount(amount, currency));
            params.setCountryCode(countryCode);
            params.setLocale(shopperLocale);
            params.setHide(Collections.singletonList("fields"));
            final GetPaymentProductsResponse paymentProducts = client.merchant(getMerchantId()).products().getPaymentProducts(params);

            WorldlineLogUtils.logAction(LOGGER, "getPaymentProducts", params, paymentProducts);

            return paymentProducts;
        } catch (IOException e) {
            LOGGER.error("[ WORLDLINE ] Errors during getting PaymentProducts ", e);
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

        try (Client client = worldlineClientFactory.getClient()) {

            final GetPaymentProductParams params = new GetPaymentProductParams();
            params.setCurrencyCode(currency);
            params.setAmount(worldlineAmountUtils.createAmount(amount, currency));
            params.setCountryCode(countryCode);
            params.setLocale(shopperLocale);

            final PaymentProduct paymentProduct = client.merchant(getMerchantId()).products().getPaymentProduct(id, params);

            WorldlineLogUtils.logAction(LOGGER, "getPaymentProduct", params, paymentProduct);

            return paymentProduct;
        } catch (IOException e) {
            LOGGER.error("[ WORLDLINE ] Errors during getting PaymentProduct ", e);
            //TODO Throw Logical Exception
            return null;
        }
    }

    @Override
    @Cacheable(value = "productDirectory", key = "T(com.worldline.direct.cache.WorldlineCacheKeyGenerator).generateKey(true,'directory',#id,#currency,#countryCode)")
    public ProductDirectory getProductDirectory(Integer id, String currency, String countryCode) {
        validateParameterNotNull(id, "id cannot be null");
        validateParameterNotNull(currency, "currency cannot be null");
        validateParameterNotNull(countryCode, "countryCode cannot be null");

        try (Client client = worldlineClientFactory.getClient()) {

            final GetProductDirectoryParams params = new GetProductDirectoryParams();
            params.setCurrencyCode(currency);
            params.setCountryCode(countryCode);

            final ProductDirectory productDirectory = client.merchant(getMerchantId()).products().getProductDirectory(id, params);

            WorldlineLogUtils.logAction(LOGGER, "getProductDirectory", params, productDirectory);

            return productDirectory;
        } catch (IOException e) {
            LOGGER.error("[ WORLDLINE ] Errors during getting productDirectory ", e);
            //TODO Throw Logical Exception
            return null;
        }
    }

    @Override
    @Cacheable(value = "productDirectory", key = "T(com.worldline.direct.cache.WorldlineCacheKeyGenerator).generateKey(true,'entry',#id,#currency,#countryCode)")
    public List<DirectoryEntry> getProductDirectoryEntries(Integer id, String currency, String countryCode) {
        return getProductDirectory(id, currency, countryCode).getEntries();
    }

    @Override
    public CreateHostedTokenizationResponse createHostedTokenization(String shopperLocale, List<String> savedTokens) {
        validateParameterNotNull(shopperLocale, "shopperLocale cannot be null");
        try (Client client = worldlineClientFactory.getClient()) {
            final WorldlineConfigurationModel currentWorldlineConfiguration = worldlineConfigurationService.getCurrentWorldlineConfiguration();
            CreateHostedTokenizationRequest params = new CreateHostedTokenizationRequest();
            params.setLocale(shopperLocale);
            if (StringUtils.isNotBlank(currentWorldlineConfiguration.getVariant())) {
                params.setVariant(currentWorldlineConfiguration.getVariant());
            }
            params.setAskConsumerConsent(BooleanUtils.isTrue(currentWorldlineConfiguration.getAskConsumerConsent()));
            if (CollectionUtils.isNotEmpty(savedTokens)) {
                params.setTokens(String.join(",", savedTokens));
            }
            final CreateHostedTokenizationResponse hostedTokenization = client.merchant(getMerchantId()).hostedTokenization().createHostedTokenization(params);

            WorldlineLogUtils.logAction(LOGGER, "createHostedTokenization", params, hostedTokenization);

            return hostedTokenization;
        } catch (IOException e) {
            LOGGER.error("[ WORLDLINE ] Errors during getting createHostedTokenization ", e);
            //TODO Throw Logical Exception
            return null;
        }
    }

    @Override
    public GetHostedTokenizationResponse getHostedTokenization(String hostedTokenizationId) {
        validateParameterNotNull(hostedTokenizationId, "hostedTokenizationId cannot be null");
        try (Client client = worldlineClientFactory.getClient()) {

            final GetHostedTokenizationResponse hostedTokenization = client.merchant(getMerchantId()).hostedTokenization().getHostedTokenization(hostedTokenizationId);

            WorldlineLogUtils.logAction(LOGGER, "getHostedTokenization", hostedTokenizationId, hostedTokenization);

            return hostedTokenization;
        } catch (IOException e) {
            LOGGER.error("[ WORLDLINE ] Errors during getting getHostedTokenization ", e);
            //TODO Throw Logical Exception
            return null;
        }
    }

    @Override
    @SuppressWarnings("all")
    public CreatePaymentResponse createPaymentForHostedTokenization(OrderModel orderForCode, WorldlineHostedTokenizationData worldlineHostedTokenizationData, GetHostedTokenizationResponse tokenizationResponse) throws WorldlineNonAuthorizedPaymentException {
        validateParameterNotNull(tokenizationResponse, "tokenizationResponse cannot be null");
        validateParameterNotNull(orderForCode, "order cannot be null");
        try (Client client = worldlineClientFactory.getClient()) {

            final CreatePaymentRequest params = worldlineHostedTokenizationParamConverter.convert(orderForCode);
            params.getCardPaymentMethodSpecificInput()
                    .setToken(tokenizationResponse.getToken().getId());
            params.getCardPaymentMethodSpecificInput()
                    .setPaymentProductId(tokenizationResponse.getToken().getPaymentProductId());
            params.getOrder().getCustomer().setDevice(getBrowserInfo(worldlineHostedTokenizationData.getBrowserData()));
            final CreatePaymentResponse payment = client.merchant(getMerchantId()).payments().createPayment(params);

            WorldlineLogUtils.logAction(LOGGER, "createPaymentForHostedTokenization", params, payment);

            return payment;
        } catch (DeclinedPaymentException e) {
            LOGGER.debug("[ WORLDLINE ] Errors during getting createPayment ", e.getMessage());
            throw new WorldlineNonAuthorizedPaymentException(WorldlinedirectcoreConstants.UNAUTHORIZED_REASON.REJECTED);
        } catch (IOException e) {
            LOGGER.error("[ WORLDLINE ] Errors during getting createPayment ", e);
            //TODO Throw Logical Exception
        }
        return null;
    }

    @Override
    @SuppressWarnings("all")
    public CreateHostedCheckoutResponse createHostedCheckout(OrderModel orderForCode, com.worldline.direct.order.data.BrowserData browserData) {
        validateParameterNotNullStandardMessage("browserData", browserData);
        validateParameterNotNull(orderForCode, "order cannot be null");
        try (Client client = worldlineClientFactory.getClient()) {
            final CreateHostedCheckoutRequest params = worldlineHostedCheckoutParamConverter.convert(orderForCode);
            params.getOrder().getCustomer().setDevice(getBrowserInfo(browserData));
            final CreateHostedCheckoutResponse hostedCheckout = client.merchant(getMerchantId()).hostedCheckout().createHostedCheckout(params);

            WorldlineLogUtils.logAction(LOGGER, "createHostedCheckout", params, hostedCheckout);

            return hostedCheckout;

        } catch (IOException e) {
            LOGGER.error("[ WORLDLINE ] Errors during getting createHostedCheckout ", e);
            //TODO Throw Logical Exception
            return null;
        }
    }

    @Override
    public GetHostedCheckoutResponse getHostedCheckout(String hostedCheckoutId) {
        try (Client client = worldlineClientFactory.getClient()) {
            final GetHostedCheckoutResponse hostedCheckoutResponse = client.merchant(getMerchantId()).hostedCheckout().getHostedCheckout(hostedCheckoutId);

            WorldlineLogUtils.logAction(LOGGER, "getHostedCheckout", hostedCheckoutId, hostedCheckoutResponse);

            return hostedCheckoutResponse;
        } catch (IOException e) {
            LOGGER.error("[ WORLDLINE ] Errors during getting createHostedCheckout ", e);
            //TODO Throw Logical Exception
            return null;
        }

    }

    @Override
    public TokenResponse getToken(String tokenId) {
        validateParameterNotNullStandardMessage("tokenId", tokenId);

        try (Client client = worldlineClientFactory.getClient()) {
            final TokenResponse tokenResponse = client.merchant(getMerchantId()).tokens().getToken(tokenId);

            WorldlineLogUtils.logAction(LOGGER, "getToken", tokenId, tokenResponse);

            return tokenResponse;
        } catch (IOException e) {
            LOGGER.error("[ WORLDLINE ] Errors during getToken", e);
            return null;
        } catch (ApiException e) {
            LOGGER.info("[ WORLDLINE ] token not found!", e);
            return null;
        }
    }

    @Override
    public void deleteToken(String tokenId) {
        validateParameterNotNullStandardMessage("tokenId", tokenId);

        try (Client client = worldlineClientFactory.getClient()) {
            client.merchant(getMerchantId()).tokens().deleteToken(tokenId);
            WorldlineLogUtils.logAction(LOGGER, "deleteToken", tokenId, "Token deleted!");
        } catch (IOException e) {
            LOGGER.error("[ WORLDLINE ] Errors during deleteToken", e);
        }
    }

    @Override
    public PaymentResponse getPayment(String paymentId) {
        validateParameterNotNull(paymentId, "paymentId cannot be null");
        try (Client client = worldlineClientFactory.getClient()) {

            final PaymentResponse payment = client.merchant(getMerchantId()).payments().getPayment(paymentId);

            WorldlineLogUtils.logAction(LOGGER, "getPayment", paymentId, payment);

            return payment;
        } catch (IOException e) {
            LOGGER.error("[ WORLDLINE ] Errors during getting getPayment", e);
            //TODO Throw Logical Exception
            return null;
        }
    }

    @Override
    public PaymentResponse getPayment(WorldlineConfigurationModel worldlineConfigurationModel, String paymentId) {
        validateParameterNotNull(paymentId, "paymentId cannot be null");
        try (Client client = worldlineClientFactory.getClient(worldlineConfigurationModel)) {

            final PaymentResponse payment = client.merchant(worldlineConfigurationModel.getMerchantID()).payments().getPayment(paymentId);

            WorldlineLogUtils.logAction(LOGGER, "getPayment", paymentId, payment);

            return payment;
        } catch (IOException e) {
            LOGGER.error("[ WORLDLINE ] Errors during getting getPayment", e);
            //TODO Throw Logical Exception
            return null;
        }
    }

    @Override
    public CapturesResponse getCaptures(WorldlineConfigurationModel worldlineConfigurationModel, String paymentId) {
        validateParameterNotNull(paymentId, "paymentId cannot be null");
        try (Client client = worldlineClientFactory.getClient(worldlineConfigurationModel)) {

            final CapturesResponse captures = client.merchant(worldlineConfigurationModel.getMerchantID()).payments().getCaptures(paymentId);

            WorldlineLogUtils.logAction(LOGGER, "getCaptures", paymentId, captures);

            return captures;
        } catch (IOException e) {
            LOGGER.error("[ WORLDLINE ] Errors during getting getPayment", e);
            //TODO Throw Logical Exception
            return null;
        }
    }

    @Override
    public CaptureResponse capturePayment(WorldlineConfigurationModel worldlineConfigurationModel, String paymentId, BigDecimal amountToCapture, String currencyISOcode, Boolean isFinal) {

        try (Client client = worldlineClientFactory.getClient(worldlineConfigurationModel)) {
            CapturePaymentRequest capturePaymentRequest = new CapturePaymentRequest();
            capturePaymentRequest.setAmount(worldlineAmountUtils.createAmount(amountToCapture, currencyISOcode));
            capturePaymentRequest.setIsFinal(isFinal);

            CaptureResponse captureResponse =
                    client.merchant(worldlineConfigurationModel.getMerchantID()).payments().capturePayment(paymentId, capturePaymentRequest);

            WorldlineLogUtils.logAction(LOGGER, "capturePayment", paymentId, captureResponse);

            return captureResponse;
        } catch (IOException e) {
            LOGGER.error("[ WORLDLINE ] Errors during getting capturePayment", e);
            //TODO Throw Logical Exception
            return null;
        }
    }

    @Override
    public Long getNonCapturedAmount(WorldlineConfigurationModel worldlineConfigurationModel, String paymentId, BigDecimal plannedAmount, String currencyISOcode) {
        //Find if there was amount that was captured before performing the capture action
        CapturesResponse capturesResponse = getCaptures(worldlineConfigurationModel, paymentId);
        return getNonCapturedAmount(worldlineConfigurationModel, paymentId, capturesResponse, plannedAmount, currencyISOcode);
    }

    @Override
    public Long getNonCapturedAmount(WorldlineConfigurationModel worldlineConfigurationModel, String paymentId, CapturesResponse capturesResponse, BigDecimal plannedAmount, String currencyISOcode) {
        final long fullAmount = worldlineAmountUtils.createAmount(plannedAmount, currencyISOcode);
        Long amountPaid = 0L;

        final PaymentResponse paymentResponse = getPayment(worldlineConfigurationModel,paymentId);
        if (WorldlinedirectcoreConstants.PAYMENT_STATUS_ENUM.valueOf(paymentResponse.getStatus()).equals(WorldlinedirectcoreConstants.PAYMENT_STATUS_ENUM.CAPTURED)) {
            amountPaid = paymentResponse.getPaymentOutput().getAmountOfMoney().getAmount();
        }

        if (CollectionUtils.isEmpty(capturesResponse.getCaptures())) {
            return fullAmount - amountPaid;
        }


        for (Capture capture : capturesResponse.getCaptures()) {
            if (WorldlinedirectcoreConstants.PAYMENT_STATUS_ENUM.CAPTURED.getValue().equals(capture.getStatus()) ||
                    WorldlinedirectcoreConstants.PAYMENT_STATUS_ENUM.CAPTURE_REQUESTED.getValue().equals(capture.getStatus())) {
                amountPaid += capture.getCaptureOutput().getAmountOfMoney().getAmount();
            }
        }
        return fullAmount - amountPaid;
    }

    @Override
    public CancelPaymentResponse cancelPayment(WorldlineConfigurationModel worldlineConfigurationModel, String paymentId) {
        try (Client client = worldlineClientFactory.getClient(worldlineConfigurationModel)) {

            CancelPaymentResponse cancelPaymentResponse =
                    client.merchant(worldlineConfigurationModel.getMerchantID()).payments().cancelPayment(paymentId);

            WorldlineLogUtils.logAction(LOGGER, "cancelPayment", paymentId, cancelPaymentResponse);

            return cancelPaymentResponse;
        } catch (IOException e) {
            LOGGER.error("[ WORLDLINE ] Errors during getting cancelPayment", e);
            //TODO Throw Logical Exception
            return null;
        }
    }

    @Override
    public RefundResponse refundPayment(WorldlineConfigurationModel worldlineConfigurationModel, String paymentId, BigDecimal returnAmount, String currencyISOCode) {
        try (Client client = worldlineClientFactory.getClient(worldlineConfigurationModel)) {

            RefundRequest refundRequest = new RefundRequest();
            AmountOfMoney amountOfMoney = new AmountOfMoney();
            amountOfMoney.setCurrencyCode(currencyISOCode);
            amountOfMoney.setAmount(worldlineAmountUtils.createAmount(returnAmount, currencyISOCode));
            refundRequest.setAmountOfMoney(amountOfMoney);
            RefundResponse refundResponse =
                    client.merchant(worldlineConfigurationModel.getMerchantID()).payments().refundPayment(paymentId, refundRequest);
            WorldlineLogUtils.logAction(LOGGER, "refundPayment", paymentId, refundResponse);

            return refundResponse;
        } catch (IOException e) {
            LOGGER.error("[ WORLDLINE ] Errors during getting refundPayment", e);
            //TODO Throw Logical Exception
            return null;
        }
    }

    private CustomerDevice getBrowserInfo(com.worldline.direct.order.data.BrowserData internalBrowserData) {
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
        return worldlineConfigurationService.getCurrentMerchantId();
    }

    private CartModel getSessionCart() {
        if (cartService.hasSessionCart()) {
            return cartService.getSessionCart();
        }
        return null;
    }

    public void setCartService(CartService cartService) {
        this.cartService = cartService;
    }

    public void setWorldlineConfigurationService(WorldlineConfigurationService worldlineConfigurationService) {
        this.worldlineConfigurationService = worldlineConfigurationService;
    }

    public void setWorldlineAmountUtils(WorldlineAmountUtils worldlineAmountUtils) {
        this.worldlineAmountUtils = worldlineAmountUtils;
    }

    public void setWorldlineClientFactory(WorldlineClientFactory worldlineClientFactory) {
        this.worldlineClientFactory = worldlineClientFactory;
    }

    public void setWorldlineHostedTokenizationParamConverter(Converter<AbstractOrderModel, CreatePaymentRequest> worldlineHostedTokenizationParamConverter) {
        this.worldlineHostedTokenizationParamConverter = worldlineHostedTokenizationParamConverter;
    }

    public void setWorldlineHostedCheckoutParamConverter(Converter<AbstractOrderModel, CreateHostedCheckoutRequest> worldlineHostedCheckoutParamConverter) {
        this.worldlineHostedCheckoutParamConverter = worldlineHostedCheckoutParamConverter;
    }
}
