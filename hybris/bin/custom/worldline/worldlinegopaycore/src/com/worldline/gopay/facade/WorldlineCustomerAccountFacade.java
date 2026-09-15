package com.worldline.gopay.facade;

import de.hybris.platform.b2bacceleratorfacades.order.data.ScheduledCartData;

public interface WorldlineCustomerAccountFacade {
    ScheduledCartData getCartToOrderCronJob(String jobCode);
}
