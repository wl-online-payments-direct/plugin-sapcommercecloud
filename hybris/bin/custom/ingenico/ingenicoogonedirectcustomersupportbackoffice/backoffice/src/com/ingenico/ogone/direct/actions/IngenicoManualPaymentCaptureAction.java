package com.ingenico.ogone.direct.actions;

import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.INGENICO_EVENT_CAPTURE;

import javax.annotation.Resource;
import java.util.List;

import com.hybris.cockpitng.actions.ActionContext;
import com.hybris.cockpitng.actions.ActionResult;
import com.hybris.cockpitng.actions.CockpitAction;
import com.ingenico.direct.domain.CaptureResponse;
import com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants;
import com.ingenico.ogone.direct.service.IngenicoBusinessProcessService;
import com.ingenico.ogone.direct.service.IngenicoPaymentService;
import com.ingenico.ogone.direct.service.IngenicoTransactionService;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.omsbackoffice.actions.order.ManualPaymentCaptureAction;
import de.hybris.platform.payment.enums.PaymentTransactionType;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.payment.model.PaymentTransactionModel;
import org.zkoss.zhtml.Messagebox;

public class IngenicoManualPaymentCaptureAction extends ManualPaymentCaptureAction implements CockpitAction<OrderModel, OrderModel> {

   @Resource
   private IngenicoPaymentService ingenicoPaymentService;

   @Resource
   private IngenicoTransactionService ingenicoTransactionService;

   @Resource
   private IngenicoBusinessProcessService ingenicoBusinessProcessService;

   @Override
   public ActionResult<OrderModel> perform(ActionContext<OrderModel> actionContext) {
      OrderModel order = actionContext.getData();

      CaptureResponse captureResponse = ingenicoPaymentService.capturePayment(order.getStore().getIngenicoConfiguration(), order.getPaymentTransactions().get(order.getPaymentTransactions().size() - 1).getCode());

      //result
      ActionResult<OrderModel> result = null;
      Object[] resultMessage = null;
      if (IngenicoogonedirectcoreConstants.PAYMENT_STATUS_ENUM.CAPTURE_REQUESTED.equals(captureResponse.getStatus())) {
         PaymentTransactionModel paymentTransactionModel = order.getPaymentTransactions().get(order.getPaymentTransactions().size() - 1);
         ingenicoTransactionService.updatePaymentTransaction(paymentTransactionModel, captureResponse.getStatus(), captureResponse.getStatus(), captureResponse.getCaptureOutput().getAmountOfMoney(), PaymentTransactionType.CAPTURE);
         ingenicoBusinessProcessService.triggerOrderProcessEvent(order, INGENICO_EVENT_CAPTURE);
         result = new ActionResult<OrderModel>(ActionResult.SUCCESS, order);
         resultMessage = new Object[]{actionContext.getLabel("action.manualpaymentcapture.success")};
      } else {
         result = new ActionResult<OrderModel>(ActionResult.ERROR, order);
         resultMessage = new Object[]{actionContext.getLabel("action.manualpaymentcapture.error")};
      }
      Messagebox.show(resultMessage + ", " + captureResponse.getStatus() + ", (" + result.getResultCode() + ")");

      return result;
   }

   @Override
   public boolean canPerform(ActionContext<OrderModel> ctx) {
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
