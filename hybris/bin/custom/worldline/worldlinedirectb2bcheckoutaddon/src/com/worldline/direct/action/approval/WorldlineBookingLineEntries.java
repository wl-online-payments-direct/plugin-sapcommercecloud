package com.worldline.direct.action.approval;

import de.hybris.platform.b2b.process.approval.actions.SetBookingLineEntries;
import de.hybris.platform.b2b.process.approval.model.B2BApprovalProcessModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.payment.CreditCardPaymentInfoModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.task.RetryLaterException;

public class WorldlineBookingLineEntries extends SetBookingLineEntries {
    @Override
    public Transition executeAction(final B2BApprovalProcessModel process) throws RetryLaterException {
        final OrderModel order = process.getOrder();
        modelService.refresh(order);

        if (order.getPaymentInfo() instanceof CreditCardPaymentInfoModel || order.getPaymentInfo() instanceof WorldlinePaymentInfoModel) {
            return Transition.OK;
        } else {
            return super.executeAction(process);
        }
    }

}
