package com.ingenico.ogone.direct.actions;

import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.INGENICO_EVENT_REFUND;
import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_STATUS_ENUM.REFUND_REQUESTED;
import static de.hybris.platform.core.enums.PaymentStatus.INGENICO_CAPTURED;

import javax.annotation.Resource;

import com.hybris.cockpitng.actions.ActionContext;
import com.hybris.cockpitng.actions.ActionResult;
import com.hybris.cockpitng.actions.CockpitAction;
import com.ingenico.direct.domain.RefundResponse;
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

public class IngenicoManualPaymentRefundAction implements CockpitAction<OrderModel, OrderModel> {

   @Resource
   private IngenicoPaymentService ingenicoPaymentService;

   @Resource
   private IngenicoTransactionService ingenicoTransactionService;

   @Resource
   private IngenicoBusinessProcessService ingenicoBusinessProcessService;

   @Override public ActionResult<OrderModel> perform(ActionContext<OrderModel> actionContext) {
      OrderModel order = actionContext.getData();

      final PaymentTransactionEntryModel paymentTransactionToRefund = getPaymentTransactionToRefund(order);
      final IngenicoConfigurationModel ingenicoConfiguration = getIngenicoConfiguration(order);

      RefundResponse refundResponse = ingenicoPaymentService.refundPayment(ingenicoConfiguration, paymentTransactionToRefund.getRequestId(), paymentTransactionToRefund.getPaymentTransaction().getPlannedAmount(), paymentTransactionToRefund.getCurrency().getIsocode());

      //result
      ActionResult<OrderModel> result = null;
      String resultMessage = null;
      ingenicoTransactionService.updatePaymentTransaction(paymentTransactionToRefund.getPaymentTransaction(),
            paymentTransactionToRefund.getRequestId(),
            refundResponse.getStatus(),
            refundResponse.getStatus(),
            refundResponse.getRefundOutput().getAmountOfMoney(),
            PaymentTransactionType.REFUND_STANDALONE);

      if (REFUND_REQUESTED.getValue().equals(refundResponse.getStatus())) {
         ingenicoBusinessProcessService.triggerOrderProcessEvent(order, INGENICO_EVENT_REFUND);
         result = new ActionResult<OrderModel>(ActionResult.SUCCESS, order);
         resultMessage = actionContext.getLabel("action.manualrefund.success");
      } else {
         result = new ActionResult<OrderModel>(ActionResult.ERROR, order);
         resultMessage = actionContext.getLabel("action.manualrefund.failure");
      }
      Messagebox.show(resultMessage + ", " + refundResponse.getStatus() + ", (" + result.getResultCode() + ")");

      return result;
   }

   @Override public boolean canPerform(ActionContext<OrderModel> ctx) {
      OrderModel order = ctx.getData();
      return order != null && INGENICO_CAPTURED.equals(order.getPaymentStatus());
   }

   private IngenicoConfigurationModel getIngenicoConfiguration(OrderModel orderModel) {
      final BaseStoreModel store = orderModel.getStore();
      return store != null ? store.getIngenicoConfiguration() : null;
   }

   private PaymentTransactionEntryModel getPaymentTransactionToRefund(final OrderModel order) {
      final PaymentTransactionModel finalPaymentTransaction = order.getPaymentTransactions().get(order.getPaymentTransactions().size() - 1);
      return finalPaymentTransaction.getEntries()
            .stream()
            .filter(entry -> PaymentTransactionType.AUTHORIZATION.equals(entry.getType()))
            .findFirst().orElse(null);
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
