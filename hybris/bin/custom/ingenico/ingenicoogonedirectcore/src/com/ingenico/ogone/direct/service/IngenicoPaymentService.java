package com.ingenico.ogone.direct.service;

import java.math.BigDecimal;
import java.util.List;

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
import com.ingenico.ogone.direct.model.IngenicoConfigurationModel;
import com.ingenico.direct.domain.TokenResponse;
import com.ingenico.ogone.direct.order.data.BrowserData;
import com.ingenico.ogone.direct.order.data.IngenicoHostedTokenizationData;

public interface IngenicoPaymentService {

    GetPaymentProductsResponse getPaymentProductsResponse(BigDecimal amount, String currency, String countryCode, String shopperLocale);

    List<PaymentProduct> getPaymentProducts(BigDecimal amount, String currency, String countryCode, String shopperLocale);

    PaymentProduct getPaymentProduct(Integer id, BigDecimal amount, String currency, String countryCode, String shopperLocale);

    ProductDirectory getProductDirectory(Integer id, String currency, String countryCode);

    List<DirectoryEntry> getProductDirectoryEntries(Integer id, String currency, String countryCode);

    CreateHostedTokenizationResponse createHostedTokenization(String shopperLocale, List<String> savedTokens);

    GetHostedTokenizationResponse getHostedTokenization(String hostedTokenizationId);

    CreatePaymentResponse createPaymentForHostedTokenization(IngenicoHostedTokenizationData ingenicoHostedTokenizationData, GetHostedTokenizationResponse tokenizationResponse);

    PaymentResponse getPayment(String paymentId);

    CreateHostedCheckoutResponse createHostedCheckout(BrowserData browserData);

    GetHostedCheckoutResponse getHostedCheckout(String hostedCheckoutId);

    CaptureResponse capturePayment(IngenicoConfigurationModel ingenicoConfigurationModel, String paymentId, BigDecimal plannedAmount, String currencyISOcode);

    CapturesResponse getCaptures(IngenicoConfigurationModel ingenicoConfigurationModel, String paymentId);

    Long getNonCapturedAmount(IngenicoConfigurationModel ingenicoConfigurationModel, String paymentId, BigDecimal plannedAmount, String currencyISOcode);

    Long getNonCapturedAmount(IngenicoConfigurationModel ingenicoConfigurationModel, CapturesResponse capturesResponse, BigDecimal plannedAmount, String currencyISOcode);

    CancelPaymentResponse cancelPayment(IngenicoConfigurationModel ingenicoConfigurationModel, String paymentId);

    RefundResponse refundPayment(IngenicoConfigurationModel ingenicoConfigurationModel, String paymentId, BigDecimal returnAmount, String currencyISOCode);

    TokenResponse getToken(String tokenId);

    void deleteToken(String tokenId);

}
