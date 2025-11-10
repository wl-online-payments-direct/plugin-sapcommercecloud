package com.worldline.direct.service.impl;

import com.worldline.direct.dao.WorldlinePaymentModeDao;
import com.worldline.direct.service.WorldlinePaymentModeService;
import de.hybris.platform.core.model.order.payment.PaymentModeModel;
import de.hybris.platform.order.PaymentModeService;
import de.hybris.platform.order.impl.DefaultPaymentModeService;

import java.util.List;

public class WorldlinePaymentModeServiceImpl extends DefaultPaymentModeService implements WorldlinePaymentModeService {
    private WorldlinePaymentModeDao worldlinePaymentModeDao;

    @Override
    public List<PaymentModeModel> getActivePaymentModes() {
        return worldlinePaymentModeDao.getActivePaymentModes();
    }

    @Override
    public boolean isIntersolve(String paymentModeId) {
        PaymentModeModel paymentMode = getPaymentModeForCode(paymentModeId);
        if(paymentMode != null) {
            return paymentMode.getIntersolve();
        }
        return false;
    }

    @Override
    public boolean isFloa(String paymentModeId) {
        PaymentModeModel paymentMode = getPaymentModeForCode(paymentModeId);
        if(paymentMode != null) {
            return paymentMode.getFloapay();
        }
        return false;
    }

    /**
     * Returns a boolean representing whether the payment mode is SALE only (e.g. cannot be preauthed).
     * TODO: If more Payment Modes require this, look into making a more generic way of handling this.
     * @param paymentModeId A String representing the code of a PaymentMode.
     * @return true if the Payment Mode MUST be used with the Authorization Mode SALE.
     */
    @Override
    public boolean isSaleOnly(String paymentModeId) {
        PaymentModeModel paymentMode = getPaymentModeForCode(paymentModeId);
        if(paymentMode != null) {
            return paymentMode.getFloapay() || paymentMode.getIntersolve();
        }
        return false;
    }

    public void setWorldlinePaymentModeDao(WorldlinePaymentModeDao worldlinePaymentModeDao) {
        this.worldlinePaymentModeDao = worldlinePaymentModeDao;
    }
}
