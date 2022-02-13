package com.worldline.direct.actions;

import java.util.StringJoiner;

import com.hybris.cockpitng.actions.ActionContext;
import com.hybris.cockpitng.actions.ActionResult;
import com.hybris.cockpitng.actions.CockpitAction;

import de.hybris.platform.util.Config;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.Executions;

import com.worldline.direct.model.WorldlineConfigurationModel;

public class WorldlineSupportContactAction implements CockpitAction<WorldlineConfigurationModel, Object> {

    private final static Logger LOGGER = LoggerFactory.getLogger(WorldlineSupportContactAction.class);
    private static final String SUPPORT_WORLDLINE_EMAIL = "support.ecom@worldline.com";
    private static final String MAILTO = "mailto:";

    @Override
    public ActionResult<Object> perform(ActionContext<WorldlineConfigurationModel> actionContext) {

        StringBuilder mailto = new StringBuilder(MAILTO)
                .append(SUPPORT_WORLDLINE_EMAIL)
                .append("?");

        String cc = Config.getString("worldline.contact.support.default.cc", "");
        String bcc = Config.getString("worldline.contact.support.default.bcc", "");
        String subject = Config.getString("worldline.contact.support.default.subject", "");

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
        LOGGER.debug("[WORLDLINE] Contact support using {}",mailto.toString());
        Executions.getCurrent().sendRedirect(mailto.toString(), "_blank");

        return new ActionResult<>(ActionResult.SUCCESS);
    }
}
