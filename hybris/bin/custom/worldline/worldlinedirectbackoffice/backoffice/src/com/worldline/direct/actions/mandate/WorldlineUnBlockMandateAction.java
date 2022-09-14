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
import org.zkoss.zhtml.Messagebox;

import javax.annotation.Resource;

public class WorldlineUnBlockMandateAction implements CockpitAction<WorldlineMandateModel, Object> {

    @Resource
    private ObjectFacade objectFacade;
    @Resource
    private WorldlinePaymentService worldlinePaymentService;
    @Resource
    private WorldlineRecurringService worldlineRecurringService;

    @Override
    public ActionResult<Object> perform(ActionContext<WorldlineMandateModel> actionContext) {
        WorldlineMandateModel worldlineMandateModel = actionContext.getData();
        ActionResult<Object> result;
        worldlineRecurringService.updateMandate(worldlineMandateModel);
        try {
            objectFacade.reload(worldlineMandateModel);
            if (WorldlineRecurringPaymentStatus.BLOCKED.equals(worldlineMandateModel.getStatus())) {
                GetMandateResponse blockMandateResponse = worldlinePaymentService.unBlockMandate(worldlineMandateModel);
                if (blockMandateResponse != null && "ACTIVE".equals(blockMandateResponse.getMandate().getStatus())) {
                    worldlineMandateModel.setStatus(WorldlineRecurringPaymentStatus.ACTIVE);
                    objectFacade.save(worldlineMandateModel);
                    result = new ActionResult<>(ActionResult.SUCCESS, "Mandate was unBlocked successfully");
                } else {
                    result = new ActionResult<>(ActionResult.ERROR, "Mandate was not unBlocked successfully");
                }
            } else if (WorldlineRecurringPaymentStatus.ACTIVE.equals(worldlineMandateModel.getStatus())) {
                result = new ActionResult<>(ActionResult.ERROR, "Mandate was already activated");
            } else {
                result = new ActionResult<>(ActionResult.ERROR, "Cannot unBlocked a mandate with status : " + worldlineMandateModel.getStatus());
            }
        } catch (ObjectSavingException e) {
            result = new ActionResult<>(ActionResult.ERROR, "Mandate was unblocked in Worldline , but could not be saved successfully");
        } catch (ObjectNotFoundException e) {
            result = new ActionResult<>(ActionResult.ERROR, "something went wrong while reloading Object");
        }

        Messagebox.show(result.getData() + " (" + result.getResultCode() + ")");
        return result;
    }

    @Override
    public boolean canPerform(ActionContext<WorldlineMandateModel> ctx) {
        WorldlineMandateModel data = ctx.getData();
        return WorldlineRecurringPaymentStatus.BLOCKED.equals(data.getStatus());
    }
}
