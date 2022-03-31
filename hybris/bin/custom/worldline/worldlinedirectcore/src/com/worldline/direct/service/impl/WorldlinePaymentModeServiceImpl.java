package com.worldline.direct.service.impl;

import com.worldline.direct.dao.WorldlinePaymentModeDao;
import com.worldline.direct.service.WorldlinePaymentModeService;
import de.hybris.platform.core.model.order.payment.PaymentModeModel;
import de.hybris.platform.order.impl.DefaultPaymentModeService;

import java.util.List;

public class WorldlinePaymentModeServiceImpl extends DefaultPaymentModeService implements WorldlinePaymentModeService {
    private WorldlinePaymentModeDao worldlinePaymentModeDao;
    @Override
    public List<PaymentModeModel> getActivePaymentModes() {
        return worldlinePaymentModeDao.getActivePaymentModes();
    }

    public void setWorldlinePaymentModeDao(WorldlinePaymentModeDao worldlinePaymentModeDao) {
        this.worldlinePaymentModeDao = worldlinePaymentModeDao;
    }
}
