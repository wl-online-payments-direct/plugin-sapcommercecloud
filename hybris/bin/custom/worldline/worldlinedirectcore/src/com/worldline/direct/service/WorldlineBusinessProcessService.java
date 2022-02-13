package com.worldline.direct.service;

import de.hybris.platform.core.model.order.OrderModel;

public interface WorldlineBusinessProcessService {

    void triggerOrderProcessEvent(OrderModel orderModel, String event);

    void triggerReturnProcessEvent(OrderModel orderModel, String event);
}
