package com.ingenico.ogone.direct.service;

import java.util.List;

import de.hybris.platform.core.model.order.payment.IngenicoPaymentInfoModel;
import de.hybris.platform.core.model.user.CustomerModel;

public interface IngenicoCustomerAccountService {

    List<IngenicoPaymentInfoModel> getIngenicoPaymentInfos(CustomerModel customerModel, boolean saved);

    IngenicoPaymentInfoModel getIngenicoPaymentInfoByToken(CustomerModel customerModel, String token, boolean saved);
}
