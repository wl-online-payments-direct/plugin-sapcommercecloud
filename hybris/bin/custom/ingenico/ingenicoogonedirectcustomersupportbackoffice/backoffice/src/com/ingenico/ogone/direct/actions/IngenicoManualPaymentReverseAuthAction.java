package com.ingenico.ogone.direct.actions;

import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.INGENICO_EVENT_CAPTURE;
import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_STATUS_ENUM.CANCELLED;
import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_STATUS_ENUM.CAPTURE_REQUESTED;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.hybris.cockpitng.actions.ActionContext;
import com.hybris.cockpitng.actions.ActionResult;
import com.hybris.cockpitng.actions.CockpitAction;
import com.ingenico.direct.domain.CancelPaymentResponse;
import com.ingenico.ogone.direct.model.IngenicoConfigurationModel;
import com.ingenico.ogone.direct.service.IngenicoBusinessProcessService;
import com.ingenico.ogone.direct.service.IngenicoPaymentService;
import com.ingenico.ogone.direct.service.IngenicoTransactionService;
import com.ingenico.ogone.direct.service.impl.IngenicoTransactionServiceImpl;
import de.hybris.platform.basecommerce.enums.CancelReason;
import de.hybris.platform.core.enums.OrderStatus;
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
import de.hybris.platform.store.BaseStoreModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zhtml.Messagebox;

public class IngenicoManualPaymentReverseAuthAction extends CancelOrderAction implements CockpitAction<OrderModel, OrderModel> {
   private static final Logger LOGGER = LoggerFactory.getLogger(IngenicoTransactionServiceImpl.class);

   @Resource
   private IngenicoPaymentService ingenicoPaymentService;

   @Resource
   private IngenicoTransactionService ingenicoTransactionService;

   @Resource
   private OrderCancelService orderCancelService;

   @Resource
   private ModelService modelService;

   @Resource
   private IngenicoBusinessProcessService ingenicoBusinessProcessService;

   @Override public ActionResult<OrderModel> perform(ActionContext<OrderModel> actionContext) {
      OrderModel order = actionContext.getData();

      final PaymentTransactionEntryModel paymentTransactionToCancel = getPaymentTransactionToCancel(order);
      final IngenicoConfigurationModel ingenicoConfiguration = getIngenicoConfiguration(order);

      CancelPaymentResponse cancelPaymentResponse = ingenicoPaymentService.cancelPayment(ingenicoConfiguration, paymentTransactionToCancel.getRequestId());

      ActionResult<OrderModel> result = null;
      String resultMessage = null;

      if (CANCELLED.getValue().equals(cancelPaymentResponse.getPayment().getStatus())) {
         try {
            ingenicoTransactionService.updatePaymentTransaction(paymentTransactionToCancel.getPaymentTransaction(),
                  paymentTransactionToCancel.getRequestId(),
                  cancelPaymentResponse.getPayment().getStatus(),
                  cancelPaymentResponse.getPayment().getStatus(),
                  cancelPaymentResponse.getPayment().getPaymentOutput().getAmountOfMoney(),
                  PaymentTransactionType.CANCEL);

            final OrderCancelRequest orderCancelRequest = new OrderCancelRequest(order,
                  Lists.newArrayList(createCancellationEntries(order)));
            orderCancelService.requestOrderCancel(orderCancelRequest, getUserService().getCurrentUser());
            result = new ActionResult<OrderModel>(ActionResult.SUCCESS, order);
            resultMessage = actionContext.getLabel("action.manualpaymentcancelation.success");
         } catch (OrderCancelException ex) {
            LOGGER.error(ex.getMessage());
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
      boolean isTransactionAuthorizedNotCaptured = false;

      List<PaymentTransactionEntryModel> paymentTransactionEntryModels = order.getPaymentTransactions().get(order.getPaymentTransactions().size() - 1).getEntries(); // take the last one - it will be the most recent one
      for (PaymentTransactionEntryModel paymentTransactionEntryModel : paymentTransactionEntryModels) {

         switch (paymentTransactionEntryModel.getType()) {
            case AUTHORIZATION:
               isTransactionAuthorizedNotCaptured = "SUCCESSFUL".equals(paymentTransactionEntryModel.getTransactionStatus()); // TODO change this status to PENDING_CAPTURE when the code is ready use PAYMENT_STATUS_ENUM.PENDING_CAPTURE
               break;
            case CAPTURE:
               isTransactionAuthorizedNotCaptured = !"SUCCESSFUL".equals(paymentTransactionEntryModel.getTransactionStatus());
               break;
            case CANCEL:
               isTransactionAuthorizedNotCaptured = !"REJECTED".equals(paymentTransactionEntryModel.getTransactionStatus());
               break;
            default: //
               break;
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

   public void setIngenicoPaymentService(IngenicoPaymentService ingenicoPaymentService) {
      this.ingenicoPaymentService = ingenicoPaymentService;
   }

   public void setIngenicoTransactionService(IngenicoTransactionService ingenicoTransactionService) {
      this.ingenicoTransactionService = ingenicoTransactionService;
   }

   public void setIngenicoBusinessProcessService(IngenicoBusinessProcessService ingenicoBusinessProcessService) {
      this.ingenicoBusinessProcessService = ingenicoBusinessProcessService;
   }

   public void setOrderCancelService(OrderCancelService orderCancelService) {
      this.orderCancelService = orderCancelService;
   }

   public void setModelService(ModelService modelService) {
      this.modelService = modelService;
   }
}
