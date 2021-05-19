package com.ingenico.ogone.direct.service;

import de.hybris.platform.core.model.order.OrderModel;

public interface IngenicoBusinessProcessService {

    void triggerOrderProcessEvent(OrderModel orderModel, String event);

    void triggerReturnProcessEvent(OrderModel orderModel, String event);
}
