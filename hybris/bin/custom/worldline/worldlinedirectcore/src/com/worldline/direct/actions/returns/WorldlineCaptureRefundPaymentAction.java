package com.worldline.direct.actions.returns;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import de.hybris.platform.basecommerce.enums.ReturnStatus;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.payment.enums.PaymentTransactionType;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.payment.model.PaymentTransactionModel;
import de.hybris.platform.processengine.action.AbstractAction;
import de.hybris.platform.returns.model.ReturnProcessModel;
import de.hybris.platform.returns.model.ReturnRequestModel;
import de.hybris.platform.task.RetryLaterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorldlineCaptureRefundPaymentAction extends AbstractAction<ReturnProcessModel> {
   private static final Logger LOG = LoggerFactory.getLogger(WorldlineCaptureRefundPaymentAction.class);

   @Override public String execute(ReturnProcessModel returnProcessModel) throws RetryLaterException, Exception {
      LOG.debug("[WORLDLINE] Process: " + returnProcessModel.getCode() + " in step " + getClass().getSimpleName());

      final ReturnRequestModel returnRequest = returnProcessModel.getReturnRequest();
      final List<PaymentTransactionModel> transactions = returnRequest.getOrder().getPaymentTransactions();

      if (transactions.isEmpty()) {
         LOG.info("[WORLDLINE] Unable to refund for ReturnRequest {}, no PaymentTransactions found", returnRequest.getCode());
         setReturnRequestStatus(returnRequest, ReturnStatus.PAYMENT_REVERSAL_FAILED);
         return Transition.NOK.toString();
      }

      final PaymentTransactionModel transaction = transactions.get(0);

      if (!(returnRequest.getOrder().getPaymentInfo() instanceof WorldlinePaymentInfoModel)) {
         LOG.info("[WORLDLINE] Skip capture check!");
         return Transition.OK.toString();
      }

      List<PaymentTransactionEntryModel> paymentTransactionEntryModels = getPaymentTransactionToRefund(returnRequest.getOrder());
      if (paymentTransactionEntryModels.size() == 0) {
         return Transition.WAIT.toString();
      } else {
         PaymentTransactionEntryModel paymentTransactionEntryModel = paymentTransactionEntryModels.get(paymentTransactionEntryModels.size() - 1);
         if (paymentTransactionEntryModel.getTransactionStatusDetails().equals(WorldlinedirectcoreConstants.PAYMENT_STATUS_ENUM.REFUND_REQUESTED.getValue()) ||
               paymentTransactionEntryModel.getTransactionStatusDetails().equals(WorldlinedirectcoreConstants.PAYMENT_STATUS_ENUM.REFUNDED.getValue())) {
            setReturnRequestStatus(returnRequest, ReturnStatus.PAYMENT_REVERSED);

            //TODO update order
            return Transition.OK.toString();
         } else {
            setReturnRequestStatus(returnRequest, ReturnStatus.PAYMENT_REVERSAL_FAILED);
            return Transition.NOK.toString();
         }
      }
   }


   enum Transition {
      OK, NOK, WAIT;

      public static Set<String> getStringValues() {
         Set<String> res = new HashSet<>();
         for (final WorldlineCaptureRefundPaymentAction.Transition transitions : WorldlineCaptureRefundPaymentAction.Transition.values()) {
            res.add(transitions.toString());
         }
         return res;
      }
   }
   @Override public Set<String> getTransitions() {
      return WorldlineCaptureRefundPaymentAction.Transition.getStringValues();
   }

   protected void setReturnRequestStatus(final ReturnRequestModel returnRequest, final ReturnStatus status) {
      returnRequest.setStatus(status);
      returnRequest.getReturnEntries().stream().forEach(entry -> {
         entry.setStatus(status);
         getModelService().save(entry);
      });
      getModelService().save(returnRequest);
   }

   private List<PaymentTransactionEntryModel> getPaymentTransactionToRefund(final OrderModel order) {
      final PaymentTransactionModel finalPaymentTransaction = order.getPaymentTransactions().get(order.getPaymentTransactions().size() - 1);
      return finalPaymentTransaction.getEntries()
            .stream()
            .filter(entry -> PaymentTransactionType.REFUND_FOLLOW_ON.equals(entry.getType()))
            .collect(Collectors.toList());
   }
}
