package com.worldline.direct.widgets.worldlinepartialcapture;

import static com.worldline.direct.constants.WorldlinedirectcoreConstants.WORLDLINE_EVENT_PAYMENT;

import java.math.BigDecimal;

import com.hybris.cockpitng.annotations.SocketEvent;
import com.hybris.cockpitng.annotations.ViewEvent;
import com.hybris.cockpitng.util.DefaultWidgetController;
import com.worldline.direct.service.WorldlineBusinessProcessService;
import com.worldline.direct.util.WorldlineAmountUtils;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.payment.enums.PaymentTransactionType;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.payment.model.PaymentTransactionModel;
import de.hybris.platform.store.BaseStoreModel;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;

import com.ingenico.direct.domain.CaptureResponse;
import com.ingenico.direct.domain.CapturesResponse;
import com.worldline.direct.model.WorldlineConfigurationModel;
import com.worldline.direct.service.WorldlinePaymentService;
import com.worldline.direct.service.WorldlineTransactionService;

public class WorldlinePartialCaptureController extends DefaultWidgetController {
    private final static Logger LOGGER = LoggerFactory.getLogger(WorldlinePartialCaptureController.class);


    protected static final Object COMPLETED = "completed";

    @Wire
    private Textbox orderNumber;

    @Wire
    private Textbox customerName;

    @Wire
    private Checkbox fullAmount;

    @Wire
    private Doublebox amount;

    @WireVariable
    private transient WorldlinePaymentService worldlinePaymentService;

    @WireVariable
    private transient WorldlineTransactionService worldlineTransactionService;

    @WireVariable
    private transient WorldlineBusinessProcessService worldlineBusinessProcessService;

    @WireVariable
    private transient WorldlineAmountUtils worldlineAmountUtils;

    private OrderModel orderModel;
    private PaymentTransactionEntryModel paymentTransactionToCapture;
    private WorldlineConfigurationModel worldlineConfigurationModel;
    private Long amountToCapture;
    private Long nonCapturedAmount;

    // send amount to Worldline
    @ViewEvent(
            componentID = "confirmpayment",
            eventName = "onClick"
    )
    public void confirmCapturePayment() {
        this.validateRequest();
        this.showMessageBox();
    }

    // clear the amount that is entered
    @ViewEvent(
            componentID = "clearamount",
            eventName = "onClick"
    )
    public void reset() {
        this.initCapturePaymentForm(this.getOrderModel());
    }

    //initialize the partial capture form
    @SocketEvent(
            socketId = "inputObject"
    )
    public void initCapturePaymentForm(OrderModel inputObject) {
        this.amount.setValue(0.0);
        this.amount.setReadonly(Boolean.FALSE);
        this.fullAmount.setChecked(false);
        this.setOrderModel(inputObject);
        this.getWidgetInstanceManager().setTitle(this.getWidgetInstanceManager().getLabel("worldlinedirectcustomersupportbackoffice.partial.capture.popup.title") + " " + this.getOrderModel().getCode());
        this.orderNumber.setValue(this.getOrderModel().getCode());
        this.customerName.setValue(this.getOrderModel().getUser().getDisplayName());
        this.addListeners();
    }

    protected void addListeners() {
        this.fullAmount.addEventListener("onCheck", (event) -> {
            if (this.fullAmount.isChecked()) {
                this.amount.setValue(0.0);
                this.amount.setReadonly(Boolean.TRUE);
            } else {
                this.amount.setReadonly(Boolean.FALSE);
            }
        });
    }

    protected void processPaymentCapture(Event obj) {
        if (Messagebox.Button.YES.event.equals(obj.getName())) {
            try {
                setPaymentTransactionToCapture(getPaymentTransactionToCapture(this.orderModel));
                setWorldlineConfiguration(getWorldlineConfiguration(this.orderModel));

                final CapturesResponse captures = worldlinePaymentService.getCaptures(worldlineConfigurationModel, paymentTransactionToCapture.getRequestId());

                if (CollectionUtils.isNotEmpty(captures.getCaptures())) {
                    captures.getCaptures().forEach(capture -> worldlineTransactionService.processCapture(capture));
                }

                setNonCapturedAmount(worldlinePaymentService.getNonCapturedAmount(worldlineConfigurationModel,
                        paymentTransactionToCapture.getRequestId(),
                        captures,
                        paymentTransactionToCapture.getPaymentTransaction().getPlannedAmount(),
                        paymentTransactionToCapture.getCurrency().getIsocode()));
                final Long requestedAmount = worldlineAmountUtils.createAmount(amount.doubleValue(), paymentTransactionToCapture.getCurrency().getIsocode());

                if (nonCapturedAmount <= 0) {
                    Messagebox.show(this.getLabel("worldlinedirectcustomersupportbackoffice.partial.capture.skipped.message"),
                            this.getLabel("worldlinedirectcustomersupportbackoffice.partial.capture.confirm.title", new String[]{this.getOrderModel().getCode()}),
                            1, "z-messagebox-icon z-messagebox-information", 0, null);
                } else {
                    final boolean invalidRequestedAmount = requestedAmount > nonCapturedAmount;
                    setAmountToCapture(this.fullAmount.isChecked() || invalidRequestedAmount ? nonCapturedAmount : requestedAmount);
                    if (!fullAmount.isChecked() && invalidRequestedAmount) {
                        showWarningMessageBox(worldlineAmountUtils.fromAmount(amountToCapture, paymentTransactionToCapture.getCurrency().getIsocode()));
                    } else {
                        capture();
                    }
                }
            } catch (Exception exception) {
                handleException(exception);
            }
            this.sendOutput("confirmpaymentcapture", COMPLETED);
        }
    }


    protected void continuePaymentCapture(Event event) {
        if (Messagebox.Button.OK.event.equals(event.getName())) {
            try {
                capture();
            } catch (Exception exception) {
                handleException(exception);
            }
        }
    }

    private void capture() {

        final CaptureResponse captureResponse = worldlinePaymentService.capturePayment(worldlineConfigurationModel,
                paymentTransactionToCapture.getRequestId(),
                worldlineAmountUtils.fromAmount(amountToCapture, paymentTransactionToCapture.getCurrency().getIsocode()),
                paymentTransactionToCapture.getCurrency().getIsocode(),
                amountToCapture.equals(nonCapturedAmount));
        worldlineTransactionService.updatePaymentTransaction(paymentTransactionToCapture.getPaymentTransaction(),
                captureResponse.getId(),
                captureResponse.getStatus(),
                captureResponse.getCaptureOutput().getAmountOfMoney(),
                PaymentTransactionType.CAPTURE);

        worldlineBusinessProcessService.triggerOrderProcessEvent(this.orderModel, WORLDLINE_EVENT_PAYMENT);
    }

    private WorldlineConfigurationModel getWorldlineConfiguration(OrderModel orderModel) {
        final BaseStoreModel store = orderModel.getStore();
        return store != null ? store.getWorldlineConfiguration() : null;
    }

    private PaymentTransactionEntryModel getPaymentTransactionToCapture(final OrderModel order) {
        final PaymentTransactionModel finalPaymentTransaction = order.getPaymentTransactions().get(order.getPaymentTransactions().size() - 1);
        return finalPaymentTransaction.getEntries()
                .stream()
                .filter(entry -> PaymentTransactionType.AUTHORIZATION.equals(entry.getType()))
                .findFirst().orElse(null);
    }


    protected void validateRequest() {
        Double paymentAmount = this.amount.getValue();

        if (!this.fullAmount.isChecked() && paymentAmount.doubleValue() <= 0.0) {
            throw new WrongValueException(this.amount, this.getLabel("worldlinedirectcustomersupportbackoffice.partial.capture.wrong.amount.message"));
        }
    }

    protected void showMessageBox() {
        Messagebox.show(this.getLabel("worldlinedirectcustomersupportbackoffice.partial.capture.confirm.message"),
                this.getLabel("worldlinedirectcustomersupportbackoffice.partial.capture.confirm.title", new String[]{this.getOrderModel().getCode()}),
                new Messagebox.Button[]{Messagebox.Button.NO, Messagebox.Button.YES}, "oms-widget-cancelorder-confirm-icon", this::processPaymentCapture);
    }

    protected void showWarningMessageBox(BigDecimal amountToCapture) {

        Messagebox.show(this.getLabel("worldlinedirectcustomersupportbackoffice.partial.capture.warn.message", new String[]{amountToCapture.toString()}),
                this.getLabel("worldlinedirectcustomersupportbackoffice.partial.capture.confirm.title", new String[]{this.getOrderModel().getCode()}),
                new Messagebox.Button[]{Messagebox.Button.CANCEL, Messagebox.Button.OK}, "z-messagebox-icon z-messagebox-information", this::continuePaymentCapture);
    }


    private void handleException(Exception exception) {
        Messagebox.show(this.getLabel("worldlinedirectcustomersupportbackoffice.partial.capture.error.message"),
                this.getLabel("worldlinedirectcustomersupportbackoffice.partial.capture.confirm.title", new String[]{this.getOrderModel().getCode()}),
                1, "z-messagebox-icon z-messagebox-information", 0, null);
        LOGGER.error("[WORLDLINE] Error while capturing : ", exception);
    }

    public void setOrderModel(OrderModel orderModel) {
        this.orderModel = orderModel;
    }

    protected OrderModel getOrderModel() {
        return orderModel;
    }


    public void setPaymentTransactionToCapture(PaymentTransactionEntryModel paymentTransactionToCapture) {
        this.paymentTransactionToCapture = paymentTransactionToCapture;
    }

    public void setWorldlineConfiguration(WorldlineConfigurationModel worldlineConfigurationModel) {
        this.worldlineConfigurationModel = worldlineConfigurationModel;
    }

    public void setAmountToCapture(Long amountToCapture) {
        this.amountToCapture = amountToCapture;
    }

    public void setNonCapturedAmount(Long nonCapturedAmount) {
        this.nonCapturedAmount = nonCapturedAmount;
    }
}
