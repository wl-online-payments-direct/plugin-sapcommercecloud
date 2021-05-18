package com.ingenico.ogone.direct.actions;

import javax.annotation.Resource;

import com.hybris.cockpitng.actions.ActionContext;
import com.hybris.cockpitng.actions.ActionResult;
import com.hybris.cockpitng.actions.CockpitAction;
import com.ingenico.ogone.direct.model.IngenicoConfigurationModel;
import com.ingenico.ogone.direct.service.IngenicoServiceMethodsService;
import org.zkoss.zhtml.Messagebox;

public class IngenicoTestConnectionAction implements CockpitAction {

   private final static String TEST_CONNECTION_OK_RESULT = "OK";

   @Resource
   private IngenicoServiceMethodsService ingenicoServiceMethodsService;

   @Override
   public ActionResult<String> perform(ActionContext actionContext) {
      IngenicoConfigurationModel ingenicoConfigurationModel = (IngenicoConfigurationModel) actionContext.getData();
      String testConnectionResult = ingenicoServiceMethodsService.testConnection(ingenicoConfigurationModel);

      ActionResult<String> result = null;
      if (testConnectionResult.equals(TEST_CONNECTION_OK_RESULT)) {
         result = new ActionResult<String>(ActionResult.SUCCESS, TEST_CONNECTION_OK_RESULT);
      } else {
         result = new ActionResult<String>(ActionResult.ERROR);
      }
      Messagebox.show(result.getData() + " (" + result.getResultCode() + ")");
      return result;
   }
}
