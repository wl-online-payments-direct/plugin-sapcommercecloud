package com.ingenico.ogone.direct.actions;

import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.INGENICO_EVENT_CAPTURE;
import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_STATUS_ENUM.CANCELLED;
import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_STATUS_ENUM.CAPTURE_REQUESTED;

import javax.annotation.Resource;
import java.util.List;

import com.hybris.cockpitng.actions.ActionContext;
import com.hybris.cockpitng.actions.ActionResult;
import com.hybris.cockpitng.actions.CockpitAction;
import com.ingenico.direct.domain.CancelPaymentResponse;
import com.ingenico.ogone.direct.model.IngenicoConfigurationModel;
import com.ingenico.ogone.direct.service.IngenicoBusinessProcessService;
import com.ingenico.ogone.direct.service.IngenicoPaymentService;
import com.ingenico.ogone.direct.service.IngenicoTransactionService;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.payment.enums.PaymentTransactionType;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.payment.model.PaymentTransactionModel;
import de.hybris.platform.store.BaseStoreModel;
import org.zkoss.zhtml.Messagebox;

public class IngenicoManualPaymentReverseAuthAction implements CockpitAction<OrderModel, OrderModel> {

   @Resource
   private IngenicoPaymentService ingenicoPaymentService;

   @Resource
   private IngenicoTransactionService ingenicoTransactionService;

   @Resource
   private IngenicoBusinessProcessService ingenicoBusinessProcessService;

   @Override public ActionResult<OrderModel> perform(ActionContext<OrderModel> actionContext) {
      OrderModel order = actionContext.getData();

      final PaymentTransactionEntryModel paymentTransactionToCancel = getPaymentTransactionToCancel(order);
      final IngenicoConfigurationModel ingenicoConfiguration = getIngenicoConfiguration(order);

      CancelPaymentResponse cancelPaymentResponse = ingenicoPaymentService.cancelPayment(ingenicoConfiguration, paymentTransactionToCancel.getRequestId());
      ingenicoTransactionService.updatePaymentTransaction(paymentTransactionToCancel.getPaymentTransaction(),
            paymentTransactionToCancel.getRequestId(),
            cancelPaymentResponse.getPayment().getStatus(),
            cancelPaymentResponse.getPayment().getStatus(),
            cancelPaymentResponse.getPayment().getPaymentOutput().getAmountOfMoney(),
            PaymentTransactionType.CANCEL);

      ActionResult<OrderModel> result = null;
      String resultMessage = null;

      if (CANCELLED.getValue().equals(cancelPaymentResponse.getPayment().getStatus())) {
         ingenicoBusinessProcessService.triggerOrderProcessEvent(order, "cancelOrder");
         result = new ActionResult<OrderModel>(ActionResult.SUCCESS, order);
         resultMessage = actionContext.getLabel("action.manualpaymentcancelation.success");
      } else {
         result = new ActionResult<OrderModel>(ActionResult.ERROR, order);
         resultMessage = actionContext.getLabel("action.manualpaymentcancelation.error");
      }
      Messagebox.show(resultMessage + ", " + cancelPaymentResponse.getPayment().getStatus() + ", (" + result.getResultCode() + ")");

      return result;
   }

   @Override public boolean canPerform(ActionContext<OrderModel> ctx) {
      OrderModel order = ctx.getData();
      boolean isTransactionAuthorizedNotCaptured = false;

      List<PaymentTransactionEntryModel> paymentTransactionEntryModels = order.getPaymentTransactions().get(order.getPaymentTransactions().size() - 1).getEntries(); // take the last one - it will be the most recent one
      for (PaymentTransactionEntryModel paymentTransactionEntryModel : paymentTransactionEntryModels) {
         if (PaymentTransactionType.AUTHORIZATION.equals(paymentTransactionEntryModel.getType())) {
            isTransactionAuthorizedNotCaptured = "SUCCESSFUL".equals(paymentTransactionEntryModel.getTransactionStatus()); // TODO change this status to PENDING_CAPTURE when the code is ready use PAYMENT_STATUS_ENUM.PENDING_CAPTURE
         } else if (PaymentTransactionType.CAPTURE.equals(paymentTransactionEntryModel.getType())) {
            isTransactionAuthorizedNotCaptured = !"SUCCESSFUL".equals(paymentTransactionEntryModel.getTransactionStatus());
         }
      }
      return order != null && isTransactionAuthorizedNotCaptured;
   }

   private PaymentTransactionEntryModel getPaymentTransactionToCancel(final OrderModel order) {
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
