package com.ingenico.ogone.direct.service.impl;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

import java.util.List;

import de.hybris.platform.core.model.order.payment.IngenicoPaymentInfoModel;
import de.hybris.platform.core.model.user.CustomerModel;

import com.ingenico.ogone.direct.dao.IngenicoCustomerAccountDao;
import com.ingenico.ogone.direct.service.IngenicoCustomerAccountService;

public class IngenicoCustomerAccountServiceImpl implements IngenicoCustomerAccountService {

    private IngenicoCustomerAccountDao ingenicoCustomerAccountDao;

    @Override
    public List<IngenicoPaymentInfoModel> getIngenicoPaymentInfos(CustomerModel customerModel, boolean saved) {
        validateParameterNotNull(customerModel, "Customer model cannot be null");
        return ingenicoCustomerAccountDao.findIgenicoPaymentInfosByCustomer(customerModel, saved);
    }

    public void setIngenicoCustomerAccountDao(IngenicoCustomerAccountDao ingenicoCustomerAccountDao) {
        this.ingenicoCustomerAccountDao = ingenicoCustomerAccountDao;
    }
}