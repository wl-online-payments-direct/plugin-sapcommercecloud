package com.worldline.direct.dao;

import java.util.List;

import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.core.model.user.CustomerModel;

public interface WorldlineCustomerAccountDao {

    List<WorldlinePaymentInfoModel> findWorldlinePaymentInfosByCustomer(CustomerModel customerModel, boolean saved);

    WorldlinePaymentInfoModel findWorldlinePaymentInfosByCustomerAndCode(CustomerModel customerModel, String code);

    WorldlinePaymentInfoModel findWorldlinePaymentInfosByCustomerAndToken(CustomerModel customerModel, String token, boolean saved);

    WorldlinePaymentInfoModel findWorldlinePaymentInfosByCustomerAndRecurringToken(CustomerModel customerModel, String token, boolean recurring);

}
