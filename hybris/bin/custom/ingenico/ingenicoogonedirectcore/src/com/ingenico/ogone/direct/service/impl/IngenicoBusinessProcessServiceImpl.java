package com.ingenico.ogone.direct.service.impl;

import java.util.Collection;
import java.util.List;

import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.orderprocessing.model.OrderProcessModel;
import de.hybris.platform.processengine.BusinessProcessService;
import de.hybris.platform.returns.model.ReturnProcessModel;
import de.hybris.platform.returns.model.ReturnRequestModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingenico.ogone.direct.service.IngenicoBusinessProcessService;

public class IngenicoBusinessProcessServiceImpl implements IngenicoBusinessProcessService {
    private static final Logger LOGGER = LoggerFactory.getLogger(IngenicoBusinessProcessServiceImpl.class);


    private BusinessProcessService businessProcessService;

    @Override
    public void triggerOrderProcessEvent(OrderModel orderModel, String event) {
        final Collection<OrderProcessModel> orderProcesses = orderModel.getOrderProcess();
        for (final OrderProcessModel orderProcess : orderProcesses) {
            LOGGER.debug("Order process code: " + orderProcess.getCode());

            final String eventName = orderProcess.getCode() + "_" + event;
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
