package com.worldline.direct.actions.recurringToken;

import com.hybris.cockpitng.actions.ActionContext;
import com.hybris.cockpitng.actions.ActionResult;
import com.hybris.cockpitng.actions.CockpitAction;
import com.hybris.cockpitng.dataaccess.facades.object.ObjectFacade;
import com.hybris.cockpitng.dataaccess.facades.object.exceptions.ObjectSavingException;
import com.worldline.direct.enums.WorldlineRecurringPaymentStatus;
import com.worldline.direct.model.WorldlineRecurringTokenModel;

import javax.annotation.Resource;

import static org.zkoss.zul.Messagebox.show;

public class WorldlineBlockRecurringTokenAction implements CockpitAction<WorldlineRecurringTokenModel, Object> {

   @Resource
   private ObjectFacade objectFacade;
   @Override
   public ActionResult<Object> perform(ActionContext<WorldlineRecurringTokenModel> actionContext) {
      WorldlineRecurringTokenModel recurringTokenModel = actionContext.getData();
      ActionResult<Object> result;

      try {
         if (WorldlineRecurringPaymentStatus.ACTIVE.equals(recurringTokenModel.getStatus())) {
            recurringTokenModel.setStatus(WorldlineRecurringPaymentStatus.BLOCKED);
            objectFacade.save(recurringTokenModel);

            result = new ActionResult<>(ActionResult.SUCCESS, "Token was blocked successfully");
         } else {
            result = new ActionResult<>(ActionResult.ERROR, "Cannot block a token with status : " + recurringTokenModel.getStatus());
         }
      } catch (ObjectSavingException e) {
         result = new ActionResult<>(ActionResult.ERROR, "Token could not be saved successfully");
      }

      show(result.getData() + " (" + result.getResultCode() + ")");
      return result;
   }

   @Override
   public boolean canPerform(ActionContext<WorldlineRecurringTokenModel> ctx) {
      WorldlineRecurringTokenModel data = ctx.getData();
      if (data == null) {
         return Boolean.FALSE;
      }
      return WorldlineRecurringPaymentStatus.ACTIVE.equals(data.getStatus());
   }
}
