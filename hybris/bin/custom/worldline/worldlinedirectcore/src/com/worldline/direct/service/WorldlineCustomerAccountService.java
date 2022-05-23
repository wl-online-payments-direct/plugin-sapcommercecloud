package com.worldline.direct.service;

import java.util.List;

import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.core.model.user.CustomerModel;

public interface WorldlineCustomerAccountService {

    List<WorldlinePaymentInfoModel> getWorldlinePaymentInfos(CustomerModel customerModel, boolean saved);

    WorldlinePaymentInfoModel getWorldlinePaymentInfoByCode(CustomerModel customerModel, String code);

    WorldlinePaymentInfoModel getWorldlinePaymentInfoByToken(CustomerModel customerModel, String token, boolean saved);
}
