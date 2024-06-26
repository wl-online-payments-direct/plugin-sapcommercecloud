package com.worldline.direct.actions;

import com.hybris.cockpitng.actions.ActionContext;
import com.hybris.cockpitng.actions.ActionResult;
import com.hybris.cockpitng.actions.CockpitAction;
import com.onlinepayments.domain.RefundResponse;
import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import com.worldline.direct.service.WorldlineBusinessProcessService;
import com.worldline.direct.service.WorldlinePaymentService;
import com.worldline.direct.service.WorldlineTransactionService;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.omsbackoffice.actions.returns.ManualRefundAction;
import de.hybris.platform.payment.enums.PaymentTransactionType;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.payment.model.PaymentTransactionModel;
import de.hybris.platform.returns.model.ReturnRequestModel;
import org.zkoss.zhtml.Messagebox;

import javax.annotation.Resource;
import java.math.BigDecimal;

import static com.worldline.direct.constants.WorldlinedirectcoreConstants.PAYMENT_STATUS_ENUM.REFUNDED;
import static com.worldline.direct.constants.WorldlinedirectcoreConstants.PAYMENT_STATUS_ENUM.REFUND_REQUESTED;
import static com.worldline.direct.constants.WorldlinedirectcoreConstants.WORLDLINE_EVENT_REFUND;

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
      //result
      ActionResult<ReturnRequestModel> result = null;
      String resultMessage = null;

      BigDecimal refundAmount = calculateRefundAmount(returnRequestModel, order.getStore().getUid(), paymentTransactionToRefund.getPaymentTransaction().getWorldlineRawTransactionCode(), paymentTransactionToRefund.getPaymentTransaction().getPlannedAmount(), paymentTransactionToRefund.getCurrency().getIsocode());
      if (refundAmount.compareTo(BigDecimal.ZERO) == 0) {
         result = new ActionResult<ReturnRequestModel>(ActionResult.ERROR, returnRequestModel);
         resultMessage = actionContext.getLabel("action.manualrefund.failure");
         Messagebox.show(resultMessage + ", (" + result.getResultCode() + ")");

         return result;
      }

      RefundResponse refundResponse = worldlinePaymentService.refundPayment(order.getStore().getUid(), paymentTransactionToRefund.getRequestId(), refundAmount, paymentTransactionToRefund.getCurrency().getIsocode());


      if (REFUND_REQUESTED.getValue().equals(refundResponse.getStatus()) || REFUNDED.getValue().equals(refundResponse.getStatus())) {
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

   private BigDecimal calculateRefundAmount(ReturnRequestModel returnRequestModel, String storeId, String paymentId, BigDecimal plannedAmount, String currencyISOcode) {
     Long nonCapturedAmount = worldlinePaymentService.getNonCapturedAmount(storeId, paymentId, plannedAmount, currencyISOcode);
     BigDecimal capturedAmount = plannedAmount.subtract(new BigDecimal(nonCapturedAmount));
     BigDecimal refundAmount = returnRequestModel.getSubtotal();
     if (returnRequestModel.getRefundDeliveryCost()) {
        BigDecimal deliveryCost = new BigDecimal(returnRequestModel.getOrder().getDeliveryCost());
        refundAmount = returnRequestModel.getSubtotal().add(deliveryCost.setScale(refundAmount.scale(), BigDecimal.ROUND_HALF_EVEN));
     }
     if (capturedAmount.compareTo(refundAmount) == 1
           || capturedAmount.compareTo(refundAmount) == 0) { // we have fully captured order or captured amount is greater than refund amount
        return refundAmount;
     } else { // we have partly captured order and the return amount is gr than captured amount
        return new BigDecimal(0);
     }
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
