package com.worldline.direct.dao;

import de.hybris.platform.core.model.order.payment.PaymentModeModel;
import de.hybris.platform.order.daos.PaymentModeDao;

import java.util.List;

public interface WorldlinePaymentModeDao extends PaymentModeDao {
    List<PaymentModeModel> getActivePaymentModes();
}
