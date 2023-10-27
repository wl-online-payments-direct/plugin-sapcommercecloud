package com.worldline.direct.facade;

import com.onlinepayments.domain.*;
import com.worldline.direct.enums.WorldlineCheckoutTypesEnum;
import com.worldline.direct.enums.WorldlineReplenishmentOccurrenceEnum;
import com.worldline.direct.exception.WorldlineNonAuthorizedPaymentException;
import com.worldline.direct.exception.WorldlineNonValidPaymentProductException;
import com.worldline.direct.exception.WorldlineNonValidReturnMACException;
import com.worldline.direct.order.data.BrowserData;
import com.worldline.direct.order.data.WorldlineHostedTokenizationData;
import com.worldline.direct.order.data.WorldlinePaymentInfoData;
import de.hybris.platform.b2bacceleratorfacades.checkout.data.PlaceOrderData;
import de.hybris.platform.commercefacades.order.data.AbstractOrderData;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.cronjob.enums.DayOfWeek;
import de.hybris.platform.order.InvalidCartException;

import java.util.Date;
import java.util.List;

public interface WorldlineCheckoutFacade {

    List<PaymentProduct> getAvailablePaymentMethods();

    Boolean checkForCardPaymentMethods(List<PaymentProduct> paymentProducts);

    WorldlineCheckoutTypesEnum getWorldlineCheckoutType();

    PaymentProduct getPaymentMethodById(int paymentId);

    CreateHostedTokenizationResponse createHostedTokenization();

    List<DirectoryEntry> getIdealIssuers(List<PaymentProduct> paymentProducts);

    void handlePaymentInfo(WorldlinePaymentInfoData paymentInfoData);

    void fillWorldlinePaymentInfoData(WorldlinePaymentInfoData paymentInfoData, String savedPaymentCode, Integer paymentId, String paymentDirId, String hostedTokenizationId) throws WorldlineNonValidPaymentProductException;

    void authorisePaymentForHostedTokenization(String orderCode, WorldlineHostedTokenizationData hostedTokenizationId) throws WorldlineNonAuthorizedPaymentException, InvalidCartException;

    void handle3dsResponse(String ref, String paymentId) throws WorldlineNonAuthorizedPaymentException, InvalidCartException;

    CreateHostedCheckoutResponse createHostedCheckout(String orderCode, BrowserData browserData) throws InvalidCartException;

    void authorisePaymentForHostedCheckout(String orderCode, String hostedCheckoutId, Boolean isRecurring) throws WorldlineNonAuthorizedPaymentException, InvalidCartException;

    void handlePaymentResponse(OrderModel abstractOrderModel, PaymentResponse paymentResponse) throws WorldlineNonAuthorizedPaymentException, InvalidCartException;

    void validateReturnMAC(AbstractOrderData orderDetails, String returnMAC) throws WorldlineNonValidReturnMACException;

    boolean isTemporaryToken(String hostedtokenizationID);

    void calculateSurcharge(AbstractOrderModel cartModel, String hostedTokenizationID, String token, String savedPaymentInfoId, String paymentMethodType);

    void saveReplenishmentData( boolean replenishmentOrder,
                                Date replenishmentStartDate,
                                Date replenishmentEndDate,
                                String nDays,
                                String nWeeks,
                                String nMonths,
                                String nthDayOfMonth,
                                List<String> nDaysOfWeek,
                                String replenishmentRecurrence);

    PlaceOrderData prepareOrderPlacementData();


}
