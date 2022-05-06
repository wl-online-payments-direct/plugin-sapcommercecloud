package com.worldline.direct.service.impl;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

import java.util.List;

import com.worldline.direct.dao.WorldlineCustomerAccountDao;
import com.worldline.direct.service.WorldlineCustomerAccountService;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.servicelayer.exceptions.ModelNotFoundException;

public class WorldlineCustomerAccountServiceImpl implements WorldlineCustomerAccountService {

    private WorldlineCustomerAccountDao worldlineCustomerAccountDao;

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

    public void setWorldlineCustomerAccountDao(WorldlineCustomerAccountDao worldlineCustomerAccountDao) {
        this.worldlineCustomerAccountDao = worldlineCustomerAccountDao;
    }
}
