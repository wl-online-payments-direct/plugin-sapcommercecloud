/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.worldline.direct.checkoutaddon.controllers.pages;

import com.worldline.direct.checkoutaddon.controllers.WorldlineWebConstants;
import com.worldline.direct.checkoutaddon.forms.ReorderForm;
import com.worldline.direct.facade.WorldlineReplenishmentFacade;
import de.hybris.platform.acceleratorfacades.ordergridform.OrderGridFormFacade;
import de.hybris.platform.acceleratorfacades.product.data.ReadOnlyOrderGridData;
import de.hybris.platform.acceleratorstorefrontcommons.annotations.RequireHardLogIn;
import de.hybris.platform.acceleratorstorefrontcommons.breadcrumb.Breadcrumb;
import de.hybris.platform.acceleratorstorefrontcommons.breadcrumb.ResourceBreadcrumbBuilder;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.ThirdPartyConstants;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.pages.AbstractSearchPageController;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.util.GlobalMessages;
import de.hybris.platform.b2bacceleratorfacades.order.data.ScheduledCartData;
import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.cms2.model.pages.ContentPageModel;
import de.hybris.platform.commercefacades.customer.CustomerFacade;
import de.hybris.platform.commercefacades.order.OrderFacade;
import de.hybris.platform.commercefacades.order.data.OrderHistoryData;
import de.hybris.platform.commercefacades.product.ProductOption;
import de.hybris.platform.commerceservices.search.pagedata.PageableData;
import de.hybris.platform.commerceservices.search.pagedata.SearchPageData;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


@Controller
@RequestMapping({WorldlineWebConstants.URL.Account.root})
public class WorldlineReplenishmentOrdersController extends AbstractSearchPageController {
    private static final String REDIRECT_MY_ACCOUNT = REDIRECT_PREFIX + "/my-account";
    private static final String REDIRECT_TO_MYREPLENISHMENTS_PAGE = REDIRECT_PREFIX + "/my-account/worldline/my-replenishment";
    private static final String REDIRECT_TO_MYREPLENISHMENTS_DETAILS_PAGE = REDIRECT_PREFIX + "/my-account/worldline/my-replenishment/%s/";

    private static final String MY_REPLENISHMENT_ORDERS_CMS_PAGE = "my-replenishment-orders";
    private static final String MY_REPLENISHMENT_CANCEL_CONFIRMATION_CMS_PAGE = "my-replenishment-cancel-confirmation";
    private static final String MY_REPLENISHMENT_DETAILS_CMS_PAGE = "my-replenishment-details";
    private static final String ACCOUNT_ORDER_CMS_PAGE = "order";

    private static final String JOB_CODE_PATH_VARIABLE_PATTERN = "{jobCode:.*}";
    private static final String ORDER_CODE_PATH_VARIABLE_PATTERN = "{orderCode:.*}";

    private static final Logger LOG = Logger.getLogger(WorldlineReplenishmentOrdersController.class);
    @Resource(name = "customerFacade")
    protected CustomerFacade customerFacade;

    @Resource(name = "worldlineReplenishmentFacade")
    private WorldlineReplenishmentFacade worldlineReplenishmentFacade;
    @Resource(name = "orderFacade")
    private OrderFacade orderFacade;

    @Resource(name = "orderGridFormFacade")
    private OrderGridFormFacade orderGridFormFacade;

    @Resource(name = "accountBreadcrumbBuilder")
    private ResourceBreadcrumbBuilder accountBreadcrumbBuilder;

    @RequestMapping(value = "/my-replenishment", method = RequestMethod.GET)
    @RequireHardLogIn
    public String myReplenishment(@RequestParam(value = "page", defaultValue = "0") final int page,
                                  @RequestParam(value = "show", defaultValue = "Page") final ShowMode showMode,
                                  @RequestParam(value = "sort", required = false) final String sortCode, final Model model)
            throws CMSItemNotFoundException {
        final PageableData pageableData = createPageableData(page, 5, sortCode, showMode);
        final SearchPageData<? extends ScheduledCartData> searchPageData = worldlineReplenishmentFacade.getPagedReplenishmentHistory(pageableData);
        populateModel(model, searchPageData, showMode);

        model.addAttribute("breadcrumbs", accountBreadcrumbBuilder.getBreadcrumbs("text.account.manageReplenishment"));

        final ContentPageModel myReplenishmentOrdersPage = getContentPageForLabelOrId(MY_REPLENISHMENT_ORDERS_CMS_PAGE);
        storeCmsPageInModel(model, myReplenishmentOrdersPage);
        setUpMetaDataForContentPage(model, myReplenishmentOrdersPage);
        model.addAttribute(ThirdPartyConstants.SeoRobots.META_ROBOTS, ThirdPartyConstants.SeoRobots.NOINDEX_NOFOLLOW);
        return getViewForPage(model);
    }

    @RequestMapping(value = "/my-replenishment/cancel/" + JOB_CODE_PATH_VARIABLE_PATTERN, method =
            {RequestMethod.GET, RequestMethod.POST}) // NOSONAR
    @RequireHardLogIn
    public String cancelReplenishment(@PathVariable("jobCode") final String jobCode, final Model model,
                                      final RedirectAttributes redirectModel) throws CMSItemNotFoundException {
        worldlineReplenishmentFacade.cancelReplenishment(jobCode, customerFacade.getCurrentCustomer().getUid());
        GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.CONF_MESSAGES_HOLDER,
                "text.account.replenishment.confirmation.canceled");
        return REDIRECT_TO_MYREPLENISHMENTS_PAGE;
    }

    @RequestMapping(value = "/my-replenishment/confirmation/cancel/" + JOB_CODE_PATH_VARIABLE_PATTERN, method = RequestMethod.GET)
    @RequireHardLogIn
    public String confirmCancelReplenishment(@PathVariable("jobCode") final String jobCode, final Model model,
                                             final HttpServletRequest request) throws CMSItemNotFoundException {
        final List<Breadcrumb> breadcrumbs = accountBreadcrumbBuilder.getBreadcrumbs(null);
        breadcrumbs.add(new Breadcrumb("/my-account/my-replenishment", getMessageSource().getMessage(
                "text.account.manageReplenishment", null, getI18nService().getCurrentLocale()), null));
        breadcrumbs.add(new Breadcrumb("#", getMessageSource().getMessage(
                "text.account.manageReplenishment.confirm.cancel.breadcrumb", new Object[]
                        {jobCode}, "Remove Replenishment Schedule {0}", getI18nService().getCurrentLocale()), null));

        model.addAttribute("breadcrumbs", breadcrumbs);
        model.addAttribute("arguments", String.format("%s", jobCode));
        model.addAttribute("page", "replenishment");
        model.addAttribute("disableUrl",
                String.format("%s/my-account/my-replenishment/cancel/%s", request.getContextPath(), jobCode));
        model.addAttribute("cancelUrl", String.format("%s/my-account/my-replenishment/%s", request.getContextPath(), jobCode));
        final ContentPageModel myReplenishmentCancelConfirmationPage = getContentPageForLabelOrId(MY_REPLENISHMENT_CANCEL_CONFIRMATION_CMS_PAGE);
        storeCmsPageInModel(model, myReplenishmentCancelConfirmationPage);
        setUpMetaDataForContentPage(model, myReplenishmentCancelConfirmationPage);
        model.addAttribute(ThirdPartyConstants.SeoRobots.META_ROBOTS, ThirdPartyConstants.SeoRobots.NOINDEX_NOFOLLOW);
        return getViewForPage(model);
    }

    @RequestMapping(value = "/my-replenishment/" + JOB_CODE_PATH_VARIABLE_PATTERN, method = RequestMethod.GET)
    @RequireHardLogIn
    public String replenishmentDetails(@RequestParam(value = "page", defaultValue = "0") final int page,
                                       @RequestParam(value = "show", defaultValue = "Page") final ShowMode showMode,
                                       @RequestParam(value = "sort", required = false) final String sortCode, @PathVariable("jobCode") final String jobCode,
                                       final Model model) throws CMSItemNotFoundException {
        final ScheduledCartData scheduledCartData = worldlineReplenishmentFacade.getReplenishmentOrderDetailsForCode(jobCode, customerFacade
                .getCurrentCustomer().getUid());
        model.addAttribute("orderData", scheduledCartData);

        final PageableData pageableData = createPageableData(page, 5, sortCode, showMode);
        final SearchPageData<? extends OrderHistoryData> searchPageData = worldlineReplenishmentFacade.getPagedReplenishmentOrderHistory(jobCode,
                pageableData);
        populateModel(model, searchPageData, showMode);

        final List<Breadcrumb> breadcrumbs = accountBreadcrumbBuilder.getBreadcrumbs(null);
        breadcrumbs.add(new Breadcrumb("/my-account/my-replenishment", getMessageSource().getMessage(
                "text.account.manageReplenishment", null, getI18nService().getCurrentLocale()), null));
        breadcrumbs.add(new Breadcrumb(String.format("/my-account/my-replenishment/%s/", urlEncode(jobCode)), getMessageSource()
                .getMessage("text.account.replenishment.replenishmentBreadcrumb", new Object[]
                        {jobCode}, "Replenishment Schedule {0}", getI18nService().getCurrentLocale()), null));
        model.addAttribute("breadcrumbs", breadcrumbs);
        final ContentPageModel myReplenishmentDetailsPage = getContentPageForLabelOrId(MY_REPLENISHMENT_DETAILS_CMS_PAGE);
        storeCmsPageInModel(model, myReplenishmentDetailsPage);
        setUpMetaDataForContentPage(model, myReplenishmentDetailsPage);
        model.addAttribute(ThirdPartyConstants.SeoRobots.META_ROBOTS, ThirdPartyConstants.SeoRobots.NOINDEX_NOFOLLOW);
        return getViewForPage(model);
    }

    @RequestMapping(value = "/my-replenishment/detail/cancel/" + JOB_CODE_PATH_VARIABLE_PATTERN, method =
            {RequestMethod.GET, RequestMethod.POST}) // NOSONAR
    @RequireHardLogIn
    public String cancelReplenishmentFromDetailPage(@PathVariable("jobCode") final String jobCode, final Model model,
                                                    final RedirectAttributes redirectModel) throws CMSItemNotFoundException {
        this.worldlineReplenishmentFacade.cancelReplenishment(jobCode, customerFacade.getCurrentCustomer().getUid());
        GlobalMessages.addFlashMessage(redirectModel, GlobalMessages.CONF_MESSAGES_HOLDER,
                "text.account.replenishment.confirmation.canceled");
        return String.format(REDIRECT_TO_MYREPLENISHMENTS_DETAILS_PAGE, jobCode);
    }

    @RequestMapping(value = "/my-replenishment/detail/confirmation/cancel/" + JOB_CODE_PATH_VARIABLE_PATTERN, method = RequestMethod.GET)
    @RequireHardLogIn
    public String confirmCancelReplenishmentFromDetailsPage(@PathVariable("jobCode") final String jobCode, final Model model,
                                                            final HttpServletRequest request) throws CMSItemNotFoundException {
        final List<Breadcrumb> breadcrumbs = accountBreadcrumbBuilder.getBreadcrumbs(null);
        breadcrumbs.add(new Breadcrumb("/my-account/my-replenishment", getMessageSource().getMessage(
                "text.account.manageReplenishment", null, getI18nService().getCurrentLocale()), null));
        breadcrumbs.add(new Breadcrumb(String.format("/my-account/my-replenishment/%s/", jobCode), getMessageSource().getMessage(
                "text.account.replenishment.replenishmentBreadcrumb", new Object[]
                        {jobCode}, "Replenishment Orders {0}", getI18nService().getCurrentLocale()), null));
        breadcrumbs.add(new Breadcrumb("#", getMessageSource().getMessage(
                "text.account.manageReplenishment.confirm.cancel.breadcrumb", new Object[]
                        {jobCode}, "Remove Replenishment Schedule {0}", getI18nService().getCurrentLocale()), null));

        model.addAttribute("breadcrumbs", breadcrumbs);
        model.addAttribute("arguments", String.format("%s", jobCode));
        model.addAttribute("page", "replenishment");
        model.addAttribute("disableUrl",
                String.format("%s/my-account/my-replenishment/detail/cancel/%s", request.getContextPath(), jobCode));
        model.addAttribute("cancelUrl", String.format("%s/my-account/my-replenishment/%s", request.getContextPath(), jobCode));

        final ContentPageModel myReplenishmentCancelConfirmationPage = getContentPageForLabelOrId(MY_REPLENISHMENT_CANCEL_CONFIRMATION_CMS_PAGE);
        storeCmsPageInModel(model, myReplenishmentCancelConfirmationPage);
        setUpMetaDataForContentPage(model, myReplenishmentCancelConfirmationPage);
        model.addAttribute(ThirdPartyConstants.SeoRobots.META_ROBOTS, ThirdPartyConstants.SeoRobots.NOINDEX_NOFOLLOW);
        return getViewForPage(model);
    }

    @RequestMapping(value = "/my-replenishment/" + JOB_CODE_PATH_VARIABLE_PATTERN + "/" + ORDER_CODE_PATH_VARIABLE_PATTERN, method = RequestMethod.GET)
    @RequireHardLogIn
    public String replenishmentOrderDetail(@PathVariable("jobCode") final String jobCode,
                                           @PathVariable("orderCode") final String orderCode, final Model model) throws CMSItemNotFoundException {
        try {
            model.addAttribute("orderData", orderFacade.getOrderDetailsForCode(orderCode));
            model.addAttribute("scheduleData",
                    worldlineReplenishmentFacade.getReplenishmentOrderDetailsForCode(jobCode, customerFacade.getCurrentCustomer().getUid()));
            model.addAttribute(new ReorderForm());

            final List<Breadcrumb> breadcrumbs = accountBreadcrumbBuilder.getBreadcrumbs(null);
            breadcrumbs.add(new Breadcrumb("/my-account/my-replenishment", getMessageSource().getMessage(
                    "text.account.manageReplenishment", null, getI18nService().getCurrentLocale()), null));
            breadcrumbs.add(new Breadcrumb(String.format("/my-account/my-replenishment/%s/", jobCode), getMessageSource()
                    .getMessage("text.account.replenishment.replenishmentBreadcrumb", new Object[]
                            {jobCode}, "Replenishment {0}", getI18nService().getCurrentLocale()), null));
            breadcrumbs.add(new Breadcrumb(String.format("/my-account/my-replenishment/%s/%s/", jobCode, orderCode),
                    getMessageSource().getMessage("text.account.replenishment.replenishmentOrderDetailBreadcrumb", new Object[]
                            {orderCode}, "Order {0}", getI18nService().getCurrentLocale()), null));
            model.addAttribute("breadcrumbs", breadcrumbs);
        } catch (final UnknownIdentifierException e) {
            LOG.warn("Attempted to load a order that does not exist or is not visible", e);
            return REDIRECT_MY_ACCOUNT;
        }
        final ContentPageModel accountOrderPage = getContentPageForLabelOrId(ACCOUNT_ORDER_CMS_PAGE);
        storeCmsPageInModel(model, accountOrderPage);
        setUpMetaDataForContentPage(model, accountOrderPage);
        model.addAttribute(ThirdPartyConstants.SeoRobots.META_ROBOTS, ThirdPartyConstants.SeoRobots.NOINDEX_NOFOLLOW);
        return getViewForPage(model);
    }

    @RequestMapping(value = "/my-replenishment/" + JOB_CODE_PATH_VARIABLE_PATTERN
            + "/getReadOnlyProductVariantMatrix", method = RequestMethod.GET)
    @RequireHardLogIn
    public String getProductVariantMatrixForResponsive(@PathVariable("jobCode") final String jobCode,
                                                       @RequestParam("productCode") final String productCode, final Model model) {
        final ScheduledCartData scheduledCartData = worldlineReplenishmentFacade.getReplenishmentOrderDetailsForCode(jobCode,
                customerFacade.getCurrentCustomer().getUid());

        final Map<String, ReadOnlyOrderGridData> readOnlyMultiDMap = orderGridFormFacade.getReadOnlyOrderGridForProductInOrder(
                productCode, Arrays.asList(ProductOption.BASIC, ProductOption.CATEGORIES), scheduledCartData);
        model.addAttribute("readOnlyMultiDMap", readOnlyMultiDMap);

        return WorldlineWebConstants.URL.Account.Replenishment.ReadOnlyExpandedOrderForm;
    }
}
