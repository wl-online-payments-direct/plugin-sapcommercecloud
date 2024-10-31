package com.worldline.direct.actions;

import static de.hybris.platform.core.enums.PaymentStatus.WORLDLINE_AUTHORIZED;
import static de.hybris.platform.core.enums.PaymentStatus.WORLDLINE_WAITING_CAPTURE;

import com.hybris.cockpitng.actions.ActionContext;
import com.hybris.cockpitng.actions.ActionResult;
import com.hybris.cockpitng.actions.CockpitAction;
import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.payment.PaymentModeModel;
import de.hybris.platform.omsbackoffice.actions.order.ManualPaymentCaptureAction;
import de.hybris.platform.payment.enums.PaymentTransactionType;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.payment.model.PaymentTransactionModel;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.List;

public class WorldlineManualPaymentCaptureAction extends ManualPaymentCaptureAction implements CockpitAction<OrderModel, OrderModel> {

    private static final String TWINT_BACKOFFICE_MESSAGE_KEY = "action.worldline.twint.capture.confirmation";

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
        List<PaymentTransactionEntryModel> entries = finalPaymentTransaction.getEntries();
        PaymentModeModel paymentMode = order.getPaymentMode();
        // Special case for Twint payments. If any Capture has already been requested, disallow further manual captures (WL4SAP-12)
        if (paymentMode != null && Integer.valueOf(paymentMode.getCode()) == WorldlinedirectcoreConstants.PAYMENT_METHOD_TWINT) {
            for (PaymentTransactionEntryModel entry : entries) {
                if (PaymentTransactionType.CAPTURE.equals(entry.getType()) || PaymentTransactionType.PARTIAL_CAPTURE.equals(entry.getType())) {
                    return null;
                }
            }
        }
        return entries
                .stream()
                .filter(entry -> (PaymentTransactionType.AUTHORIZATION.equals(entry.getType()) && !(BigDecimal.ZERO.compareTo(entry.getAmount()) == 0)))
                .findFirst().orElse(null);

    }

    @Override
    public String getConfirmationMessage(ActionContext<OrderModel> ctx) {
        int paymentModeCode = getPaymentModeCodeAsInt(ctx);
        if (WorldlinedirectcoreConstants.PAYMENT_METHOD_TWINT == paymentModeCode) {
            return ctx.getLabel(TWINT_BACKOFFICE_MESSAGE_KEY);
        }

        return StringUtils.EMPTY;
    }

    @Override
    public boolean needsConfirmation(ActionContext<OrderModel> ctx) {
        int paymentModeCode = getPaymentModeCodeAsInt(ctx);
        if (WorldlinedirectcoreConstants.PAYMENT_METHOD_TWINT == paymentModeCode) {
            return true;
        }

        return false;
    }

    private int getPaymentModeCodeAsInt(ActionContext<OrderModel> ctx) {
        OrderModel order = ctx.getData();
        if (order == null) {
            return -1;
        }

        PaymentModeModel paymentMode = order.getPaymentMode();
        if (paymentMode == null) {
            return -1;
        }

        return Integer.valueOf(paymentMode.getCode());
    }

}
