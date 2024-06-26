package com.worldline.direct.service;

import com.onlinepayments.domain.*;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.payment.enums.PaymentTransactionType;
import de.hybris.platform.payment.model.PaymentTransactionModel;

public interface WorldlineTransactionService {

    PaymentTransactionModel getOrCreatePaymentTransaction(AbstractOrderModel abstractOrderModel,
                                                          String merchantReference,
                                                          String paymentTransactionId);

    PaymentTransactionModel createAuthorizationPaymentTransaction(AbstractOrderModel abstractOrderModel,
                                                                  String merchantReference,
                                                                  String paymentTransactionId,
                                                                  String status,
                                                                  AmountOfMoney amountOfMoney);

    PaymentTransactionModel updatePaymentTransaction(PaymentTransactionModel paymentTransactionModel,
                                                     String paymentTransactionId,
                                                     String status,
                                                     AmountOfMoney amountOfMoney,
                                                     PaymentTransactionType paymentTransactionType);

    void processCapturedEvent(WebhooksEvent webhooksEvent);

    void processCapture(Capture capture);

    void processRefundedEvent(WebhooksEvent webhooksEvent);

    void saveSurchargeData(AbstractOrderModel orderModel, SurchargeSpecificOutput surchargeSpecificOutput);

    void savePaymentCost(AbstractOrderModel orderModel, Double surchargeAmount);

    void savePaymentCost(AbstractOrderModel orderModel, AmountOfMoney surchargeAmount);

}
