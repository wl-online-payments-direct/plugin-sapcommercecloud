package com.ingenico.ogone.direct.widgets.ingenicopartialcapture;

import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.INGENICO_EVENT_PAYMENT;

import com.hybris.cockpitng.annotations.SocketEvent;
import com.hybris.cockpitng.annotations.ViewEvent;
import com.hybris.cockpitng.util.DefaultWidgetController;
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
import com.ingenico.ogone.direct.model.IngenicoConfigurationModel;
import com.ingenico.ogone.direct.service.IngenicoBusinessProcessService;
import com.ingenico.ogone.direct.service.IngenicoPaymentService;
import com.ingenico.ogone.direct.service.IngenicoTransactionService;
import com.ingenico.ogone.direct.util.IngenicoAmountUtils;

public class IngenicoPartialCaptureController extends DefaultWidgetController {
   private final static Logger LOGGER = LoggerFactory.getLogger(IngenicoPartialCaptureController.class);


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
   private transient IngenicoPaymentService ingenicoPaymentService;

   @WireVariable
   private transient IngenicoTransactionService ingenicoTransactionService;

   @WireVariable
   private transient IngenicoBusinessProcessService ingenicoBusinessProcessService;

   @WireVariable
   private transient IngenicoAmountUtils ingenicoAmountUtils;

   private OrderModel orderModel;

   // send amount to Ingenico
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
      this.getWidgetInstanceManager().setTitle(this.getWidgetInstanceManager().getLabel("ingenicoogonedirectcustomersupportbackoffice.partial.capture.popup.title") + " " + this.getOrderModel().getCode());
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
            final PaymentTransactionEntryModel paymentTransactionToCapture = getPaymentTransactionToCapture(this.orderModel);
            final IngenicoConfigurationModel ingenicoConfiguration = getIngenicoConfiguration(this.orderModel);

            final CapturesResponse captures = ingenicoPaymentService.getCaptures(ingenicoConfiguration, paymentTransactionToCapture.getRequestId());

            if (CollectionUtils.isNotEmpty(captures.getCaptures())) {
                captures.getCaptures().forEach(capture -> ingenicoTransactionService.processCapture(capture));
            }

            final Long nonCapturedAmount = ingenicoPaymentService.getNonCapturedAmount(ingenicoConfiguration,
                    paymentTransactionToCapture.getRequestId(),
                    captures,
                    paymentTransactionToCapture.getPaymentTransaction().getPlannedAmount(),
                    paymentTransactionToCapture.getCurrency().getIsocode());
            final Long requestedAmount = ingenicoAmountUtils.createAmount(amount.doubleValue(), paymentTransactionToCapture.getCurrency().getIsocode());
            final Long amountToCapture = this.fullAmount.isChecked() || nonCapturedAmount < requestedAmount ? nonCapturedAmount : requestedAmount;
            if (nonCapturedAmount > 0 && amountToCapture <= nonCapturedAmount) {
                final CaptureResponse captureResponse = ingenicoPaymentService.capturePayment(ingenicoConfiguration,
                        paymentTransactionToCapture.getRequestId(),
                        ingenicoAmountUtils.fromAmount(amountToCapture, paymentTransactionToCapture.getCurrency().getIsocode()),
                        paymentTransactionToCapture.getCurrency().getIsocode(),
                        amountToCapture.equals(nonCapturedAmount) ? Boolean.TRUE : Boolean.FALSE);
                ingenicoTransactionService.updatePaymentTransaction(paymentTransactionToCapture.getPaymentTransaction(),
                        captureResponse.getId(),
                        captureResponse.getStatus(),
                        captureResponse.getCaptureOutput().getAmountOfMoney(),
                        PaymentTransactionType.CAPTURE);
            } else {
               LOGGER.debug("[INGENICO] Don't send request to Ingenico when nonCapturedAmount=" + nonCapturedAmount + " is less than amountToCapture=" + amountToCapture + ".");
            }

            ingenicoBusinessProcessService.triggerOrderProcessEvent(this.orderModel, INGENICO_EVENT_PAYMENT);
            LOGGER.info("[INGENICO] order business process triggered for capturing amount " + amountToCapture + " for order " + this.orderModel.getCode());
        } catch (Exception exception) {
            LOGGER.error("[INGENICO] Error while capturing : ", exception);
        }
         this.sendOutput("confirmpaymentcapture", COMPLETED);
      }
   }

      protected void validateRequest() {
      Double paymentAmount = this.amount.getValue();

      if (paymentAmount.doubleValue() < 0.0) {
         throw new WrongValueException(this.amount, this.getLabel("ingenicoogonedirectcustomersupportbackoffice.partial.capture.wrong.amount.message"));
      }
   }

   protected void showMessageBox() {
      Messagebox.show(this.getLabel("ingenicoogonedirectcustomersupportbackoffice.partial.capture.confirm.message"), this.getLabel("ingenicoogonedirectcustomersupportbackoffice.partial.capture.confirm.title") + " " + this.getOrderModel().getCode(), new Messagebox.Button[]{
            Messagebox.Button.NO, Messagebox.Button.YES}, "oms-widget-cancelorder-confirm-icon", this::processPaymentCapture);
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

   public void setIngenicoAmountUtils(IngenicoAmountUtils ingenicoAmountUtils) {
      this.ingenicoAmountUtils = ingenicoAmountUtils;
   }

   public void setOrderModel(OrderModel orderModel) {
      this.orderModel = orderModel;
   }

   protected OrderModel getOrderModel() {
      return orderModel;
   }

   private IngenicoConfigurationModel getIngenicoConfiguration(OrderModel orderModel) {
      final BaseStoreModel store = orderModel.getStore();
      return store != null ? store.getIngenicoConfiguration() : null;
   }

   private PaymentTransactionEntryModel getPaymentTransactionToCapture(final OrderModel order) {
      final PaymentTransactionModel finalPaymentTransaction = order.getPaymentTransactions().get(order.getPaymentTransactions().size() - 1);
      return finalPaymentTransaction.getEntries()
            .stream()
            .filter(entry -> PaymentTransactionType.AUTHORIZATION.equals(entry.getType()))
            .findFirst().orElse(null);
   }
}
