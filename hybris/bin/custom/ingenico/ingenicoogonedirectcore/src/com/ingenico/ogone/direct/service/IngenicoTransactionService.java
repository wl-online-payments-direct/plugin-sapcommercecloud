package com.ingenico.ogone.direct.service;

import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.payment.enums.PaymentTransactionType;
import de.hybris.platform.payment.model.PaymentTransactionModel;

import com.ingenico.direct.domain.AmountOfMoney;
import com.ingenico.direct.domain.Capture;
import com.ingenico.direct.domain.WebhooksEvent;

public interface IngenicoTransactionService {

    PaymentTransactionModel getOrCreatePaymentTransaction(AbstractOrderModel abstractOrderModel,
                                                          String merchantReference,
                                                          String paymentTransactionId);

    PaymentTransactionModel createAuthorizationPaymentTransaction(AbstractOrderModel abstractOrderModel,
                                                                  String merchantReference,
                                                                  String paymentTransactionId,
                                                                  String status,
                                                                  String statusDetails,
                                                                  AmountOfMoney amountOfMoney);

    PaymentTransactionModel updatePaymentTransaction(PaymentTransactionModel paymentTransactionModel,
                                                     String paymentTransactionId,
                                                     String status,
                                                     String statusDetails,
                                                     AmountOfMoney amountOfMoney,
                                                     PaymentTransactionType paymentTransactionType);

    void processCapturedEvent(WebhooksEvent webhooksEvent);

    void processCapture(Capture capture);

    void processRefundedEvent(WebhooksEvent webhooksEvent);

}
