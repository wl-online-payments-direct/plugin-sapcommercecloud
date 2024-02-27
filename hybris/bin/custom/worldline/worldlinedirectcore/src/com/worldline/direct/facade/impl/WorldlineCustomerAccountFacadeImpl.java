package com.worldline.direct.facade.impl;

import com.worldline.direct.facade.WorldlineCustomerAccountFacade;
import com.worldline.direct.service.WorldlineCustomerAccountService;
import de.hybris.platform.b2bacceleratorfacades.order.data.ScheduledCartData;
import de.hybris.platform.orderscheduling.model.CartToOrderCronJobModel;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import org.springframework.beans.factory.annotation.Required;

public class WorldlineCustomerAccountFacadeImpl implements WorldlineCustomerAccountFacade {
    private WorldlineCustomerAccountService worldlineCustomerAccountService;
    private Converter<CartToOrderCronJobModel, ScheduledCartData> scheduledCartDataConverter;

    @Override
    public ScheduledCartData getCartToOrderCronJob(String jobCode) {
        return scheduledCartDataConverter.convert(worldlineCustomerAccountService.getCartToOrderCronJob(jobCode));
    }

    public void setWorldlineCustomerAccountService(WorldlineCustomerAccountService worldlineCustomerAccountService) {
        this.worldlineCustomerAccountService = worldlineCustomerAccountService;
    }

    @Required
    public void setScheduledCartDataConverter(Converter<CartToOrderCronJobModel, ScheduledCartData> scheduledCartDataConverter) {
        this.scheduledCartDataConverter = scheduledCartDataConverter;
    }
}
