package com.worldline.direct.actions;

import javax.annotation.Resource;

import com.hybris.cockpitng.actions.ActionContext;
import com.hybris.cockpitng.actions.ActionResult;
import com.hybris.cockpitng.actions.CockpitAction;
import com.worldline.direct.model.WorldlineConfigurationModel;
import com.worldline.direct.service.WorldlineServiceMethodsService;
import org.zkoss.zhtml.Messagebox;

public class WorldlineTestConnectionAction implements CockpitAction<WorldlineConfigurationModel, Object> {

   private final static String TEST_CONNECTION_OK_RESULT = "OK";

   @Resource
   private WorldlineServiceMethodsService worldlineServiceMethodsService;

   @Override
   public ActionResult<Object> perform(ActionContext<WorldlineConfigurationModel> actionContext) {
      WorldlineConfigurationModel worldlineConfigurationModel = actionContext.getData();
      String testConnectionResult = worldlineServiceMethodsService.testConnection(worldlineConfigurationModel);

      ActionResult<Object> result;
      if (TEST_CONNECTION_OK_RESULT.equals(testConnectionResult)) {
         result = new ActionResult<>(ActionResult.SUCCESS, TEST_CONNECTION_OK_RESULT);
      } else {
         result = new ActionResult<>(ActionResult.ERROR, testConnectionResult);
      }
      Messagebox.show(result.getData() + " (" + result.getResultCode() + ")");
      return result;
   }
}
