package com.ingenico.ogone.direct.actions.order;

import java.util.HashSet;
import java.util.Set;

import de.hybris.platform.core.enums.OrderStatus;
import de.hybris.platform.core.enums.PaymentStatus;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.payment.IngenicoPaymentInfoModel;
import de.hybris.platform.orderprocessing.model.OrderProcessModel;
import de.hybris.platform.processengine.action.AbstractAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class IngenicoCheckCapturedPaymentAction extends AbstractAction<OrderProcessModel> {
    private static final Logger LOG = LoggerFactory.getLogger(IngenicoCheckCapturedPaymentAction.class);

    @Override
    public String execute(final OrderProcessModel process) {
        LOG.debug("[INGENICO] Process: " + process.getCode() + " in step " + getClass().getSimpleName());

        final OrderModel order = process.getOrder();


        if (!(order.getPaymentInfo() instanceof IngenicoPaymentInfoModel)) {
            LOG.debug("[INGENICO] Skip capture check!");
            return Transition.OK.toString();
        }


        if (PaymentStatus.INGENICO_AUTHORIZED.equals(order.getPaymentStatus())
                || PaymentStatus.INGENICO_WAITING_CAPTURE.equals(order.getPaymentStatus())) {
            LOG.debug("[INGENICO] Process: {} Order Waiting", process.getCode());
            return Transition.WAIT.toString();
        } else if (PaymentStatus.INGENICO_REJECTED.equals(order.getPaymentStatus())) {
            LOG.debug("[INGENICO] Process: " + process.getCode() + " Order Not Captured");
            return Transition.NOK.toString();
        } else if (PaymentStatus.INGENICO_CAPTURED.equals(order.getPaymentStatus())) {
            LOG.debug("[INGENICO] Process: {} Order Captured", process.getCode());
            order.setStatus(OrderStatus.PAYMENT_CAPTURED);
            modelService.save(order);
            return Transition.OK.toString();
        }
        LOG.debug("[INGENICO] Process: {} Order with unknown status [{}]", process.getCode(), order.getPaymentStatus());
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