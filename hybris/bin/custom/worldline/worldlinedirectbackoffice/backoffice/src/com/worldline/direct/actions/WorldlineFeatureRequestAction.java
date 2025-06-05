package com.worldline.direct.actions;

import com.hybris.cockpitng.actions.ActionContext;
import com.hybris.cockpitng.actions.ActionResult;
import com.hybris.cockpitng.actions.CockpitAction;
import com.hybris.cockpitng.engine.impl.AbstractComponentWidgetAdapterAware;
import com.worldline.direct.constants.WorldlinedirectbackofficeConstants;
import com.worldline.direct.model.WorldlineConfigurationModel;

import java.util.HashMap;
import java.util.Map;

public class WorldlineFeatureRequestAction extends AbstractComponentWidgetAdapterAware implements CockpitAction<WorldlineConfigurationModel, String> {

    public static final String WIZARD_ID = "WorldlineFeatureRequestWizard";

    @Override
    public ActionResult<String> perform(final ActionContext<WorldlineConfigurationModel> actionContext) {
        Map<String, Object> contextMap = new HashMap<>();
        WorldlineConfigurationModel worldlineConfiguration = actionContext.getData();

        contextMap.put(WorldlinedirectbackofficeConstants.WORLDLINE_CONFIG_WIZARD_CONTEXT, worldlineConfiguration);
        contextMap.put("TYPE_CODE", WIZARD_ID);

        sendOutput("featureRequestWizardContext", contextMap);
        return new ActionResult<>(ActionResult.SUCCESS, "Opening email form wizard.");
    }

    @Override
    public boolean canPerform(final ActionContext<WorldlineConfigurationModel> actionContext) {
        return true;
    }
}
