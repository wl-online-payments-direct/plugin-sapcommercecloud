package com.ingenico.ogone.direct.actions;

import java.util.StringJoiner;

import com.hybris.cockpitng.actions.ActionContext;
import com.hybris.cockpitng.actions.ActionResult;
import com.hybris.cockpitng.actions.CockpitAction;

import de.hybris.platform.util.Config;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.Executions;

import com.ingenico.ogone.direct.model.IngenicoConfigurationModel;

public class IngenicoSupportContactAction implements CockpitAction<IngenicoConfigurationModel, Object> {

    private final static Logger LOGGER = LoggerFactory.getLogger(IngenicoSupportContactAction.class);

    @Override
    public ActionResult<Object> perform(ActionContext<IngenicoConfigurationModel> actionContext) {

        //TODO to define a real mail
        StringBuilder mailto = new StringBuilder("mailto:support@ingenico.com?");

        String cc = Config.getString("ingenico.contact.support.default.cc", "");
        String bcc = Config.getString("ingenico.contact.support.default.bcc", "");
        String subject = Config.getString("ingenico.contact.support.default.subject", "");

        StringJoiner parameters = new StringJoiner("&");
        if (StringUtils.isNotBlank(cc)) {
            parameters.add("cc=" + cc);
        }
        if (StringUtils.isNotBlank(bcc)) {
            parameters.add("bcc=" + bcc);
        }
        if (StringUtils.isNotBlank(subject)) {
            parameters.add("subject=" + subject);
        }
        mailto.append(parameters);
        Executions.getCurrent().sendRedirect(mailto.toString(), "_blank");

        return new ActionResult<>(ActionResult.SUCCESS);
    }
}
