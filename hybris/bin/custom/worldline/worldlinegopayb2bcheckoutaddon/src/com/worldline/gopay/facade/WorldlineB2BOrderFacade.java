package com.worldline.gopay.facade;

import com.worldline.gopay.service.WorldlineRecurringService;
import de.hybris.platform.b2bacceleratorfacades.order.impl.DefaultB2BOrderFacade;
import de.hybris.platform.b2bacceleratorservices.customer.B2BCustomerAccountService;
import de.hybris.platform.orderscheduling.model.CartToOrderCronJobModel;

public class WorldlineB2BOrderFacade extends DefaultB2BOrderFacade {
    private WorldlineRecurringService worldlineRecurringService;

    @Override
    public void cancelReplenishment(String jobCode, String user) {
        final CartToOrderCronJobModel cronJob = this.<B2BCustomerAccountService>getCustomerAccountService()
                .getCartToOrderCronJobForCode(jobCode, getUserService().getUserForUID(user));
        if (cronJob != null) {
            worldlineRecurringService.cancelRecurringPayment(cronJob);
            cronJob.setActive(Boolean.FALSE);
            getModelService().save(cronJob);
        }
    }

    public void setWorldlineRecurringService(WorldlineRecurringService worldlineRecurringService) {
        this.worldlineRecurringService = worldlineRecurringService;
    }
}
