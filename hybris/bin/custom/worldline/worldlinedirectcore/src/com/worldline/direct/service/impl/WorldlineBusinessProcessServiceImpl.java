package com.worldline.direct.service.impl;

import com.worldline.direct.service.WorldlineBusinessProcessService;
import de.hybris.platform.b2bacceleratorservices.model.process.ReplenishmentProcessModel;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.orderprocessing.model.OrderProcessModel;
import de.hybris.platform.processengine.BusinessProcessService;
import de.hybris.platform.returns.model.ReturnProcessModel;
import de.hybris.platform.returns.model.ReturnRequestModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

public class WorldlineBusinessProcessServiceImpl implements WorldlineBusinessProcessService {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldlineBusinessProcessServiceImpl.class);


    private BusinessProcessService businessProcessService;

    @Override
    public void triggerOrderProcessEvent(AbstractOrderModel abstractOrderModel, String event) {
        if (abstractOrderModel instanceof OrderModel) {
            final Collection<OrderProcessModel> orderProcesses = ((OrderModel) abstractOrderModel).getOrderProcess();
            for (final OrderProcessModel orderProcess : orderProcesses) {
                LOGGER.debug("Order process code: " + orderProcess.getCode());

                final String eventName = orderProcess.getCode() + "_" + event;
                LOGGER.debug("Sending event:" + eventName);
                businessProcessService.triggerEvent(eventName);
            }
        } else if (abstractOrderModel instanceof CartModel && (((CartModel) abstractOrderModel).getReplenishmentOrderProcess() != null)) {
            ReplenishmentProcessModel replenishmentProcessModel = ((CartModel) abstractOrderModel).getReplenishmentOrderProcess();
            LOGGER.debug("Replenishment process code: " + replenishmentProcessModel.getCode());

            final String eventName = replenishmentProcessModel.getCode() + "_" + event;
            LOGGER.debug("Sending event:" + eventName);
            businessProcessService.triggerEvent(eventName);
        }
    }

    @Override
    public void triggerReturnProcessEvent(OrderModel orderModel, String event) {
        List<ReturnRequestModel> returnRequests = orderModel.getReturnRequests();
        for (ReturnRequestModel returnRequest : returnRequests) {
            Collection<ReturnProcessModel> returnProcesses = returnRequest.getReturnProcess();
            for (ReturnProcessModel returnProcess : returnProcesses) {
                LOGGER.debug("Return process code: " + returnProcess.getCode());

                final String eventName = returnProcess.getCode() + "_" + event;
                LOGGER.debug("Sending event:" + eventName);
                businessProcessService.triggerEvent(eventName);
            }
        }
    }

    public void setBusinessProcessService(BusinessProcessService businessProcessService) {
        this.businessProcessService = businessProcessService;
    }
}
