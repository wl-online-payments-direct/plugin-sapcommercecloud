package com.ingenico.ogone.direct.dao;

import java.util.List;

import de.hybris.platform.core.model.order.payment.IngenicoPaymentInfoModel;
import de.hybris.platform.core.model.user.CustomerModel;

public interface IngenicoCustomerAccountDao {

    List<IngenicoPaymentInfoModel> findIgenicoPaymentInfosByCustomer(CustomerModel customerModel, boolean saved);
}
