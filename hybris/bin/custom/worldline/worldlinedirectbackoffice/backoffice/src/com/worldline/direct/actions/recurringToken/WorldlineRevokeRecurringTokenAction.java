package com.worldline.direct.actions.recurringToken;

import com.hybris.cockpitng.actions.ActionContext;
import com.hybris.cockpitng.actions.ActionResult;
import com.hybris.cockpitng.actions.CockpitAction;
import com.hybris.cockpitng.dataaccess.facades.object.ObjectFacade;
import com.hybris.cockpitng.dataaccess.facades.object.exceptions.ObjectSavingException;
import com.worldline.direct.enums.WorldlineRecurringPaymentStatus;
import com.worldline.direct.facade.WorldlineCustomerAccountFacade;
import com.worldline.direct.model.WorldlineRecurringTokenModel;
import com.worldline.direct.service.WorldlineCustomerAccountService;
import com.worldline.direct.service.WorldlineRecurringService;
import de.hybris.platform.b2bacceleratorfacades.order.data.ScheduledCartData;
import de.hybris.platform.b2bacceleratorservices.customer.B2BCustomerAccountService;
import de.hybris.platform.cronjob.model.CronJobModel;
import de.hybris.platform.orderscheduling.model.CartToOrderCronJobModel;
import de.hybris.platform.servicelayer.model.ModelService;

import javax.annotation.Resource;

import static org.zkoss.zul.Messagebox.show;

public class WorldlineRevokeRecurringTokenAction implements CockpitAction<WorldlineRecurringTokenModel, Object> {

   @Resource
   private ObjectFacade objectFacade;

   @Resource
   private WorldlineRecurringService worldlineRecurringService;

   @Resource
   private B2BCustomerAccountService b2BCustomerAccountService;

   @Override
   public ActionResult<Object> perform(ActionContext<WorldlineRecurringTokenModel> actionContext) {
      WorldlineRecurringTokenModel recurringTokenModel = actionContext.getData();
      CartToOrderCronJobModel cronjob = b2BCustomerAccountService.getCartToOrderCronJobForCode(recurringTokenModel.getSubscriptionID(), recurringTokenModel.getCustomer());
      ActionResult<Object> result;

      try {
            worldlineRecurringService.cancelRecurringPayment(cronjob);
            cronjob.setActive(Boolean.FALSE);
            objectFacade.save(cronjob);

            result = new ActionResult<>(ActionResult.SUCCESS, "Token was blocked successfully");

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
      return !WorldlineRecurringPaymentStatus.REVOKED.equals(data.getStatus());
   }

}
