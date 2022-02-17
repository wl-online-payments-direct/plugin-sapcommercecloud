package com.worldline.direct.dao;

import de.hybris.platform.payment.model.PaymentTransactionModel;

public interface WorldlineTransactionDao {

    PaymentTransactionModel findPaymentTransaction(String reference);
}
