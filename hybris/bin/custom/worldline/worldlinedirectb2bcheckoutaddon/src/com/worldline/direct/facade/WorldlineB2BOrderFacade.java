package com.worldline.direct.facade;

import com.worldline.direct.service.WorldlineRecurringService;
import de.hybris.platform.b2bacceleratorfacades.order.impl.DefaultB2BOrderFacade;
import de.hybris.platform.b2bacceleratorservices.customer.B2BCustomerAccountService;
import de.hybris.platform.orderscheduling.model.CartToOrderCronJobModel;
import org.springframework.beans.factory.annotation.Required;

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

    @Required
    public void setWorldlineRecurringService(WorldlineRecurringService worldlineRecurringService) {
        this.worldlineRecurringService = worldlineRecurringService;
    }
}
