package com.worldline.direct.dao;

import java.util.List;

import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.core.model.user.CustomerModel;

public interface WorldlineCustomerAccountDao {

    List<WorldlinePaymentInfoModel> findIgenicoPaymentInfosByCustomer(CustomerModel customerModel, boolean saved);

    WorldlinePaymentInfoModel findIgenicoPaymentInfosByCustomerAndToken(CustomerModel customerModel, String token, boolean saved);
}
