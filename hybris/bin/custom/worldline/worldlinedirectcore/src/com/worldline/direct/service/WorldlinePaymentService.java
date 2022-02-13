package com.worldline.direct.service;

import java.math.BigDecimal;
import java.util.List;

import de.hybris.platform.core.model.order.OrderModel;

import com.ingenico.direct.domain.CancelPaymentResponse;
import com.ingenico.direct.domain.CaptureResponse;
import com.ingenico.direct.domain.CapturesResponse;
import com.ingenico.direct.domain.CreateHostedCheckoutResponse;
import com.ingenico.direct.domain.CreateHostedTokenizationResponse;
import com.ingenico.direct.domain.CreatePaymentResponse;
import com.ingenico.direct.domain.DirectoryEntry;
import com.ingenico.direct.domain.GetHostedCheckoutResponse;
import com.ingenico.direct.domain.GetHostedTokenizationResponse;
import com.ingenico.direct.domain.GetPaymentProductsResponse;
import com.ingenico.direct.domain.PaymentProduct;
import com.ingenico.direct.domain.PaymentResponse;
import com.ingenico.direct.domain.ProductDirectory;
import com.ingenico.direct.domain.RefundResponse;
import com.worldline.direct.exception.WorldlineNonAuthorizedPaymentException;
import com.worldline.direct.model.WorldlineConfigurationModel;
import com.ingenico.direct.domain.TokenResponse;
import com.worldline.direct.order.data.BrowserData;
import com.worldline.direct.order.data.WorldlineHostedTokenizationData;

public interface WorldlinePaymentService {

    GetPaymentProductsResponse getPaymentProductsResponse(BigDecimal amount, String currency, String countryCode, String shopperLocale);

    List<PaymentProduct> getPaymentProducts(BigDecimal amount, String currency, String countryCode, String shopperLocale);

    PaymentProduct getPaymentProduct(Integer id, BigDecimal amount, String currency, String countryCode, String shopperLocale);

    ProductDirectory getProductDirectory(Integer id, String currency, String countryCode);

    List<DirectoryEntry> getProductDirectoryEntries(Integer id, String currency, String countryCode);

    CreateHostedTokenizationResponse createHostedTokenization(String shopperLocale, List<String> savedTokens);

    GetHostedTokenizationResponse getHostedTokenization(String hostedTokenizationId);

    CreatePaymentResponse createPaymentForHostedTokenization(OrderModel orderForCode, WorldlineHostedTokenizationData worldlineHostedTokenizationData, GetHostedTokenizationResponse tokenizationResponse) throws WorldlineNonAuthorizedPaymentException;

    PaymentResponse getPayment(String paymentId);

    CreateHostedCheckoutResponse createHostedCheckout(OrderModel orderForCode, BrowserData browserData);

    GetHostedCheckoutResponse getHostedCheckout(String hostedCheckoutId);

    CaptureResponse capturePayment(WorldlineConfigurationModel worldlineConfigurationModel, String paymentId, BigDecimal plannedAmount, String currencyISOcode, Boolean isFinal);

    PaymentResponse getPayment(WorldlineConfigurationModel worldlineConfigurationModel, String paymentId);

    CapturesResponse getCaptures(WorldlineConfigurationModel worldlineConfigurationModel, String paymentId);

    Long getNonCapturedAmount(WorldlineConfigurationModel worldlineConfigurationModel, String paymentId, BigDecimal plannedAmount, String currencyISOcode);

    Long getNonCapturedAmount(WorldlineConfigurationModel worldlineConfigurationModel, String paymentId, CapturesResponse capturesResponse, BigDecimal plannedAmount, String currencyISOcode);

    CancelPaymentResponse cancelPayment(WorldlineConfigurationModel worldlineConfigurationModel, String paymentId);

    RefundResponse refundPayment(WorldlineConfigurationModel worldlineConfigurationModel, String paymentId, BigDecimal returnAmount, String currencyISOCode);

    TokenResponse getToken(String tokenId);

    void deleteToken(String tokenId);

}
