package com.worldline.direct.actions;

import static com.worldline.direct.constants.WorldlinedirectcoreConstants.WORLDLINE_EVENT_REFUND;
import static com.worldline.direct.constants.WorldlinedirectcoreConstants.PAYMENT_STATUS_ENUM.REFUND_REQUESTED;

import javax.annotation.Resource;

import java.math.BigDecimal;

import com.hybris.cockpitng.actions.ActionContext;
import com.hybris.cockpitng.actions.ActionResult;
import com.hybris.cockpitng.actions.CockpitAction;
import com.ingenico.direct.domain.RefundResponse;
import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import com.worldline.direct.model.WorldlineConfigurationModel;
import com.worldline.direct.service.WorldlineBusinessProcessService;
import com.worldline.direct.service.WorldlinePaymentService;
import com.worldline.direct.service.WorldlineTransactionService;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.omsbackoffice.actions.returns.ManualRefundAction;
import de.hybris.platform.payment.model.PaymentTransactionModel;
import de.hybris.platform.payment.enums.PaymentTransactionType;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.returns.model.ReturnRequestModel;
import de.hybris.platform.store.BaseStoreModel;
import org.zkoss.zhtml.Messagebox;

public class WorldlineManualPaymentRefundAction extends ManualRefundAction implements CockpitAction<ReturnRequestModel, ReturnRequestModel> {

   @Resource
   private WorldlinePaymentService worldlinePaymentService;

   @Resource
   private WorldlineTransactionService worldlineTransactionService;

   @Resource
   private WorldlineBusinessProcessService worldlineBusinessProcessService;

   @Override public ActionResult<ReturnRequestModel> perform(ActionContext<ReturnRequestModel> actionContext) {
      ReturnRequestModel returnRequestModel = actionContext.getData();
      OrderModel order = returnRequestModel.getOrder();

      final PaymentTransactionEntryModel paymentTransactionToRefund = getPaymentTransactionToRefund(order);
      final WorldlineConfigurationModel worldlineConfiguration = getWorldlineConfiguration(order);

      //result
      ActionResult<ReturnRequestModel> result = null;
      String resultMessage = null;

      BigDecimal refundAmount = calculateRefundAmount(returnRequestModel, worldlineConfiguration, paymentTransactionToRefund.getRequestId(), paymentTransactionToRefund.getPaymentTransaction().getPlannedAmount(), paymentTransactionToRefund.getCurrency().getIsocode());
      if (refundAmount.compareTo(BigDecimal.ZERO) == 0) {
         result = new ActionResult<ReturnRequestModel>(ActionResult.ERROR, returnRequestModel);
         resultMessage = actionContext.getLabel("action.manualrefund.failure");
         Messagebox.show(resultMessage + ", (" + result.getResultCode() + ")");

         return result;
      }

      RefundResponse refundResponse = worldlinePaymentService.refundPayment(worldlineConfiguration, paymentTransactionToRefund.getRequestId(), refundAmount, paymentTransactionToRefund.getCurrency().getIsocode());


      if (REFUND_REQUESTED.getValue().equals(refundResponse.getStatus())) {
         worldlineTransactionService.updatePaymentTransaction(paymentTransactionToRefund.getPaymentTransaction(),
               paymentTransactionToRefund.getRequestId(),
               refundResponse.getStatus(),
               refundResponse.getRefundOutput().getAmountOfMoney(),
               PaymentTransactionType.REFUND_FOLLOW_ON);
         worldlineBusinessProcessService.triggerReturnProcessEvent(order, WORLDLINE_EVENT_REFUND);
         result = new ActionResult<ReturnRequestModel>(ActionResult.SUCCESS, returnRequestModel);
         resultMessage = actionContext.getLabel("action.manualrefund.success");
      } else {
         result = new ActionResult<ReturnRequestModel>(ActionResult.ERROR, returnRequestModel);
         resultMessage = actionContext.getLabel("action.manualrefund.failure");
      }
      Messagebox.show(resultMessage + ", " + refundResponse.getStatus() + ", (" + result.getResultCode() + ")");

      return result;
   }

   private BigDecimal calculateRefundAmount(ReturnRequestModel returnRequestModel, WorldlineConfigurationModel worldlineConfigurationModel, String paymentId, BigDecimal plannedAmount, String currencyISOcode) {
     Long nonCapturedAmount = worldlinePaymentService.getNonCapturedAmount(worldlineConfigurationModel, paymentId, plannedAmount, currencyISOcode);
     BigDecimal capturedAmount = plannedAmount.subtract(new BigDecimal(nonCapturedAmount));
     if (capturedAmount.compareTo(returnRequestModel.getSubtotal()) == 1
           || capturedAmount.compareTo(returnRequestModel.getSubtotal()) == 0) { // we have fully captured order or captured amount is greater than refund amount
        return returnRequestModel.getSubtotal();
     } else { // we have partly captured order and the return amount is gr than captured amount
        return new BigDecimal(0);
     }
   }

   private WorldlineConfigurationModel getWorldlineConfiguration(OrderModel orderModel) {
      final BaseStoreModel store = orderModel.getStore();
      return store != null ? store.getWorldlineConfiguration() : null;
   }

   private PaymentTransactionEntryModel getPaymentTransactionToRefund(final OrderModel order) {
      final PaymentTransactionModel finalPaymentTransaction = order.getPaymentTransactions().get(order.getPaymentTransactions().size() - 1);
      return finalPaymentTransaction.getEntries()
            .stream()
            .filter(entry -> PaymentTransactionType.CAPTURE.equals(entry.getType()))
            .filter(entry -> WorldlinedirectcoreConstants.PAYMENT_STATUS_CATEGORY_ENUM.SUCCESSFUL.getValue().equals(entry.getTransactionStatus()))
            .findFirst().orElse(null);
   }
}
