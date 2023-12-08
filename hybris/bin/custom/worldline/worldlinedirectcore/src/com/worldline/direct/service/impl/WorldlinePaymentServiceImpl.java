package com.worldline.direct.service.impl;

import com.onlinepayments.ApiException;
import com.onlinepayments.DeclinedPaymentException;
import com.onlinepayments.domain.*;
import com.onlinepayments.merchant.MerchantClient;
import com.onlinepayments.merchant.products.GetPaymentProductParams;
import com.onlinepayments.merchant.products.GetPaymentProductsParams;
import com.onlinepayments.merchant.products.GetProductDirectoryParams;
import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import com.worldline.direct.exception.WorldlineNonAuthorizedPaymentException;
import com.worldline.direct.factory.WorldlineClientFactory;
import com.worldline.direct.model.WorldlineConfigurationModel;
import com.worldline.direct.model.WorldlineMandateModel;
import com.worldline.direct.order.data.WorldlineHostedTokenizationData;
import com.worldline.direct.service.WorldlineConfigurationService;
import com.worldline.direct.service.WorldlinePaymentService;
import com.worldline.direct.util.WorldlineAmountUtils;
import com.worldline.direct.util.WorldlineLogUtils;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.store.services.BaseStoreService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;
import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNullStandardMessage;

public class WorldlinePaymentServiceImpl implements WorldlinePaymentService {

    private final static Logger LOGGER = LoggerFactory.getLogger(WorldlinePaymentServiceImpl.class);

    protected WorldlineConfigurationService worldlineConfigurationService;
    protected WorldlineAmountUtils worldlineAmountUtils;
    protected WorldlineClientFactory worldlineClientFactory;
    protected Converter<AbstractOrderModel, CreatePaymentRequest> worldlineHostedTokenizationParamConverter;
    protected Converter<AbstractOrderModel, CreateHostedCheckoutRequest> worldlineHostedCheckoutParamConverter;
    protected Converter<com.worldline.direct.order.data.BrowserData, CustomerDevice> worldlineBrowserCustomerDeviceConverter;

    protected BaseStoreService baseStoreService;
    @Override
    public GetPaymentProductsResponse getPaymentProductsResponse(BigDecimal amount, String currency, String countryCode, String shopperLocale, Boolean isReplenishmentOrder) {
        validateParameterNotNull(amount, "amount cannot be null");
        validateParameterNotNull(currency, "currency cannot be null");
        validateParameterNotNull(countryCode, "countryCode cannot be null");
        validateParameterNotNull(shopperLocale, "shopperLocale cannot be null");

        try {
            MerchantClient merchant = worldlineClientFactory.getMerchantClient(getStoreId(), getMerchantId());

            final GetPaymentProductsParams params = new GetPaymentProductsParams();
            params.setCurrencyCode(currency);
            params.setAmount(worldlineAmountUtils.createAmount(amount, currency));
            params.setCountryCode(countryCode);
            params.setLocale(shopperLocale);
            params.setHide(Collections.singletonList("fields"));
            params.setIsRecurring(isReplenishmentOrder);
            final GetPaymentProductsResponse paymentProducts =merchant.products().getPaymentProducts(params);

            WorldlineLogUtils.logAction(LOGGER, "getPaymentProducts", params, paymentProducts);

            return paymentProducts;
        } catch (Exception e) {
            LOGGER.error("[ WORLDLINE ] Errors during getting PaymentProducts ", e);
            //TODO Throw Logical Exception
            return null;
        }
    }

    @Override
    public List<PaymentProduct> getPaymentProducts(BigDecimal amount, String currency, String countryCode, String shopperLocale, Boolean isReplenishmentOrder) {
        final GetPaymentProductsResponse getPaymentProductsResponse = getPaymentProductsResponse(amount, currency, countryCode, shopperLocale, isReplenishmentOrder);
        return getPaymentProductsResponse.getPaymentProducts();
    }

    @Override
    public PaymentProduct getPaymentProduct(Integer id, BigDecimal amount, String currency, String countryCode, String shopperLocale) {
        validateParameterNotNull(id, "id cannot be null");
        validateParameterNotNull(amount, "amount cannot be null");
        validateParameterNotNull(currency, "currency cannot be null");
        validateParameterNotNull(countryCode, "countryCode cannot be null");
        validateParameterNotNull(shopperLocale, "shopperLocale cannot be null");

        try {
            MerchantClient merchant = worldlineClientFactory.getMerchantClient(getStoreId(), getMerchantId());

            final GetPaymentProductParams params = new GetPaymentProductParams();
            params.setCurrencyCode(currency);
            params.setAmount(worldlineAmountUtils.createAmount(amount, currency));
            params.setCountryCode(countryCode);
            params.setLocale(shopperLocale);

            final PaymentProduct paymentProduct = merchant.products().getPaymentProduct(id, params);

            WorldlineLogUtils.logAction(LOGGER, "getPaymentProduct", params, paymentProduct);

            return paymentProduct;
        } catch (Exception e) {
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

        try {

            MerchantClient merchant = worldlineClientFactory.getMerchantClient(getStoreId(), getMerchantId());

            final GetProductDirectoryParams params = new GetProductDirectoryParams();
            params.setCurrencyCode(currency);
            params.setCountryCode(countryCode);

            final ProductDirectory productDirectory = merchant.products().getProductDirectory(id, params);

            WorldlineLogUtils.logAction(LOGGER, "getProductDirectory", params, productDirectory);

            return productDirectory;
        } catch (Exception e) {
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
    public CreateHostedTokenizationResponse createHostedTokenization(String shopperLocale, List<String> savedTokens, Boolean isAnonymousUser) {
        validateParameterNotNull(shopperLocale, "shopperLocale cannot be null");
        try {
            MerchantClient merchant = worldlineClientFactory.getMerchantClient(getStoreId(), getMerchantId());

            final WorldlineConfigurationModel currentWorldlineConfiguration = worldlineConfigurationService.getCurrentWorldlineConfiguration();
            CreateHostedTokenizationRequest params = new CreateHostedTokenizationRequest();
            params.setLocale(shopperLocale);
            if (StringUtils.isNotBlank(currentWorldlineConfiguration.getVariant())) {
                params.setVariant(currentWorldlineConfiguration.getVariant());
            }
            params.setAskConsumerConsent(isAnonymousUser ? Boolean.FALSE : BooleanUtils.isTrue(currentWorldlineConfiguration.getAskConsumerConsent()));

            if (CollectionUtils.isNotEmpty(savedTokens)) {
                params.setTokens(String.join(",", savedTokens));
            }
            final CreateHostedTokenizationResponse hostedTokenization = merchant.hostedTokenization().createHostedTokenization(params);

            WorldlineLogUtils.logAction(LOGGER, "createHostedTokenization", params, hostedTokenization);

            return hostedTokenization;
        } catch (Exception e) {
            LOGGER.error("[ WORLDLINE ] Errors during getting createHostedTokenization ", e);
            //TODO Throw Logical Exception
            return null;
        }
    }

    @Override
    public GetHostedTokenizationResponse getHostedTokenization(String hostedTokenizationId) {
        validateParameterNotNull(hostedTokenizationId, "hostedTokenizationId cannot be null");
        try {
            MerchantClient merchant = worldlineClientFactory.getMerchantClient(getStoreId(), getMerchantId());

            final GetHostedTokenizationResponse hostedTokenization = merchant.hostedTokenization().getHostedTokenization(hostedTokenizationId);

            WorldlineLogUtils.logAction(LOGGER, "getHostedTokenization", hostedTokenizationId, hostedTokenization);

            return hostedTokenization;
        } catch (Exception e) {
            LOGGER.error("[ WORLDLINE ] Errors during getting getHostedTokenization ", e);
            //TODO Throw Logical Exception
            return null;
        }
    }

    @Override
    @SuppressWarnings("all")
    public CreatePaymentResponse createPaymentForHostedTokenization(OrderModel orderForCode, WorldlineHostedTokenizationData worldlineHostedTokenizationData) throws WorldlineNonAuthorizedPaymentException {
        validateParameterNotNull(orderForCode, "order cannot be null");
        try {
            MerchantClient merchant = worldlineClientFactory.getMerchantClient(getStoreId(), getMerchantId());

            final CreatePaymentRequest params = worldlineHostedTokenizationParamConverter.convert(orderForCode);
            params.getOrder().getCustomer().setDevice(worldlineBrowserCustomerDeviceConverter.convert(worldlineHostedTokenizationData.getBrowserData()));

            final CreatePaymentResponse payment = merchant.payments().createPayment(params);

            WorldlineLogUtils.logAction(LOGGER, "createPaymentForHostedTokenization", params, payment);

            return payment;
        } catch (DeclinedPaymentException e) {
            LOGGER.debug("[ WORLDLINE ] Errors during getting createPayment ", e.getMessage());
            throw new WorldlineNonAuthorizedPaymentException(WorldlinedirectcoreConstants.UNAUTHORIZED_REASON.REJECTED);
        } catch (Exception e) {
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
        try {
            MerchantClient merchant = worldlineClientFactory.getMerchantClient(getStoreId(), getMerchantId());

            final CreateHostedCheckoutRequest params = worldlineHostedCheckoutParamConverter.convert(orderForCode);
            params.getOrder().getCustomer().setDevice(worldlineBrowserCustomerDeviceConverter.convert(browserData));
            final CreateHostedCheckoutResponse hostedCheckout = merchant.hostedCheckout().createHostedCheckout(params);

            WorldlineLogUtils.logAction(LOGGER, "createHostedCheckout", params, hostedCheckout);

            return hostedCheckout;

        } catch (Exception e) {
            LOGGER.error("[ WORLDLINE ] Errors during getting createHostedCheckout ", e);
            //TODO Throw Logical Exception
            return null;
        }
    }

    @Override
    public CreateHostedCheckoutResponse createHostedCheckout(CartModel cartModel) {
        validateParameterNotNull(cartModel, "cart cannot be null");
        try {
            MerchantClient merchant = worldlineClientFactory.getMerchantClient(getStoreId(), getMerchantId());

            final CreateHostedCheckoutRequest params = worldlineHostedCheckoutParamConverter.convert(cartModel);
            final CreateHostedCheckoutResponse hostedCheckout = merchant.hostedCheckout().createHostedCheckout(params);

            WorldlineLogUtils.logAction(LOGGER, "createHostedCheckout", params, hostedCheckout);

            return hostedCheckout;

        } catch (Exception e) {
            LOGGER.error("[ WORLDLINE ] Errors during getting createHostedCheckout ", e);
            //TODO Throw Logical Exception
            return null;
        }
    }

    @Override
    public GetHostedCheckoutResponse getHostedCheckout(String hostedCheckoutId) {
        try {
            MerchantClient merchant = worldlineClientFactory.getMerchantClient(getStoreId(), getMerchantId());

            final GetHostedCheckoutResponse hostedCheckoutResponse = merchant.hostedCheckout().getHostedCheckout(hostedCheckoutId);

            WorldlineLogUtils.logAction(LOGGER, "getHostedCheckout", hostedCheckoutId, hostedCheckoutResponse);

            return hostedCheckoutResponse;
        } catch (Exception e) {
            LOGGER.error("[ WORLDLINE ] Errors during getting createHostedCheckout ", e);
            //TODO Throw Logical Exception
            return null;
        }

    }

    @Override
    public TokenResponse getToken(String tokenId) {
        validateParameterNotNullStandardMessage("tokenId", tokenId);

        try {
            MerchantClient merchant = worldlineClientFactory.getMerchantClient(getStoreId(), getMerchantId());

            final TokenResponse tokenResponse = merchant.tokens().getToken(tokenId);

            WorldlineLogUtils.logAction(LOGGER, "getToken", tokenId, tokenResponse);

            return tokenResponse;
        }  catch (ApiException e) {
            LOGGER.info("[ WORLDLINE ] token not found!", e);
            return null;
        } catch (Exception e) {
            LOGGER.error("[ WORLDLINE ] Errors during getToken", e);
            return null;
        }
    }

    @Override
    public void deleteToken(String tokenId) {
        validateParameterNotNullStandardMessage("tokenId", tokenId);

        try {
            MerchantClient merchant = worldlineClientFactory.getMerchantClient(getStoreId(), getMerchantId());

            merchant.tokens().deleteToken(tokenId);
            WorldlineLogUtils.logAction(LOGGER, "deleteToken", tokenId, "Token deleted!");
        } catch (Exception e) {
            LOGGER.error("[ WORLDLINE ] Errors during deleteToken", e);
        }
    }

    @Override
    public void deleteToken(String tokenId, String storeId) {
        validateParameterNotNullStandardMessage("tokenId", tokenId);

        try {
            MerchantClient merchant = worldlineClientFactory.getMerchantClient(storeId, getMerchantId(storeId));
            merchant.tokens().deleteToken(tokenId);
            WorldlineLogUtils.logAction(LOGGER, "deleteToken", tokenId, "Token deleted!");
        } catch (Exception e) {
            LOGGER.error("[ WORLDLINE ] Errors during deleteToken", e);
        }
    }

    @Override
    public GetMandateResponse getMandate(String uniqueMandateReference) {
        validateParameterNotNullStandardMessage("uniqueMandateReference", uniqueMandateReference);

        try {
            MerchantClient merchant = worldlineClientFactory.getMerchantClient(getStoreId(), getMerchantId());

            GetMandateResponse mandateResponse = merchant.mandates().getMandate(uniqueMandateReference);

            WorldlineLogUtils.logAction(LOGGER, "getMandate", uniqueMandateReference, mandateResponse);

            return mandateResponse;
        } catch (ApiException e) {
            LOGGER.info("[ WORLDLINE ] Errors during getMandate", e);
            return null;
        } catch (Exception e) {
            LOGGER.error("[ WORLDLINE ] Errors during getMandate", e);
            return null;
        }
    }

    @Override
    public GetMandateResponse getMandate(WorldlineMandateModel worldlineMandateModel) {
        validateParameterNotNullStandardMessage("uniqueMandateReference", worldlineMandateModel.getUniqueMandateReference());
        try {
            MerchantClient merchant = worldlineClientFactory.getMerchantClient(worldlineMandateModel.getStoreId(), getMerchantId(worldlineMandateModel.getStoreId()));
            GetMandateResponse mandateResponse = merchant.mandates().getMandate( worldlineMandateModel.getUniqueMandateReference());

            WorldlineLogUtils.logAction(LOGGER, "getMandate",  worldlineMandateModel.getUniqueMandateReference(), mandateResponse);

            return mandateResponse;
        } catch (ApiException e) {
            LOGGER.info("[ WORLDLINE ] Errors during getMandate", e);
            return null;
        } catch (Exception e) {
            LOGGER.error("[ WORLDLINE ] Errors during getMandate", e);
            return null;
        }
    }

    @Override
    public GetMandateResponse revokeMandate(WorldlineMandateModel worldlineMandateModel) {
        validateParameterNotNullStandardMessage("uniqueMandateReference", worldlineMandateModel.getUniqueMandateReference());
        try {
            MerchantClient merchant = worldlineClientFactory.getMerchantClient(worldlineMandateModel.getStoreId(), getMerchantId(worldlineMandateModel.getStoreId()));
            GetMandateResponse mandateResponse = merchant.mandates().revokeMandate(worldlineMandateModel.getUniqueMandateReference());

            WorldlineLogUtils.logAction(LOGGER, "revokeMandate", worldlineMandateModel.getUniqueMandateReference(), mandateResponse);

            return mandateResponse;
        } catch (ApiException e) {
            LOGGER.info("[ WORLDLINE ] Errors during revokeMandate", e);
            return null;
        } catch (Exception e) {
            LOGGER.error("[ WORLDLINE ] Errors during revokeMandate", e);
            return null;
        }
    }

    @Override
    public GetMandateResponse blockMandate(WorldlineMandateModel worldlineMandateModel) {
        validateParameterNotNullStandardMessage("uniqueMandateReference", worldlineMandateModel.getUniqueMandateReference());
        try {
            MerchantClient merchant = worldlineClientFactory.getMerchantClient(worldlineMandateModel.getStoreId(), getMerchantId(worldlineMandateModel.getStoreId()));
            GetMandateResponse mandateResponse = merchant.mandates().blockMandate(worldlineMandateModel.getUniqueMandateReference());

            WorldlineLogUtils.logAction(LOGGER, "blockMandate", worldlineMandateModel.getUniqueMandateReference(), mandateResponse);

            return mandateResponse;
        } catch (ApiException e) {
            LOGGER.info("[ WORLDLINE ] Errors during blockMandate", e);
            return null;
        } catch (Exception e) {
            LOGGER.error("[ WORLDLINE ] Errors during blockMandate", e);
            return null;
        }
    }
    @Override
    public GetMandateResponse unBlockMandate(WorldlineMandateModel worldlineMandateModel) {
        validateParameterNotNullStandardMessage("uniqueMandateReference", worldlineMandateModel.getUniqueMandateReference());

        try {
            MerchantClient merchant = worldlineClientFactory.getMerchantClient(worldlineMandateModel.getStoreId(), getMerchantId(worldlineMandateModel.getStoreId()));
            GetMandateResponse mandateResponse = merchant.mandates().unblockMandate(worldlineMandateModel.getUniqueMandateReference());

            WorldlineLogUtils.logAction(LOGGER, "unBlockMandate", worldlineMandateModel.getUniqueMandateReference(), mandateResponse);

            return mandateResponse;
        } catch (ApiException e) {
            LOGGER.info("[ WORLDLINE ] Errors during unBlockMandate", e);
            return null;
        } catch (Exception e) {
            LOGGER.error("[ WORLDLINE ] Errors during unBlockMandate", e);
            return null;
        }
    }

    @Override
    public CalculateSurchargeResponse calculateSurcharge(String hostedTokenizationId, String token, AbstractOrderModel abstractOrderModel) {
        validateParameterNotNullStandardMessage("hostedTokenizationId", hostedTokenizationId);

        try {
            CalculateSurchargeRequest request = new CalculateSurchargeRequest();
            request.setAmountOfMoney(getAmoutOfMoney(abstractOrderModel));
            CardSource cardSource = new CardSource();
            if (!StringUtils.EMPTY.equals(token)) {
                cardSource.setToken(token);
            } else if (!StringUtils.EMPTY.equals(hostedTokenizationId)) {
                cardSource.setHostedTokenizationId(hostedTokenizationId);
            }

            request.withCardSource(cardSource);
            MerchantClient merchant = worldlineClientFactory.getMerchantClient(getStoreId(), getMerchantId());

            CalculateSurchargeResponse calculateSurchargeResponse = merchant.services().surchargeCalculation(request);
            WorldlineLogUtils.logAction(LOGGER, "calculateSurchargeResponse", request, calculateSurchargeResponse);
            return calculateSurchargeResponse;
        } catch (Exception e) {
            LOGGER.error("[ WORLDLINE ] Errors during surcharge calculation", e);
            return null;
        }

    }
    private AmountOfMoney getAmoutOfMoney(AbstractOrderModel abstractOrderModel) {
        final AmountOfMoney amountOfMoney = new AmountOfMoney();
        final String currencyCode = abstractOrderModel.getCurrency().getIsocode();
        final long amount;
        Double priceToSend = abstractOrderModel.getTotalPrice();
        if (abstractOrderModel.getPaymentCost() != 0.0d) {
            priceToSend -= abstractOrderModel.getPaymentCost();
        }
        amount = worldlineAmountUtils.createAmount(priceToSend, abstractOrderModel.getCurrency().getIsocode());
        amountOfMoney.setAmount(amount);
        amountOfMoney.setCurrencyCode(currencyCode);

        return amountOfMoney;
    }

    @Override
    public PaymentResponse getPayment(String paymentId) {
        validateParameterNotNull(paymentId, "paymentId cannot be null");
        try {
            MerchantClient merchant = worldlineClientFactory.getMerchantClient(getStoreId(), getMerchantId());

            final PaymentResponse payment = merchant.payments().getPayment(paymentId);

            WorldlineLogUtils.logAction(LOGGER, "getPayment", paymentId, payment);

            return payment;
        } catch (Exception e) {
            LOGGER.error("[ WORLDLINE ] Errors during getting getPayment", e);
            //TODO Throw Logical Exception
            return null;
        }
    }

    @Override
    public CreatePaymentResponse createPayment(AbstractOrderModel abstractOrderModel) throws WorldlineNonAuthorizedPaymentException {
        validateParameterNotNull(abstractOrderModel, "order cannot be null");
        try {
            MerchantClient merchant = worldlineClientFactory.getMerchantClient(getStoreId(), getMerchantId());

            final CreatePaymentRequest params = worldlineHostedTokenizationParamConverter.convert(abstractOrderModel);
            final CreatePaymentResponse payment = merchant.payments().createPayment(params);

            WorldlineLogUtils.logAction(LOGGER, "createPayment", params, payment);

            return payment;
        } catch (DeclinedPaymentException e) {
            LOGGER.debug("[ WORLDLINE ] Errors during getting createPayment ", e.getMessage());
            throw new WorldlineNonAuthorizedPaymentException(WorldlinedirectcoreConstants.UNAUTHORIZED_REASON.REJECTED);
        } catch (Exception e) {
            LOGGER.error("[ WORLDLINE ] Errors during getting createPayment ", e);
            //TODO Throw Logical Exception
        }
        return null;
    }

    @Override
    public PaymentResponse getPayment(String storeId, String paymentId) {
        validateParameterNotNull(paymentId, "paymentId cannot be null");
        try {

            MerchantClient merchant = worldlineClientFactory.getMerchantClient(storeId, getMerchantId(storeId));

            final PaymentResponse payment = merchant.payments().getPayment(paymentId);

            WorldlineLogUtils.logAction(LOGGER, "getPayment", paymentId, payment);

            return payment;
        } catch (Exception e) {
            LOGGER.error("[ WORLDLINE ] Errors during getting getPayment", e);
            //TODO Throw Logical Exception
            return null;
        }
    }

    @Override
    public CapturesResponse getCaptures(String storeId, String paymentId) {
        validateParameterNotNull(paymentId, "paymentId cannot be null");
        try {
            MerchantClient merchant = worldlineClientFactory.getMerchantClient(storeId, getMerchantId(storeId));
            final CapturesResponse captures = merchant.payments().getCaptures(paymentId);

            WorldlineLogUtils.logAction(LOGGER, "getCaptures", paymentId, captures);

            return captures;
        } catch (Exception e) {
            LOGGER.error("[ WORLDLINE ] Errors during getting getPayment", e);
            //TODO Throw Logical Exception
            return null;
        }
    }

    @Override
    public CaptureResponse capturePayment(String storeId, String paymentId, BigDecimal amountToCapture, String currencyISOcode, Boolean isFinal) {

        try {
            MerchantClient merchant = worldlineClientFactory.getMerchantClient(storeId, getMerchantId(storeId));
            CapturePaymentRequest capturePaymentRequest = new CapturePaymentRequest();
            capturePaymentRequest.setAmount(worldlineAmountUtils.createAmount(amountToCapture, currencyISOcode));
            capturePaymentRequest.setIsFinal(isFinal);

            CaptureResponse captureResponse =
                    merchant.payments().capturePayment(paymentId, capturePaymentRequest);

            WorldlineLogUtils.logAction(LOGGER, "capturePayment", paymentId, captureResponse);

            return captureResponse;
        } catch (Exception e) {
            LOGGER.error("[ WORLDLINE ] Errors during getting capturePayment", e);
            //TODO Throw Logical Exception
            return null;
        }
    }

    @Override
    public Long getNonCapturedAmount(String storeId, String paymentId, BigDecimal plannedAmount, String currencyISOcode) {
        //Find if there was amount that was captured before performing the capture action
        CapturesResponse capturesResponse = getCaptures(storeId, paymentId);
        return getNonCapturedAmount(storeId, paymentId, capturesResponse, plannedAmount, currencyISOcode);
    }

    @Override
    public Long getNonCapturedAmount(String storeId, String paymentId, CapturesResponse capturesResponse, BigDecimal plannedAmount, String currencyISOcode) {
        final long fullAmount = worldlineAmountUtils.createAmount(plannedAmount, currencyISOcode);
        Long amountPaid = 0L;

        final PaymentResponse paymentResponse = getPayment(storeId, paymentId);
        if (WorldlinedirectcoreConstants.PAYMENT_STATUS_ENUM.valueOf(paymentResponse.getStatus()).equals(WorldlinedirectcoreConstants.PAYMENT_STATUS_ENUM.CAPTURED)) {
            amountPaid = paymentResponse.getPaymentOutput().getAcquiredAmount().getAmount();
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
    public CancelPaymentResponse cancelPayment(String storeId, String paymentId) {
        try {
            MerchantClient merchant = worldlineClientFactory.getMerchantClient(storeId, getMerchantId(storeId));
            CancelPaymentResponse cancelPaymentResponse =
                    merchant.payments().cancelPayment(paymentId);

            WorldlineLogUtils.logAction(LOGGER, "cancelPayment", paymentId, cancelPaymentResponse);

            return cancelPaymentResponse;
        } catch (Exception e) {
            LOGGER.error("[ WORLDLINE ] Errors during getting cancelPayment", e);
            //TODO Throw Logical Exception
            return null;
        }
    }

    @Override
    public RefundResponse refundPayment(String storeId, String paymentId, BigDecimal returnAmount, String currencyISOCode) {
        try {
            MerchantClient merchant = worldlineClientFactory.getMerchantClient(storeId, getMerchantId(storeId));
            RefundRequest refundRequest = new RefundRequest();
            AmountOfMoney amountOfMoney = new AmountOfMoney();
            amountOfMoney.setCurrencyCode(currencyISOCode);
            amountOfMoney.setAmount(worldlineAmountUtils.createAmount(returnAmount, currencyISOCode));
            refundRequest.setAmountOfMoney(amountOfMoney);
            RefundResponse refundResponse =
                    merchant.payments().refundPayment(paymentId, refundRequest);
            WorldlineLogUtils.logAction(LOGGER, "refundPayment", paymentId, refundResponse);

            return refundResponse;
        } catch (Exception e) {
            LOGGER.error("[ WORLDLINE ] Errors during getting refundPayment", e);
            //TODO Throw Logical Exception
            return null;
        }
    }

    protected String getMerchantId() {
        return worldlineConfigurationService.getCurrentMerchantId();
    }

    protected String getMerchantId(String storeId) {
        return baseStoreService.getBaseStoreForUid(storeId).getWorldlineConfiguration().getMerchantID();
    }

    protected String getStoreId() {
        return baseStoreService.getCurrentBaseStore().getUid();
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

    public void setWorldlineBrowserCustomerDeviceConverter(Converter<com.worldline.direct.order.data.BrowserData, CustomerDevice> worldlineBrowserCustomerDeviceConverter) {
        this.worldlineBrowserCustomerDeviceConverter = worldlineBrowserCustomerDeviceConverter;
    }

    public void setBaseStoreService(BaseStoreService baseStoreService) {
        this.baseStoreService = baseStoreService;
    }
}
