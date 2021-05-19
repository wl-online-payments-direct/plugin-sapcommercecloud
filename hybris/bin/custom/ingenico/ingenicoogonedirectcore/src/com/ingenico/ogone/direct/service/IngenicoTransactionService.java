package com.ingenico.ogone.direct.service;

import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.payment.enums.PaymentTransactionType;
import de.hybris.platform.payment.model.PaymentTransactionModel;

import com.ingenico.direct.domain.PaymentResponse;
import com.ingenico.direct.domain.WebhooksEvent;

public interface IngenicoTransactionService {

    PaymentTransactionModel createOrUpdatePaymentTransaction(AbstractOrderModel abstractOrderModel,
                                                             PaymentResponse paymentResponse, PaymentTransactionType authorization);

    PaymentTransactionModel updatePaymentTransaction(PaymentTransactionModel paymentTransactionModel,
                                                     PaymentResponse paymentResponse);

    void processCapturedEvent(WebhooksEvent webhooksEvent);

    void processCancelledEvent(WebhooksEvent webhooksEvent);

    void processRefundedEvent(WebhooksEvent webhooksEvent);

}
