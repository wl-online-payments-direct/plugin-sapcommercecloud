package com.worldline.direct.service.impl;

import com.worldline.direct.dao.WorldlineCustomerAccountDao;
import com.worldline.direct.service.WorldlineCustomerAccountService;
import de.hybris.platform.b2bacceleratorservices.customer.B2BCustomerAccountService;
import de.hybris.platform.commerceservices.strategies.CheckoutCustomerStrategy;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.orderscheduling.model.CartToOrderCronJobModel;
import de.hybris.platform.servicelayer.exceptions.ModelNotFoundException;
import org.springframework.beans.factory.annotation.Required;

import java.util.List;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

public class WorldlineCustomerAccountServiceImpl implements WorldlineCustomerAccountService {

    private WorldlineCustomerAccountDao worldlineCustomerAccountDao;
    private CheckoutCustomerStrategy checkoutCustomerStrategy;
    private B2BCustomerAccountService b2BCustomerAccountService;

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
    public WorldlinePaymentInfoModel getWorldlinePaymentInfoByRecurringToken(CustomerModel customerModel, String token, boolean recurring) {
        validateParameterNotNull(customerModel, "Customer model cannot be null");
        validateParameterNotNull(token, "token cannot be null");
        try {
            return worldlineCustomerAccountDao.findWorldlinePaymentInfosByCustomerAndRecurringToken(customerModel, token, recurring);
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
    public WorldlinePaymentInfoModel getWroldlinePaymentInfoByRecurringToken(CustomerModel customerModel, String token) {
        validateParameterNotNull(customerModel, "Customer model cannot be null");
        validateParameterNotNull(token, "code cannot be null");
        try {
            return worldlineCustomerAccountDao.findWorldlinePaymentInfosByCustomerAndRecurringToken(customerModel, token, Boolean.TRUE);
        } catch (ModelNotFoundException ex) {
            return null;
        }
    }

    @Override
    public CartToOrderCronJobModel getCartToOrderCronJob(String jobCode) {
        final CustomerModel currentCustomer = checkoutCustomerStrategy.getCurrentUserForCheckout();
        CartToOrderCronJobModel cartToOrderCronJob = b2BCustomerAccountService.getCartToOrderCronJobForCode(jobCode, currentCustomer);
        return cartToOrderCronJob;
    }
    @Required
    public void setWorldlineCustomerAccountDao(WorldlineCustomerAccountDao worldlineCustomerAccountDao) {
        this.worldlineCustomerAccountDao = worldlineCustomerAccountDao;
    }
    @Required
    public void setCheckoutCustomerStrategy(CheckoutCustomerStrategy checkoutCustomerStrategy) {
        this.checkoutCustomerStrategy = checkoutCustomerStrategy;
    }

    @Required
    public void setB2BCustomerAccountService(B2BCustomerAccountService b2BCustomerAccountService) {
        this.b2BCustomerAccountService = b2BCustomerAccountService;
    }
}
