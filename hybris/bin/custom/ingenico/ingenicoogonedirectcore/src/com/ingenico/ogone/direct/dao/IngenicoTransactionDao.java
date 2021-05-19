package com.ingenico.ogone.direct.dao;

import de.hybris.platform.payment.model.PaymentTransactionModel;

public interface IngenicoTransactionDao {

    PaymentTransactionModel findPaymentTransaction(String reference);
}
