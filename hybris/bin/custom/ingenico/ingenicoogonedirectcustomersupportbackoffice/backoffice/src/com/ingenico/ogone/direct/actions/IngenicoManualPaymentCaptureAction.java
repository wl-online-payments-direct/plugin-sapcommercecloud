package com.ingenico.ogone.direct.actions;

import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.INGENICO_EVENT_CAPTURE;
import static de.hybris.platform.core.enums.PaymentStatus.INGENICO_AUTHORIZED;
import static de.hybris.platform.core.enums.PaymentStatus.INGENICO_WAITING_CAPTURE;

import javax.annotation.Resource;

import com.hybris.cockpitng.actions.ActionContext;
import com.hybris.cockpitng.actions.ActionResult;
import com.hybris.cockpitng.actions.CockpitAction;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.omsbackoffice.actions.order.ManualPaymentCaptureAction;
import de.hybris.platform.payment.enums.PaymentTransactionType;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.payment.model.PaymentTransactionModel;
import de.hybris.platform.store.BaseStoreModel;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zhtml.Messagebox;

import com.ingenico.direct.domain.CaptureResponse;
import com.ingenico.direct.domain.CapturesResponse;
import com.ingenico.ogone.direct.model.IngenicoConfigurationModel;
import com.ingenico.ogone.direct.service.IngenicoBusinessProcessService;
import com.ingenico.ogone.direct.service.IngenicoPaymentService;
import com.ingenico.ogone.direct.service.IngenicoTransactionService;
import com.ingenico.ogone.direct.util.IngenicoAmountUtils;

public class IngenicoManualPaymentCaptureAction extends ManualPaymentCaptureAction implements CockpitAction<OrderModel, OrderModel> {

    private final static Logger LOGGER = LoggerFactory.getLogger(IngenicoManualPaymentCaptureAction.class);

    @Resource
    private IngenicoPaymentService ingenicoPaymentService;

    @Resource
    private IngenicoTransactionService ingenicoTransactionService;

    @Resource
    private IngenicoBusinessProcessService ingenicoBusinessProcessService;

    @Resource
    private IngenicoAmountUtils ingenicoAmountUtils;

    @Override
    public ActionResult<OrderModel> perform(ActionContext<OrderModel> actionContext) {
        OrderModel order = actionContext.getData();
        ActionResult<OrderModel> result = null;
        String resultMessage = null;

        try {
            final PaymentTransactionEntryModel paymentTransactionToCapture = getPaymentTransactionToCapture(order);
            final IngenicoConfigurationModel ingenicoConfiguration = getIngenicoConfiguration(order);

            final CapturesResponse captures = ingenicoPaymentService.getCaptures(ingenicoConfiguration, paymentTransactionToCapture.getRequestId());

            if (CollectionUtils.isNotEmpty(captures.getCaptures())) {
                captures.getCaptures().forEach(capture -> ingenicoTransactionService.processCapture(capture));
            }

            final Long nonCapturedAmount = ingenicoPaymentService.getNonCapturedAmount(ingenicoConfiguration,
                    captures,
                    paymentTransactionToCapture.getPaymentTransaction().getPlannedAmount(),
                    paymentTransactionToCapture.getCurrency().getIsocode());
            if (nonCapturedAmount > 0) {
                final CaptureResponse captureResponse = ingenicoPaymentService.capturePayment(ingenicoConfiguration,
                        paymentTransactionToCapture.getRequestId(),
                        ingenicoAmountUtils.fromAmount(nonCapturedAmount, paymentTransactionToCapture.getCurrency().getIsocode()),
                        paymentTransactionToCapture.getCurrency().getIsocode());
                ingenicoTransactionService.updatePaymentTransaction(paymentTransactionToCapture.getPaymentTransaction(),
                        captureResponse.getId(),
                        captureResponse.getStatus(),
                        captureResponse.getCaptureOutput().getAmountOfMoney(),
                        PaymentTransactionType.CAPTURE);
            }

            ingenicoBusinessProcessService.triggerOrderProcessEvent(order, INGENICO_EVENT_CAPTURE);
            result = new ActionResult<OrderModel>(ActionResult.SUCCESS, order);
            resultMessage = actionContext.getLabel("action.manualpaymentcapture.success");

        } catch (Exception exception) {
            result = new ActionResult<OrderModel>(ActionResult.ERROR, order);
            resultMessage = actionContext.getLabel("action.manualpaymentcapture.error");
            LOGGER.error("[INGENICO] Error while capturing : ", exception);
        } finally {
            Messagebox.show(resultMessage + " (" + result.getResultCode() + ")");
        }
        return result;
    }

    @Override
    public boolean canPerform(ActionContext<OrderModel> ctx) {
        OrderModel order = ctx.getData();

        if (getPaymentTransactionToCapture(order) == null) { // if payment is directly captured
            return false;
        }

        return order != null && (INGENICO_WAITING_CAPTURE.equals(order.getPaymentStatus()) || INGENICO_AUTHORIZED.equals(order.getPaymentStatus()));
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
