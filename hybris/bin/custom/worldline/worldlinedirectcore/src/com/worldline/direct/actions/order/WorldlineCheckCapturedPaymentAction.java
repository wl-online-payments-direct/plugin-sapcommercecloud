package com.worldline.direct.actions.order;

import java.util.HashSet;
import java.util.Set;

import de.hybris.platform.core.enums.OrderStatus;
import de.hybris.platform.core.enums.PaymentStatus;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.orderprocessing.model.OrderProcessModel;
import de.hybris.platform.processengine.action.AbstractAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class WorldlineCheckCapturedPaymentAction extends AbstractAction<OrderProcessModel> {
    private static final Logger LOG = LoggerFactory.getLogger(WorldlineCheckCapturedPaymentAction.class);

    @Override
    public String execute(final OrderProcessModel process) {
        LOG.debug("[WORLDLINE] Process: " + process.getCode() + " in step " + getClass().getSimpleName());

        final OrderModel order = process.getOrder();


        if (!(order.getPaymentInfo() instanceof WorldlinePaymentInfoModel)) {
            LOG.debug("[WORLDLINE] Skip capture check!");
            return Transition.OK.toString();
        }


        if (PaymentStatus.WORLDLINE_AUTHORIZED.equals(order.getPaymentStatus())
                || PaymentStatus.WORLDLINE_WAITING_CAPTURE.equals(order.getPaymentStatus())) {
            LOG.debug("[WORLDLINE] Process: {} Order Waiting", process.getCode());
            return Transition.WAIT.toString();
        } else if (PaymentStatus.WORLDLINE_REJECTED.equals(order.getPaymentStatus())) {
            LOG.debug("[WORLDLINE] Process: " + process.getCode() + " Order Not Captured");
            return Transition.NOK.toString();
        } else if (PaymentStatus.WORLDLINE_CAPTURED.equals(order.getPaymentStatus())) {
            LOG.debug("[WORLDLINE] Process: {} Order Captured", process.getCode());
            order.setStatus(OrderStatus.PAYMENT_CAPTURED);
            modelService.save(order);
            return Transition.OK.toString();
        }
        LOG.debug("[WORLDLINE] Process: {} Order with unknown status [{}]", process.getCode(), order.getPaymentStatus());
        return Transition.WAIT.toString();
    }

    enum Transition {
        OK, NOK, WAIT;

        public static Set<String> getStringValues() {
            Set<String> res = new HashSet<>();
            for (final Transition transitions : Transition.values()) {
                res.add(transitions.toString());
            }
            return res;
        }
    }

    @Override
    public Set<String> getTransitions() {
        return Transition.getStringValues();
    }
}