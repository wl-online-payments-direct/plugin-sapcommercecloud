package com.worldline.direct.b2bcheckoutaddon.controllers.pages;

import com.worldline.direct.b2bcheckoutaddon.controllers.WorldlineWebConstants;
import com.worldline.direct.facade.WorldlineUserFacade;
import de.hybris.platform.acceleratorstorefrontcommons.annotations.RequireHardLogIn;
import de.hybris.platform.acceleratorstorefrontcommons.breadcrumb.ResourceBreadcrumbBuilder;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.pages.AbstractSearchPageController;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.util.GlobalMessages;
import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.commercefacades.customer.CustomerFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;

@Controller
@RequestMapping({WorldlineWebConstants.URL.Account.root})
public class WorldlineAccountPageController extends AbstractSearchPageController {

    private static final String ADD_EDIT_ADDRESS_CMS_PAGE = "add-edit-address";
    private static final String PAYMENT_DETAILS_CMS_PAGE = "payment-details";

    private static final Logger LOGGER = LoggerFactory.getLogger(WorldlineAccountPageController.class);

    @Resource(name = "worldlineUserFacade")
    private WorldlineUserFacade worldlineUserFacade;
    @Resource(name = "customerFacade")
    private CustomerFacade customerFacade;
    @Resource(name = "accountBreadcrumbBuilder")
    private ResourceBreadcrumbBuilder accountBreadcrumbBuilder;


    @RequestMapping(
            value = {"/payment-details"},
            method = {RequestMethod.GET}
    )
    @RequireHardLogIn
    public String paymentDetails(Model model) throws CMSItemNotFoundException {
        model.addAttribute("customerData", customerFacade.getCurrentCustomer());
        model.addAttribute("worldlinePaymentInfoData", worldlineUserFacade.getWorldlinePaymentInfos(Boolean.TRUE));
        storeCmsPageInModel(model, getContentPageForLabelOrId(PAYMENT_DETAILS_CMS_PAGE));
        setUpMetaDataForContentPage(model, getContentPageForLabelOrId(ADD_EDIT_ADDRESS_CMS_PAGE));
        model.addAttribute("breadcrumbs", accountBreadcrumbBuilder.getBreadcrumbs("text.account.paymentDetails"));
        model.addAttribute("metaRobots", "noindex,nofollow");
        return this.getViewForPage(model);
    }


    @RequestMapping(
            value = {"/remove-payment-detail"},
            method = {RequestMethod.POST}
    )
    @RequireHardLogIn
    public String removePaymentMethod(@RequestParam("paymentInfoId") String paymentMethodId, RedirectAttributes redirectAttributes) throws CMSItemNotFoundException {
        try {
            worldlineUserFacade.deleteSavedWorldlinePaymentInfo(paymentMethodId);
            GlobalMessages.addFlashMessage(redirectAttributes, "accConfMsgs", "text.account.profile.paymentCart.removed");
        } catch (Exception exception) {
            GlobalMessages.addFlashMessage(redirectAttributes, "accErrorMsgs", "text.account.profile.paymentCart.remove.fail");
        }
        return "redirect:" + WorldlineWebConstants.URL.Account.PaymentDetails.root;
    }


}
