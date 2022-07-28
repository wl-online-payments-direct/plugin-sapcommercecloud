package com.worldline.direct.service;

import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.OrderModel;

public interface WorldlineBusinessProcessService {

    void triggerOrderProcessEvent(AbstractOrderModel orderModel, String event);

    void triggerReturnProcessEvent(OrderModel orderModel, String event);
}
