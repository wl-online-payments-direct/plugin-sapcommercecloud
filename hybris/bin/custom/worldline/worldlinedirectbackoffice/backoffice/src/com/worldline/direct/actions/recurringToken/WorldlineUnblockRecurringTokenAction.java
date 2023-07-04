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

public class WorldlineUnblockRecurringTokenAction implements CockpitAction<WorldlineRecurringTokenModel, Object> {

   @Resource
   private ObjectFacade objectFacade;

   @Override
   public ActionResult<Object> perform(ActionContext<WorldlineRecurringTokenModel> actionContext) {
      WorldlineRecurringTokenModel recurringTokenModel = actionContext.getData();
      ActionResult<Object> result;

      try {
         if (WorldlineRecurringPaymentStatus.BLOCKED.equals(recurringTokenModel.getStatus())) {
            recurringTokenModel.setStatus(WorldlineRecurringPaymentStatus.ACTIVE);
            objectFacade.save(recurringTokenModel);

            result = new ActionResult<>(ActionResult.SUCCESS, "Token was unblocked successfully");
         } else {
            result = new ActionResult<>(ActionResult.ERROR, "Cannot unblock a token with status : " + recurringTokenModel.getStatus());
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
      return WorldlineRecurringPaymentStatus.BLOCKED.equals(data.getStatus());
   }
}
