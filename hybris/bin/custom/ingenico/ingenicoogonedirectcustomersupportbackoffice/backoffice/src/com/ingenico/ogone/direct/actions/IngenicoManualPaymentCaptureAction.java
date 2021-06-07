package com.ingenico.ogone.direct.actions;

import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.INGENICO_EVENT_CAPTURE;
import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_STATUS_ENUM.CAPTURE_REQUESTED;
import static de.hybris.platform.core.enums.PaymentStatus.INGENICO_AUTHORIZED;
import static de.hybris.platform.core.enums.PaymentStatus.INGENICO_WAITING_CAPTURE;

import javax.annotation.Resource;
import java.util.List;

import com.hybris.cockpitng.actions.ActionContext;
import com.hybris.cockpitng.actions.ActionResult;
import com.hybris.cockpitng.actions.CockpitAction;
import de.hybris.platform.core.enums.OrderStatus;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.omsbackoffice.actions.order.ManualPaymentCaptureAction;
import de.hybris.platform.payment.enums.PaymentTransactionType;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.payment.model.PaymentTransactionModel;
import de.hybris.platform.store.BaseStoreModel;

import org.zkoss.zhtml.Messagebox;

import com.ingenico.direct.domain.CaptureResponse;
import com.ingenico.ogone.direct.model.IngenicoConfigurationModel;
import com.ingenico.ogone.direct.service.IngenicoBusinessProcessService;
import com.ingenico.ogone.direct.service.IngenicoPaymentService;
import com.ingenico.ogone.direct.service.IngenicoTransactionService;

public class IngenicoManualPaymentCaptureAction extends ManualPaymentCaptureAction implements CockpitAction<OrderModel, OrderModel> {

    @Resource
    private IngenicoPaymentService ingenicoPaymentService;

    @Resource
    private IngenicoTransactionService ingenicoTransactionService;

    @Resource
    private IngenicoBusinessProcessService ingenicoBusinessProcessService;

    @Override
    public ActionResult<OrderModel> perform(ActionContext<OrderModel> actionContext) {
        OrderModel order = actionContext.getData();


        final PaymentTransactionEntryModel paymentTransactionToCapture = getPaymentTransactionToCapture(order);
        final IngenicoConfigurationModel ingenicoConfiguration = getIngenicoConfiguration(order);
        CaptureResponse captureResponse = ingenicoPaymentService.capturePayment(ingenicoConfiguration,
                paymentTransactionToCapture.getRequestId());

        //result
        ActionResult<OrderModel> result = null;
        String resultMessage = null;
        ingenicoTransactionService.updatePaymentTransaction(paymentTransactionToCapture.getPaymentTransaction(),
                paymentTransactionToCapture.getRequestId(),
                captureResponse.getStatus(),
                captureResponse.getStatus(),
                captureResponse.getCaptureOutput().getAmountOfMoney(),
                PaymentTransactionType.CAPTURE);

        if (CAPTURE_REQUESTED.getValue().equals(captureResponse.getStatus())) {
            ingenicoBusinessProcessService.triggerOrderProcessEvent(order, INGENICO_EVENT_CAPTURE);
            result = new ActionResult<OrderModel>(ActionResult.SUCCESS, order);
            resultMessage = actionContext.getLabel("action.manualpaymentcapture.success");
        } else {
            result = new ActionResult<OrderModel>(ActionResult.ERROR, order);
            resultMessage = actionContext.getLabel("action.manualpaymentcapture.error");
        }
        Messagebox.show(resultMessage + ", " + captureResponse.getStatus() + ", (" + result.getResultCode() + ")");

        return result;
    }

    @Override
    public boolean canPerform(ActionContext<OrderModel> ctx) {
        OrderModel order = ctx.getData();

        return order != null && !OrderStatus.CANCELLED.equals(order.getStatus()) &&
              (INGENICO_AUTHORIZED.equals(order.getPaymentStatus()) || INGENICO_WAITING_CAPTURE.equals(order.getPaymentStatus()));
    }

    private PaymentTransactionEntryModel getPaymentTransactionToCapture(final OrderModel order) {
        final PaymentTransactionModel finalPaymentTransaction = order.getPaymentTransactions().get(order.getPaymentTransactions().size() - 1);
        return finalPaymentTransaction.getEntries()
                .stream()
                .filter(entry -> PaymentTransactionType.AUTHORIZATION.equals(entry.getType()))
                .findFirst().orElse(null);
    }

    private IngenicoConfigurationModel getIngenicoConfiguration(OrderModel orderModel) {
        final BaseStoreModel store = orderModel.getStore();
        return store != null ? store.getIngenicoConfiguration() : null;
    }

    public void setIngenicoPaymentService(IngenicoPaymentService ingenicoPaymentService) {
        this.ingenicoPaymentService = ingenicoPaymentService;
    }

    public void setIngenicoTransactionService(IngenicoTransactionService ingenicoTransactionService) {
        this.ingenicoTransactionService = ingenicoTransactionService;
    }

    public void setIngenicoBusinessProcessService(IngenicoBusinessProcessService ingenicoBusinessProcessService) {
        this.ingenicoBusinessProcessService = ingenicoBusinessProcessService;
    }
}
