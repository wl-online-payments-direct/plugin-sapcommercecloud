package com.ingenico.ogone.direct.actions;

import javax.annotation.Resource;

import com.hybris.cockpitng.actions.ActionContext;
import com.hybris.cockpitng.actions.ActionResult;
import com.hybris.cockpitng.actions.CockpitAction;
import com.ingenico.ogone.direct.model.IngenicoConfigurationModel;
import com.ingenico.ogone.direct.service.IngenicoServiceMethodsService;
import org.zkoss.zhtml.Messagebox;

public class IngenicoTestConnectionAction implements CockpitAction<IngenicoConfigurationModel, Object> {

   private final static String TEST_CONNECTION_OK_RESULT = "OK";

   @Resource
   private IngenicoServiceMethodsService ingenicoServiceMethodsService;

   @Override
   public ActionResult<Object> perform(ActionContext<IngenicoConfigurationModel> actionContext) {
      IngenicoConfigurationModel ingenicoConfigurationModel = actionContext.getData();
      String testConnectionResult = ingenicoServiceMethodsService.testConnection(ingenicoConfigurationModel);

      ActionResult<Object> result = null;
      if (TEST_CONNECTION_OK_RESULT.equals(testConnectionResult)) {
         result = new ActionResult<Object>(ActionResult.SUCCESS, TEST_CONNECTION_OK_RESULT);
      } else {
         result = new ActionResult<Object>(ActionResult.ERROR, testConnectionResult);
      }
      Messagebox.show(result.getData() + " (" + result.getResultCode() + ")");
      return result;
   }
}
