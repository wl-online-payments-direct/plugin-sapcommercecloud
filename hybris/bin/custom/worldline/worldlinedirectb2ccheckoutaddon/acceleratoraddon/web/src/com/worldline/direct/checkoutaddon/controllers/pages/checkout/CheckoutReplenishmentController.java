/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.worldline.direct.checkoutaddon.controllers.pages.checkout;

import com.worldline.direct.facade.WorldlineCustomerAccountFacade;
import de.hybris.platform.acceleratorfacades.flow.impl.SessionOverrideCheckoutFlowFacade;
import de.hybris.platform.acceleratorstorefrontcommons.annotations.RequireHardLogIn;
import de.hybris.platform.acceleratorstorefrontcommons.constants.WebConstants;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.ThirdPartyConstants;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.pages.AbstractCheckoutController;
import de.hybris.platform.b2bacceleratorfacades.order.data.ScheduledCartData;
import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.cms2.model.pages.ContentPageModel;
import de.hybris.platform.servicelayer.exceptions.ModelNotFoundException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;


/**
 * CheckoutController
 */
@Controller
@RequestMapping(value = "/checkout/replenishment")
public class CheckoutReplenishmentController extends AbstractCheckoutController
{
    protected static final Logger LOG = Logger.getLogger(CheckoutReplenishmentController.class);
    /**
     * We use this suffix pattern because of an issue with Spring 3.1 where a Uri value is incorrectly extracted if it
     * contains on or more '.' characters. Please see https://jira.springsource.org/browse/SPR-6164 for a discussion on
     * the issue and future resolution.
     */
    private static final String JOB_CODE_PATH_VARIABLE_PATTERN = "{jobCode:.*}";

    private static final String CHECKOUT_REPLENISHMENT_CONFIRMATION_CMS_PAGE = "replenishmentConfirmationPage";
    private static final String ACCOUNT_REPLENISHMENT_PAGE = "/my-account/my-replenishment";
    private static final String CONTINUE_URL_KEY = "continueUrl";
    private static final String SCHEDULE_URL_KEY = "scheduleUrl";

    @Resource(name = "worldlineCustomerAccountFacade")
    private WorldlineCustomerAccountFacade worldlineCustomerAccountFacade;

    @ExceptionHandler(ModelNotFoundException.class)
    public String handleModelNotFoundException(final ModelNotFoundException exception, final HttpServletRequest request)
    {
        request.setAttribute("message", exception.getMessage());
        return FORWARD_PREFIX + "/404";
    }

    @RequestMapping(value = "/confirmation/" + JOB_CODE_PATH_VARIABLE_PATTERN, method = RequestMethod.GET)
    @RequireHardLogIn
    public String orderConfirmation(@PathVariable("jobCode") final String jobCode, final Model model)
            throws CMSItemNotFoundException
    {
        SessionOverrideCheckoutFlowFacade.resetSessionOverrides();
        return processOrderCode(jobCode, model);
    }

    protected String processOrderCode(final String jobCode, final Model model) throws CMSItemNotFoundException
    {
        final ScheduledCartData scheduledCartData = worldlineCustomerAccountFacade.getCartToOrderCronJob(jobCode);

        model.addAttribute("orderData", scheduledCartData);
        model.addAttribute(ThirdPartyConstants.SeoRobots.META_ROBOTS, ThirdPartyConstants.SeoRobots.NOINDEX_NOFOLLOW);
        final String continueUrl = getSessionService().getAttribute(WebConstants.CONTINUE_URL);
        model.addAttribute(CONTINUE_URL_KEY, (StringUtils.isNotEmpty(continueUrl)) ? continueUrl : ROOT);
        model.addAttribute(SCHEDULE_URL_KEY, ACCOUNT_REPLENISHMENT_PAGE + "/" + jobCode);

        final ContentPageModel replenishmentConfirmationPage = getContentPageForLabelOrId(CHECKOUT_REPLENISHMENT_CONFIRMATION_CMS_PAGE);
        storeCmsPageInModel(model, replenishmentConfirmationPage);
        setUpMetaDataForContentPage(model, replenishmentConfirmationPage);

        return getViewForPage(model);
    }

}
