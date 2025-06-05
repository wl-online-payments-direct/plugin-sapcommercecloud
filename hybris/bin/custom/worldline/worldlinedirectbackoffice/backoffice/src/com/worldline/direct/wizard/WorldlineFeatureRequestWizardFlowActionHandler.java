package com.worldline.direct.wizard;

import com.hybris.cockpitng.config.jaxb.wizard.CustomType;
import com.hybris.cockpitng.widgets.configurableflow.FlowActionHandler;
import com.hybris.cockpitng.widgets.configurableflow.FlowActionHandlerAdapter;
import com.worldline.direct.constants.WorldlinedirectbackofficeConstants;
import com.worldline.direct.dto.FeatureRequestFormBean;
import com.worldline.direct.model.WorldlineConfigurationModel;
import de.hybris.platform.servicelayer.config.ConfigurationService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.zkoss.zk.ui.Executions;

import java.util.Map;

public class WorldlineFeatureRequestWizardFlowActionHandler implements FlowActionHandler {
    private static final String HYBRIS_VERSION_CONFIG = "build.version";
    private static final String WORLDLINE_PLUGIN_VERSION_CONFIG = "worldline.direct.api.shoppingCartExtension.version";
    private static final String WORLDLINE_FEATURE_REQUEST_EMAIL_CONFIG = "worldline.direct.featurerequest.mailto";
    private static final String LINE_BREAK = "%0D%0A";
    private static final String SPACE = "%20";

    private static final Logger LOG = LoggerFactory.getLogger(WorldlineFeatureRequestWizardFlowActionHandler.class);

    private ConfigurationService configurationService;

    @Override
    public void perform(CustomType customType, FlowActionHandlerAdapter flowActionHandlerAdapter, Map<String, String> map) {
        Map<String, Object> context = flowActionHandlerAdapter.getWidgetInstanceManager().getModel().getValue("currentContext", Map.class);

        FeatureRequestFormBean featureRequestFormBean = flowActionHandlerAdapter.getWidgetInstanceManager().getModel().getValue("featureRequestFormBean", FeatureRequestFormBean.class);
        WorldlineConfigurationModel worldlineConfiguration = (WorldlineConfigurationModel) context.get(WorldlinedirectbackofficeConstants.WORLDLINE_CONFIG_WIZARD_CONTEXT);

        String mailTo = generateMailTo(featureRequestFormBean, worldlineConfiguration);

        Executions.getCurrent().sendRedirect(mailTo, "_blank");
        flowActionHandlerAdapter.done();
    }

    /**
     * Generates a mailTo link as a String for a Worldline Feature Request email based on the provided form bean
     * and using the Configuration Service to retrieve the Worldline
     * @param featureRequestFormBean The form bean with user inputted data for the feature request.
     * @param worldlineConfiguration The WorldlineConfiguration selected by the user which provides their merchant ID.
     * @return A String representing a mailTo link with the required feature request and plugin/platform info.
     */
    private String generateMailTo(FeatureRequestFormBean featureRequestFormBean, WorldlineConfigurationModel worldlineConfiguration) {
        // Extract the values from the DAO and the plugin version from standard configuration and redirect the user to a mailto: link
        if(featureRequestFormBean == null) {
            LOG.error("Cannot get Feature Request Form Bean from Wizard Context. Will not generate mailto.");
            return StringUtils.EMPTY;
        }

        String mailTo = configurationService.getConfiguration().getString(WORLDLINE_FEATURE_REQUEST_EMAIL_CONFIG);
        String merchantId = worldlineConfiguration.getMerchantID();
        String hybrisVersion = configurationService.getConfiguration().getString(HYBRIS_VERSION_CONFIG);
        String pluginVersion = configurationService.getConfiguration().getString(WORLDLINE_PLUGIN_VERSION_CONFIG);
        String companyName = featureRequestFormBean.getCompanyName();
        String featureRequest = featureRequestFormBean.getRequest();

        StringBuilder mailToLink = new StringBuilder("mailto:");
        mailToLink.append(mailTo);

        mailToLink.append("?subject=Worldline Direct SAP Commerce Plugin Feature Request");
        mailToLink.append("&body=");

        mailToLink.append("Merchant ID: ");
        mailToLink.append(merchantId);
        mailToLink.append(LINE_BREAK);

        mailToLink.append("SAP Commerce Version: ");
        mailToLink.append(hybrisVersion);
        mailToLink.append(LINE_BREAK);

        mailToLink.append("Worldline Direct Plugin Version: ");
        mailToLink.append(pluginVersion);
        mailToLink.append(LINE_BREAK);

        mailToLink.append("Company Name: ");
        mailToLink.append(companyName);
        mailToLink.append(LINE_BREAK);

        mailToLink.append("Feature Request: ");
        mailToLink.append(featureRequest);


        return mailToLink.toString();
    }

    @Required
    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

}
