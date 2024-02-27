package com.worldline.direct.actions.mandate;

import com.hybris.cockpitng.actions.ActionContext;
import com.hybris.cockpitng.actions.ActionResult;
import com.hybris.cockpitng.actions.CockpitAction;
import com.hybris.cockpitng.dataaccess.facades.object.ObjectFacade;
import com.hybris.cockpitng.dataaccess.facades.object.exceptions.ObjectNotFoundException;
import com.hybris.cockpitng.dataaccess.facades.object.exceptions.ObjectSavingException;
import com.onlinepayments.domain.GetMandateResponse;
import com.worldline.direct.enums.WorldlineRecurringPaymentStatus;
import com.worldline.direct.model.WorldlineMandateModel;
import com.worldline.direct.service.WorldlinePaymentService;
import com.worldline.direct.service.WorldlineRecurringService;
import de.hybris.platform.b2bacceleratorservices.customer.B2BCustomerAccountService;
import de.hybris.platform.orderscheduling.model.CartToOrderCronJobModel;

import javax.annotation.Resource;

import static org.zkoss.zul.Messagebox.show;

public class WorldlineRevokeMandateAction implements CockpitAction<WorldlineMandateModel, Object> {

   @Resource
   private ObjectFacade objectFacade;
   @Resource
   private WorldlinePaymentService worldlinePaymentService;
   @Resource
   private WorldlineRecurringService worldlineRecurringService;

   @Resource
   private B2BCustomerAccountService b2BCustomerAccountService;

   @Override
   public ActionResult<Object> perform(ActionContext<WorldlineMandateModel> actionContext) {
      WorldlineMandateModel worldlineMandateModel = actionContext.getData();
      ActionResult<Object> result;
      worldlineRecurringService.updateMandate(worldlineMandateModel);

      try {
         objectFacade.reload(worldlineMandateModel);
         if (!WorldlineRecurringPaymentStatus.REVOKED.equals(worldlineMandateModel.getStatus())) {
            switch (worldlineMandateModel.getRecurrenceType()) {
               case RECURRING:
                  CartToOrderCronJobModel
                        cronjob = b2BCustomerAccountService.getCartToOrderCronJobForCode(worldlineMandateModel.getCustomerReference().split("_")[0], worldlineMandateModel.getCustomer());

                  worldlineRecurringService.cancelRecurringPayment(cronjob);
                  cronjob.setActive(Boolean.FALSE);
                  objectFacade.save(cronjob);

                  result = new ActionResult<>(ActionResult.SUCCESS, "Mandate was revoked successfully");
                  break;
               case UNIQUE:
               default:
                  GetMandateResponse revokeMandateResponse = worldlinePaymentService.revokeMandate(worldlineMandateModel);
                  if (revokeMandateResponse != null && "REVOKED".equals(revokeMandateResponse.getMandate().getStatus())) {
                     worldlineMandateModel.setStatus(WorldlineRecurringPaymentStatus.REVOKED);

                     objectFacade.save(worldlineMandateModel);

                     result = new ActionResult<>(ActionResult.SUCCESS, "Mandate was revoked successfully");
                  } else {

                     result = new ActionResult<>(ActionResult.ERROR, "Mandate was not revoked successfully");
                  }
                  break;
            }
         } else {
            result = new ActionResult<>(ActionResult.ERROR, "cannot revoke a mandate with status : " + worldlineMandateModel.getStatus());
         }
      } catch (ObjectSavingException e) {
         result = new ActionResult<>(ActionResult.ERROR, "Mandate was blocked in Worldline , but could not be saved successfully");
      } catch (ObjectNotFoundException e) {
         result = new ActionResult<>(ActionResult.ERROR, "something went wrong while reloading Object");
      }

      show(result.getData() + " (" + result.getResultCode() + ")");
      return result;
   }

   @Override
   public boolean canPerform(ActionContext<WorldlineMandateModel> ctx) {
      WorldlineMandateModel data = ctx.getData();

      if (data == null) {
         return Boolean.FALSE;
      }
      return !WorldlineRecurringPaymentStatus.REVOKED.equals(data.getStatus());
   }
}
