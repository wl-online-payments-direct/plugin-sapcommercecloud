package com.worldline.gopay.dao.impl;

import com.worldline.gopay.dao.WorldlinePaymentModeDao;
import de.hybris.platform.core.model.order.payment.PaymentModeModel;
import de.hybris.platform.order.daos.impl.DefaultPaymentModeDao;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class WorldlinePaymentModeDaoImpl extends DefaultPaymentModeDao implements WorldlinePaymentModeDao {
    @Override
    public List<PaymentModeModel> getActivePaymentModes() {
        Map<String, Boolean> params = Collections.singletonMap(PaymentModeModel.ACTIVE, Boolean.TRUE);
        return find(params);
    }
}
