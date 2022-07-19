package com.worldline.direct.service;

import de.hybris.platform.commerceservices.search.pagedata.PageableData;
import de.hybris.platform.commerceservices.search.pagedata.SearchPageData;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.orderscheduling.model.CartToOrderCronJobModel;

import java.util.List;

public interface WorldlineCustomerAccountService {

    List<WorldlinePaymentInfoModel> getWorldlinePaymentInfos(CustomerModel customerModel, boolean saved);

    WorldlinePaymentInfoModel getWorldlinePaymentInfoByCode(CustomerModel customerModel, String code);

    WorldlinePaymentInfoModel getWorldlinePaymentInfoByToken(CustomerModel customerModel, String token, boolean saved);

    CartToOrderCronJobModel getCartToOrderCronJob(String jobCode);

    SearchPageData<CartToOrderCronJobModel> getPagedCartToOrderCronJobsForUser(CustomerModel currentCustomer, PageableData pageableData);

    SearchPageData<OrderModel> getOrdersForJob(String jobCode, PageableData pageableData);
}
