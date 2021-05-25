package com.ingenico.ogone.direct.actions;

import com.hybris.cockpitng.actions.ActionContext;
import com.hybris.cockpitng.actions.ActionResult;
import com.hybris.cockpitng.actions.CockpitAction;
import de.hybris.platform.core.model.order.OrderModel;

public class IngenicoManualPaymentCaptureAction implements CockpitAction<OrderModel, Object> {

   @Override
   public ActionResult<Object> perform(ActionContext<OrderModel> actionContext) {
      OrderModel order = actionContext.getData();
      return null;
   }

   @Override
   public boolean canPerform(ActionContext<OrderModel> ctx) {

      OrderModel order = ctx.getData();
      order.getPaymentTransactions();
      return false;

   }
}
