package com.worldline.direct.actions.order;

import java.util.HashSet;
import java.util.Set;

import de.hybris.platform.core.enums.OrderStatus;
import de.hybris.platform.core.enums.PaymentStatus;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.orderprocessing.model.OrderProcessModel;
import de.hybris.platform.processengine.action.AbstractAction;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class WorldlineCheckAuthorizedPaymentAction extends AbstractAction<OrderProcessModel> {
    private static final Logger LOG = LoggerFactory.getLogger(WorldlineCheckAuthorizedPaymentAction.class);

    @Override
    public String execute(final OrderProcessModel process) {
        LOG.debug("[WORLDLINE] Process: " + process.getCode() + " in step " + getClass().getSimpleName());

        final OrderModel order = process.getOrder();

        if (order == null) {
            LOG.error("[WORLDLINE] Order is null!");
            return Transition.NOK.toString();
        }

        if (!(order.getPaymentInfo() instanceof WorldlinePaymentInfoModel)) {
            LOG.debug("[WORLDLINE] Skip authorization check!");
            return Transition.OK.toString();
        }

        if (PaymentStatus.WORLDLINE_CANCELED.equals(order.getPaymentStatus())) {
            return Transition.NOK.toString();
        }

        if (CollectionUtils.isEmpty(order.getPaymentTransactions())) {
            LOG.debug("[WORLDLINE] Process: {} Order Waiting", process.getCode());
            return Transition.WAIT.toString();
        }

        if (PaymentStatus.WORLDLINE_AUTHORIZED.equals(order.getPaymentStatus())
                || PaymentStatus.WORLDLINE_WAITING_CAPTURE.equals(order.getPaymentStatus())
                || PaymentStatus.WORLDLINE_CAPTURED.equals(order.getPaymentStatus())) {
            LOG.debug("[WORLDLINE] Process: {} Order Authorized", process.getCode());
            order.setStatus(OrderStatus.PAYMENT_AUTHORIZED);
            modelService.save(order);
            return Transition.OK.toString();
        } else if (PaymentStatus.WORLDLINE_REJECTED.equals(order.getPaymentStatus())) {
            LOG.debug("[WORLDLINE] Process: {} Order Not Authorized", process.getCode());
            order.setStatus(OrderStatus.PAYMENT_NOT_AUTHORIZED);
            modelService.save(order);
            return Transition.NOK.toString();
        } else {
            LOG.debug("[WORLDLINE] Process: {} Order Waiting Auth", process.getCode());
            order.setPaymentStatus(PaymentStatus.WORLDLINE_WAITING_AUTH);
            modelService.save(order);
            return Transition.WAIT.toString();
        }
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