package com.ingenico.ogone.direct.actions.order;

import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_STATUS_CATEGORY_ENUM.STATUS_UNKNOWN;

import java.util.HashSet;
import java.util.Set;

import de.hybris.platform.core.enums.OrderStatus;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.payment.IngenicoPaymentInfoModel;
import de.hybris.platform.orderprocessing.model.OrderProcessModel;
import de.hybris.platform.payment.enums.PaymentTransactionType;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.payment.model.PaymentTransactionModel;
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

        if (CollectionUtils.isEmpty(order.getPaymentTransactions())) {
            LOG.debug("[INGENICO] Process: {} Order Waiting", process.getCode());
            return Transition.WAIT.toString();
        }

        switch (getAuthorizationStatus(order)) {
            case "SUCCESSFUL":
                LOG.debug("[INGENICO] Process: {} Order Authorized", process.getCode());
                order.setStatus(OrderStatus.PAYMENT_AUTHORIZED);
                modelService.save(order);
                return Transition.OK.toString();
            case "REJECTED":
                LOG.debug("[INGENICO] Process: {} Order Not Authorized", process.getCode());
                order.setStatus(OrderStatus.PAYMENT_NOT_AUTHORIZED);
                modelService.save(order);
                return Transition.NOK.toString();
            case "STATUS_UNKNOWN":
            default:
                LOG.debug("[INGENICO] Process: {} Order Waiting", process.getCode());
                return Transition.WAIT.toString();
        }
    }

    private String getAuthorizationStatus(final OrderModel order) {
        final PaymentTransactionModel finalPaymentTransaction = order.getPaymentTransactions().get(order.getPaymentTransactions().size() - 1);
        return finalPaymentTransaction.getEntries()
                .stream()
                .filter(entry -> PaymentTransactionType.AUTHORIZATION.equals(entry.getType()))
                .map(PaymentTransactionEntryModel::getTransactionStatus)
                .findFirst().orElseGet(STATUS_UNKNOWN::getValue);
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