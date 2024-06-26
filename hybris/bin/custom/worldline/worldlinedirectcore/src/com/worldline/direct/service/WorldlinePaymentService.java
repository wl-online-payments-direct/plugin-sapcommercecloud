package com.worldline.direct.service;

import com.onlinepayments.domain.*;
import com.worldline.direct.exception.WorldlineNonAuthorizedPaymentException;
import com.worldline.direct.model.WorldlineConfigurationModel;
import com.worldline.direct.model.WorldlineMandateModel;
import com.worldline.direct.order.data.BrowserData;
import com.worldline.direct.order.data.WorldlineHostedTokenizationData;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.OrderModel;

import java.math.BigDecimal;
import java.util.List;

public interface WorldlinePaymentService {

    GetPaymentProductsResponse getPaymentProductsResponse(BigDecimal amount, String currency, String countryCode, String shopperLocale, Boolean isReplenishmentOrder);

    List<PaymentProduct> getPaymentProducts(BigDecimal amount, String currency, String countryCode, String shopperLocale, Boolean isReplenishmentOrder);

    PaymentProduct getPaymentProduct(Integer id, BigDecimal amount, String currency, String countryCode, String shopperLocale);

    ProductDirectory getProductDirectory(Integer id, String currency, String countryCode);

    List<DirectoryEntry> getProductDirectoryEntries(Integer id, String currency, String countryCode);

    CreateHostedTokenizationResponse createHostedTokenization(String shopperLocale, List<String> savedTokens, Boolean isAnonymousUser);

    GetHostedTokenizationResponse getHostedTokenization(String hostedTokenizationId);

    CreatePaymentResponse createPaymentForHostedTokenization(OrderModel orderForCode, WorldlineHostedTokenizationData worldlineHostedTokenizationData) throws WorldlineNonAuthorizedPaymentException;

    PaymentResponse getPayment(String paymentId);

    CreatePaymentResponse createPayment(AbstractOrderModel abstractOrderModel) throws WorldlineNonAuthorizedPaymentException;

    CreateHostedCheckoutResponse createHostedCheckout(OrderModel orderForCode, BrowserData browserData);

    CreateHostedCheckoutResponse createHostedCheckout(CartModel cartModel);

    GetHostedCheckoutResponse getHostedCheckout(String hostedCheckoutId);

    CaptureResponse capturePayment(String storeId, String paymentId, BigDecimal plannedAmount, String currencyISOcode, Boolean isFinal);

    PaymentResponse getPayment(String storeId, String paymentId);

    CapturesResponse getCaptures(String storeId, String paymentId);

    Long getNonCapturedAmount(String storeId, String paymentId, BigDecimal plannedAmount, String currencyISOcode);

    Long getNonCapturedAmount(String storeId, String paymentId, CapturesResponse capturesResponse, BigDecimal plannedAmount, String currencyISOcode);

    CancelPaymentResponse cancelPayment(String storeId, String paymentId);

    RefundResponse refundPayment(String storeId, String paymentId, BigDecimal returnAmount, String currencyISOCode);

    TokenResponse getToken(String tokenId);

    void deleteToken(String tokenId);

    void deleteToken(String tokenId, String storeId);

    GetMandateResponse getMandate(String uniqueMandateReference);

    GetMandateResponse getMandate(WorldlineMandateModel worldlineMandateModel);

    GetMandateResponse revokeMandate(WorldlineMandateModel worldlineMandateModel);

    GetMandateResponse blockMandate(WorldlineMandateModel worldlineMandateModel);

    GetMandateResponse unBlockMandate(WorldlineMandateModel worldlineMandateModel);

    CalculateSurchargeResponse calculateSurcharge(String hostedTokenizationId, String token, AbstractOrderModel abstractOrderModel);

}
