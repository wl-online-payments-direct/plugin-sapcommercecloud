package com.worldline.direct.service.impl;

import com.worldline.direct.dao.WorldlineCartToOrderCronJobModelDao;
import com.worldline.direct.dao.WorldlineCustomerAccountDao;
import com.worldline.direct.service.WorldlineCustomerAccountService;
import de.hybris.platform.commerceservices.search.pagedata.PageableData;
import de.hybris.platform.commerceservices.search.pagedata.SearchPageData;
import de.hybris.platform.commerceservices.strategies.CheckoutCustomerStrategy;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.orderscheduling.model.CartToOrderCronJobModel;
import de.hybris.platform.servicelayer.exceptions.ModelNotFoundException;

import java.util.List;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

public class WorldlineCustomerAccountServiceImpl implements WorldlineCustomerAccountService {

    private WorldlineCustomerAccountDao worldlineCustomerAccountDao;
    private CheckoutCustomerStrategy checkoutCustomerStrategy;
    private WorldlineCartToOrderCronJobModelDao worldlineCartToOrderCronJobModelDao;

    @Override
    public List<WorldlinePaymentInfoModel> getWorldlinePaymentInfos(CustomerModel customerModel, boolean saved) {
        validateParameterNotNull(customerModel, "Customer model cannot be null");
        return worldlineCustomerAccountDao.findWorldlinePaymentInfosByCustomer(customerModel, saved);
    }

    @Override
    public WorldlinePaymentInfoModel getWorldlinePaymentInfoByToken(CustomerModel customerModel, String token, boolean saved) {
        validateParameterNotNull(customerModel, "Customer model cannot be null");
        validateParameterNotNull(token, "token cannot be null");
        try {
            return worldlineCustomerAccountDao.findWorldlinePaymentInfosByCustomerAndToken(customerModel, token, saved);
        } catch (ModelNotFoundException ex) {
            return null;
        }
    }

    @Override
    public WorldlinePaymentInfoModel getWorldlinePaymentInfoByCode(CustomerModel customerModel, String code) {
        validateParameterNotNull(customerModel, "Customer model cannot be null");
        validateParameterNotNull(code, "code cannot be null");
        try {
            return worldlineCustomerAccountDao.findWorldlinePaymentInfosByCustomerAndCode(customerModel, code);
        } catch (ModelNotFoundException ex) {
            return null;
        }
    }

    @Override
    public CartToOrderCronJobModel getCartToOrderCronJob(String jobCode) {
        final CustomerModel currentCustomer = checkoutCustomerStrategy.getCurrentUserForCheckout();
        CartToOrderCronJobModel cartToOrderCronJob = worldlineCartToOrderCronJobModelDao.findCartToOrderCronJobByCode(jobCode, currentCustomer);
        return cartToOrderCronJob;
    }

    @Override
    public SearchPageData<CartToOrderCronJobModel> getPagedCartToOrderCronJobsForUser(CustomerModel currentCustomer, PageableData pageableData) {
        return worldlineCartToOrderCronJobModelDao.findPagedCartToOrderCronJobsByUser(currentCustomer, pageableData);
    }

    @Override
    public SearchPageData<OrderModel> getOrdersForJob(String jobCode, PageableData pageableData) {
        return worldlineCartToOrderCronJobModelDao.findOrderByJob(jobCode, pageableData);
    }

    public void setWorldlineCustomerAccountDao(WorldlineCustomerAccountDao worldlineCustomerAccountDao) {
        this.worldlineCustomerAccountDao = worldlineCustomerAccountDao;
    }

    public void setCheckoutCustomerStrategy(CheckoutCustomerStrategy checkoutCustomerStrategy) {
        this.checkoutCustomerStrategy = checkoutCustomerStrategy;
    }

    public void setWorldlineCartToOrderCronJobModelDao(WorldlineCartToOrderCronJobModelDao worldlineCartToOrderCronJobModelDao) {
        this.worldlineCartToOrderCronJobModelDao = worldlineCartToOrderCronJobModelDao;
    }
}
