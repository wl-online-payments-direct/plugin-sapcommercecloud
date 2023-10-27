package com.worldline.direct.actions;

import com.google.common.collect.Lists;
import com.hybris.cockpitng.actions.ActionContext;
import com.hybris.cockpitng.actions.ActionResult;
import com.hybris.cockpitng.actions.CockpitAction;
import com.onlinepayments.domain.CancelPaymentResponse;
import com.worldline.direct.service.WorldlinePaymentService;
import com.worldline.direct.service.WorldlineTransactionService;
import de.hybris.platform.basecommerce.enums.CancelReason;
import de.hybris.platform.core.enums.PaymentStatus;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import de.hybris.platform.core.model.order.OrderEntryModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.omsbackoffice.actions.order.cancel.CancelOrderAction;
import de.hybris.platform.ordercancel.OrderCancelEntry;
import de.hybris.platform.ordercancel.OrderCancelException;
import de.hybris.platform.ordercancel.OrderCancelRequest;
import de.hybris.platform.ordercancel.OrderCancelService;
import de.hybris.platform.payment.enums.PaymentTransactionType;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.payment.model.PaymentTransactionModel;
import de.hybris.platform.servicelayer.model.ModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zhtml.Messagebox;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.stream.Collectors;

import static com.worldline.direct.constants.WorldlinedirectcoreConstants.PAYMENT_STATUS_ENUM.CANCELLED;

public class WorldlineManualPaymentReverseAuthAction extends CancelOrderAction implements CockpitAction<OrderModel, OrderModel> {
   private static final Logger LOGGER = LoggerFactory.getLogger(WorldlineManualPaymentReverseAuthAction.class);

   @Resource
   private WorldlinePaymentService worldlinePaymentService;

   @Resource
   private WorldlineTransactionService worldlineTransactionService;

   @Resource
   private OrderCancelService orderCancelService;

   @Resource
   private ModelService modelService;

   @Override public ActionResult<OrderModel> perform(ActionContext<OrderModel> actionContext) {
      OrderModel order = actionContext.getData();

      final PaymentTransactionEntryModel paymentTransactionToCancel = getPaymentTransactionToCancel(order);

      CancelPaymentResponse cancelPaymentResponse = worldlinePaymentService.cancelPayment(order.getStore().getUid(), paymentTransactionToCancel.getRequestId());

      ActionResult<OrderModel> result = null;
      String resultMessage = null;

      if (CANCELLED.getValue().equals(cancelPaymentResponse.getPayment().getStatus())) {
         try {
            worldlineTransactionService.updatePaymentTransaction(paymentTransactionToCancel.getPaymentTransaction(),
                  paymentTransactionToCancel.getRequestId(),
                  cancelPaymentResponse.getPayment().getStatus(),
                  order.getStore().getWorldlineConfiguration().isApplySurcharge() ? cancelPaymentResponse.getPayment().getPaymentOutput().getAcquiredAmount() : cancelPaymentResponse.getPayment().getPaymentOutput().getAmountOfMoney(),
                  PaymentTransactionType.CANCEL);

            final OrderCancelRequest orderCancelRequest = new OrderCancelRequest(order,
                  Lists.newArrayList(createCancellationEntries(order)));
            orderCancelService.requestOrderCancel(orderCancelRequest, getUserService().getCurrentUser());
            order.setPaymentStatus(PaymentStatus.WORLDLINE_CANCELED);
            modelService.save(order);
            result = new ActionResult<OrderModel>(ActionResult.SUCCESS, order);
            resultMessage = actionContext.getLabel("action.manualpaymentcancelation.success");
         } catch (OrderCancelException ex) {
            LOGGER.error(ex.getMessage() + " " + ex.getStackTrace());
            result = new ActionResult<OrderModel>(ActionResult.ERROR, order);
            resultMessage = actionContext.getLabel(ex.getLocalizedMessage());
         }
      } else {
         result = new ActionResult<OrderModel>(ActionResult.ERROR, order);
         resultMessage = actionContext.getLabel("action.manualpaymentcancelation.error");
      }
      Messagebox.show(resultMessage + ", " + cancelPaymentResponse.getPayment().getStatus() + ", (" + result.getResultCode() + ")");

      return result;
   }

   @Override public boolean canPerform(ActionContext<OrderModel> ctx) {
      OrderModel order = ctx.getData();
      return order != null && PaymentStatus.WORLDLINE_AUTHORIZED.equals(order.getPaymentStatus());
   }

   private PaymentTransactionEntryModel getPaymentTransactionToCancel(final OrderModel order) {
      final PaymentTransactionModel finalPaymentTransaction = order.getPaymentTransactions().get(order.getPaymentTransactions().size() - 1);
      return finalPaymentTransaction.getEntries()
            .stream()
            .filter(entry -> PaymentTransactionType.AUTHORIZATION.equals(entry.getType()))
            .findFirst().orElse(null);
   }

   public Collection<OrderCancelEntry> createCancellationEntries(final OrderModel order)
   {
      return order.getEntries().stream().map(entry -> createCancellationEntry(entry)).collect(Collectors.toList());
   }

   protected OrderCancelEntry createCancellationEntry(final AbstractOrderEntryModel orderEntry)
   {
      final OrderCancelEntry entry = new OrderCancelEntry(orderEntry,
            ((OrderEntryModel) orderEntry).getQuantityPending().longValue());
      entry.setCancelReason(CancelReason.OTHER);
      return entry;
   }

}
