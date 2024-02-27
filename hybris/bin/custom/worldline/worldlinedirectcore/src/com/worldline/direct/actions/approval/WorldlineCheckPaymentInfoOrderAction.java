package com.worldline.direct.actions.approval;

import de.hybris.platform.b2b.process.approval.model.B2BApprovalProcessModel;
import de.hybris.platform.b2b.enums.CheckoutPaymentType;
import de.hybris.platform.core.enums.OrderStatus;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.payment.CreditCardPaymentInfoModel;
import de.hybris.platform.core.model.order.payment.PaymentInfoModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.processengine.action.AbstractSimpleDecisionAction;
import de.hybris.platform.task.RetryLaterException;
import org.apache.log4j.Logger;

public class WorldlineCheckPaymentInfoOrderAction extends AbstractSimpleDecisionAction<B2BApprovalProcessModel> {
    protected static final Logger LOG = Logger.getLogger(WorldlineCheckPaymentInfoOrderAction.class);

    @Override
    public Transition executeAction(B2BApprovalProcessModel process) throws RetryLaterException {
        OrderModel order = null;
        Transition transition = Transition.NOK;
        try {
            order = process.getOrder();
            final PaymentInfoModel paymentInfo = order.getPaymentInfo();

            if (CheckoutPaymentType.CARD.equals(order.getPaymentType()) && (paymentInfo instanceof CreditCardPaymentInfoModel || paymentInfo instanceof WorldlinePaymentInfoModel)) {
                transition = Transition.OK;
            }
        } catch (final Exception e) {
            this.handleError(order, e);
        }
        return transition;
    }

    protected void handleError(final OrderModel order, final Exception exception) {
        if (order != null) {
            this.setOrderStatus(order, OrderStatus.B2B_PROCESSING_ERROR);
        }
        LOG.error(exception.getMessage(), exception);
    }
}
