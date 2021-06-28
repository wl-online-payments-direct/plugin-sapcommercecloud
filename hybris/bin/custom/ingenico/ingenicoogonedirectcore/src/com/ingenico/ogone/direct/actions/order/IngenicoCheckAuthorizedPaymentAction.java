package com.ingenico.ogone.direct.actions.order;

import java.util.HashSet;
import java.util.Set;

import de.hybris.platform.core.enums.OrderStatus;
import de.hybris.platform.core.enums.PaymentStatus;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.payment.IngenicoPaymentInfoModel;
import de.hybris.platform.orderprocessing.model.OrderProcessModel;
import de.hybris.platform.processengine.action.AbstractAction;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class IngenicoCheckAuthorizedPaymentAction extends AbstractAction<OrderProcessModel> {
    private static final Logger LOG = LoggerFactory.getLogger(IngenicoCheckAuthorizedPaymentAction.class);

    @Override
    public String execute(final OrderProcessModel process) {
        LOG.debug("[INGENICO] Process: " + process.getCode() + " in step " + getClass().getSimpleName());

        final OrderModel order = process.getOrder();

        if (order == null) {
            LOG.error("[INGENICO] Order is null!");
            return Transition.NOK.toString();
        }

        if (!(order.getPaymentInfo() instanceof IngenicoPaymentInfoModel)) {
            LOG.debug("[INGENICO] Skip authorization check!");
            return Transition.OK.toString();
        }

        if (PaymentStatus.INGENICO_CANCELED.equals(order.getPaymentStatus())) {
            return Transition.NOK.toString();
        }

        if (CollectionUtils.isEmpty(order.getPaymentTransactions())) {
            LOG.debug("[INGENICO] Process: {} Order Waiting", process.getCode());
            return Transition.WAIT.toString();
        }

        if (PaymentStatus.INGENICO_AUTHORIZED.equals(order.getPaymentStatus())
                || PaymentStatus.INGENICO_WAITING_CAPTURE.equals(order.getPaymentStatus())
                || PaymentStatus.INGENICO_CAPTURED.equals(order.getPaymentStatus())) {
            LOG.debug("[INGENICO] Process: {} Order Authorized", process.getCode());
            order.setStatus(OrderStatus.PAYMENT_AUTHORIZED);
            modelService.save(order);
            return Transition.OK.toString();
        } else if (PaymentStatus.INGENICO_REJECTED.equals(order.getPaymentStatus())) {
            LOG.debug("[INGENICO] Process: {} Order Not Authorized", process.getCode());
            order.setStatus(OrderStatus.PAYMENT_NOT_AUTHORIZED);
            modelService.save(order);
            return Transition.NOK.toString();
        } else {
            LOG.debug("[INGENICO] Process: {} Order Waiting Auth", process.getCode());
            order.setPaymentStatus(PaymentStatus.INGENICO_WAITING_AUTH);
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