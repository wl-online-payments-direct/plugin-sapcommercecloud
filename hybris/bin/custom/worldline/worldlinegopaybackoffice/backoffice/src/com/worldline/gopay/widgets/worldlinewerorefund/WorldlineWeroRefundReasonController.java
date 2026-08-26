package com.worldline.gopay.widgets.worldlinewerorefund;

import com.hybris.cockpitng.annotations.SocketEvent;
import com.hybris.cockpitng.annotations.ViewEvent;
import com.hybris.cockpitng.util.DefaultWidgetController;
import com.onlinepayments.domain.RefundResponse;
import com.worldline.gopay.constants.WorldlinegopaycoreConstants;
import com.worldline.gopay.enums.WeroRefundReason;
import com.worldline.gopay.service.WorldlineBusinessProcessService;
import com.worldline.gopay.service.WorldlinePaymentModeService;
import com.worldline.gopay.service.WorldlinePaymentService;
import com.worldline.gopay.service.WorldlineTransactionService;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.payment.enums.PaymentTransactionType;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.payment.model.PaymentTransactionModel;
import de.hybris.platform.returns.model.RefundEntryModel;
import de.hybris.platform.returns.model.ReturnRequestModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;

import java.math.BigDecimal;

import static com.worldline.gopay.constants.WorldlinegopaycoreConstants.PAYMENT_STATUS_ENUM.REFUNDED;
import static com.worldline.gopay.constants.WorldlinegopaycoreConstants.PAYMENT_STATUS_ENUM.REFUND_REQUESTED;
import static com.worldline.gopay.constants.WorldlinegopaycoreConstants.WORLDLINE_EVENT_REFUND;

public class WorldlineWeroRefundReasonController extends DefaultWidgetController {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorldlineWeroRefundReasonController.class);
    protected static final Object COMPLETED = "completed";

    @Wire
    private Textbox orderNumber;

    @Wire
    private Textbox customerName;

    @Wire
    private Combobox refundReasonCombobox;

    @WireVariable
    private transient WorldlinePaymentService worldlinePaymentService;

    @WireVariable
    private transient WorldlineTransactionService worldlineTransactionService;

    @WireVariable
    private transient WorldlineBusinessProcessService worldlineBusinessProcessService;

    @WireVariable
    private transient WorldlinePaymentModeService worldlinePaymentModeService;

    private ReturnRequestModel returnRequestModel;

    @ViewEvent(
            componentID = "confirmrefund",
            eventName = "onClick"
    )
    public void confirmRefund() {
        if (refundReasonCombobox.getSelectedItem() == null) {
            return;
        }
        this.showConfirmMessageBox();
    }

    @ViewEvent(
            componentID = "cancelrefund",
            eventName = "onClick"
    )
    public void cancel() {
        this.sendOutput("confirmrefund", COMPLETED);
    }

    @SocketEvent(
            socketId = "inputObject"
    )
    public void initWeroRefundForm(ReturnRequestModel inputObject) {
        this.returnRequestModel = inputObject;
        OrderModel order = returnRequestModel.getOrder();

        this.getWidgetInstanceManager().setTitle(
                this.getWidgetInstanceManager().getLabel("worldline.werorefund.popup.title") + " " + order.getCode());
        this.orderNumber.setValue(order.getCode());
        this.customerName.setValue(order.getUser().getDisplayName());

        populateRefundReasons();
    }

    private void populateRefundReasons() {
        refundReasonCombobox.getItems().clear();
        refundReasonCombobox.setSelectedItem(null);

        WeroRefundReason existingReason = getExistingWeroRefundReason();

        for (WeroRefundReason reason : WeroRefundReason.values()) {
            Comboitem item = new Comboitem();
            String labelKey = "worldline.werorefund.reason." + reason.getCode();
            String label = this.getLabel(labelKey);
            item.setLabel(label != null && !label.equals(labelKey) ? label : reason.getCode());
            item.setValue(reason);
            refundReasonCombobox.appendChild(item);
            if (reason.equals(existingReason)) {
                refundReasonCombobox.setSelectedItem(item);
            }
        }
    }

    private WeroRefundReason getExistingWeroRefundReason() {
        if (returnRequestModel == null || returnRequestModel.getReturnEntries() == null) {
            return null;
        }
        return returnRequestModel.getReturnEntries().stream()
                .filter(RefundEntryModel.class::isInstance)
                .map(RefundEntryModel.class::cast)
                .map(RefundEntryModel::getWeroRefundReason)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    protected void showConfirmMessageBox() {
        Messagebox.show(
                this.getLabel("worldline.werorefund.confirm.message"),
                this.getLabel("worldline.werorefund.confirm.title") + " " + returnRequestModel.getOrder().getCode(),
                new Messagebox.Button[]{Messagebox.Button.NO, Messagebox.Button.YES},
                "oms-widget-cancelorder-confirm-icon",
                this::processRefund);
    }

    protected void processRefund(org.zkoss.zk.ui.event.Event event) {
        if (!Messagebox.Button.YES.event.equals(event.getName())) {
            return;
        }

        try {
            OrderModel order = returnRequestModel.getOrder();
            PaymentTransactionEntryModel paymentTransactionToRefund = getPaymentTransactionToRefund(order);

            if (paymentTransactionToRefund == null) {
                Messagebox.show(this.getLabel("worldline.werorefund.error.message"));
                return;
            }

            BigDecimal refundAmount = calculateRefundAmount(returnRequestModel,
                    order.getStore().getUid(),
                    paymentTransactionToRefund.getPaymentTransaction().getWorldlineRawTransactionCode(),
                    paymentTransactionToRefund.getPaymentTransaction().getPlannedAmount(),
                    paymentTransactionToRefund.getCurrency().getIsocode());

            WeroRefundReason selectedReason = refundReasonCombobox.getSelectedItem().getValue();

            RefundResponse refundResponse = worldlinePaymentService.refundPayment(
                    order.getStore().getUid(),
                    paymentTransactionToRefund.getRequestId(),
                    refundAmount,
                    paymentTransactionToRefund.getCurrency().getIsocode(),
                    selectedReason.getCode());

            if (REFUND_REQUESTED.getValue().equals(refundResponse.getStatus())
                    || REFUNDED.getValue().equals(refundResponse.getStatus())) {
                worldlineTransactionService.updatePaymentTransaction(
                        paymentTransactionToRefund.getPaymentTransaction(),
                        paymentTransactionToRefund.getRequestId(),
                        refundResponse.getStatus(),
                        refundResponse.getRefundOutput().getAmountOfMoney(),
                        PaymentTransactionType.REFUND_FOLLOW_ON);
                worldlineBusinessProcessService.triggerReturnProcessEvent(order, WORLDLINE_EVENT_REFUND);
                Messagebox.show(this.getLabel("worldline.werorefund.success.message")
                        + " (" + refundResponse.getStatus() + ")");
            } else {
                Messagebox.show(this.getLabel("worldline.werorefund.error.message")
                        + " (" + refundResponse.getStatus() + ")");
            }
        } catch (Exception e) {
            LOGGER.error("[WORLDLINE] Error while processing Wero refund: ", e);
            Messagebox.show(this.getLabel("worldline.werorefund.error.message"));
        }

        this.sendOutput("confirmrefund", COMPLETED);
    }

    private BigDecimal calculateRefundAmount(ReturnRequestModel returnRequest, String storeId,
                                             String paymentId, BigDecimal plannedAmount, String currencyISOcode) {
        Long nonCapturedAmount = worldlinePaymentService.getNonCapturedAmount(storeId, paymentId, plannedAmount, currencyISOcode);
        BigDecimal capturedAmount = plannedAmount.subtract(new BigDecimal(nonCapturedAmount));
        BigDecimal refundAmount = returnRequest.getSubtotal();
        if (returnRequest.getRefundDeliveryCost()) {
            BigDecimal deliveryCost = new BigDecimal(returnRequest.getOrder().getDeliveryCost());
            refundAmount = returnRequest.getSubtotal().add(deliveryCost.setScale(refundAmount.scale(), BigDecimal.ROUND_HALF_EVEN));
        }
        if (capturedAmount.compareTo(refundAmount) >= 0) {
            return refundAmount;
        } else {
            return BigDecimal.ZERO;
        }
    }

    private PaymentTransactionEntryModel getPaymentTransactionToRefund(OrderModel order) {
        final PaymentTransactionModel finalPaymentTransaction =
                order.getPaymentTransactions().get(order.getPaymentTransactions().size() - 1);
        return finalPaymentTransaction.getEntries()
                .stream()
                .filter(entry -> PaymentTransactionType.CAPTURE.equals(entry.getType()))
                .filter(entry -> WorldlinegopaycoreConstants.PAYMENT_STATUS_CATEGORY_ENUM.SUCCESSFUL.getValue()
                        .equals(entry.getTransactionStatus()))
                .findFirst().orElse(null);
    }
}
