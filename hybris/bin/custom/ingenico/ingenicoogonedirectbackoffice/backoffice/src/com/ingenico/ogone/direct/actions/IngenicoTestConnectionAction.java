package com.ingenico.ogone.direct.actions;

import javax.annotation.Resource;

import com.hybris.cockpitng.actions.ActionContext;
import com.hybris.cockpitng.actions.ActionResult;
import com.hybris.cockpitng.actions.CockpitAction;
import com.ingenico.ogone.direct.service.IngenicoConfigurationService;

public class IngenicoTestConnectionAction implements CockpitAction {

   private final static String TEST_CONNECTION_OK_RESULT = "OK";

   @Resource
   private IngenicoConfigurationService ingenicoConfigurationService;

   @Override
   public ActionResult<String> perform(ActionContext actionContext) {
      String testConnectionResult = ingenicoConfigurationService.testConnection();

      if (testConnectionResult.equals(TEST_CONNECTION_OK_RESULT)) {
         return new ActionResult<String>(ActionResult.SUCCESS, TEST_CONNECTION_OK_RESULT);
      }
      return new ActionResult<String>(ActionResult.ERROR);
   }
}
