package com.worldline.direct.actions;

import static de.hybris.platform.core.enums.PaymentStatus.WORLDLINE_AUTHORIZED;
import static de.hybris.platform.core.enums.PaymentStatus.WORLDLINE_WAITING_CAPTURE;

import com.hybris.cockpitng.actions.ActionContext;
import com.hybris.cockpitng.actions.ActionResult;
import com.hybris.cockpitng.actions.CockpitAction;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.omsbackoffice.actions.order.ManualPaymentCaptureAction;
import de.hybris.platform.payment.enums.PaymentTransactionType;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.payment.model.PaymentTransactionModel;

public class WorldlineManualPaymentCaptureAction extends ManualPaymentCaptureAction implements CockpitAction<OrderModel, OrderModel> {

    @Override
    public ActionResult<OrderModel> perform(ActionContext<OrderModel> actionContext) {

        this.sendOutput("capturePaymentContext", actionContext.getData());
        return new ActionResult("success");

    }

    @Override
    public boolean canPerform(ActionContext<OrderModel> ctx) {
        OrderModel order = ctx.getData();

        if (getPaymentTransactionToCapture(order) == null) { // if payment is directly captured
            return false;
        }

        return order != null && (WORLDLINE_WAITING_CAPTURE.equals(order.getPaymentStatus()) || WORLDLINE_AUTHORIZED.equals(order.getPaymentStatus()));
    }

    private PaymentTransactionEntryModel getPaymentTransactionToCapture(final OrderModel order) {
        final PaymentTransactionModel finalPaymentTransaction = order.getPaymentTransactions().get(order.getPaymentTransactions().size() - 1);
        return finalPaymentTransaction.getEntries()
                .stream()
                .filter(entry -> PaymentTransactionType.AUTHORIZATION.equals(entry.getType()))
                .findFirst().orElse(null);
    }

}
