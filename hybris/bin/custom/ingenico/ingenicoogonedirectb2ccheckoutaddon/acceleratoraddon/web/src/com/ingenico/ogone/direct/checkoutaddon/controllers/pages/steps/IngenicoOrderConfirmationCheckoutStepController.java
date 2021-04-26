package com.ingenico.ogone.direct.checkoutaddon.controllers.pages.steps;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import com.ingenico.ogone.direct.constants.Ingenicoogonedirectb2ccheckoutaddonConstants;
import de.hybris.platform.acceleratorservices.controllers.page.PageType;
import de.hybris.platform.acceleratorstorefrontcommons.annotations.RequireHardLogIn;
import de.hybris.platform.acceleratorstorefrontcommons.constants.WebConstants;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.pages.AbstractCheckoutController;
import de.hybris.platform.acceleratorstorefrontcommons.forms.RegisterForm;
import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.cms2.model.pages.ContentPageModel;
import de.hybris.platform.commercefacades.order.OrderFacade;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping(value = "/checkout/ingenico")
public class IngenicoOrderConfirmationCheckoutStepController extends AbstractCheckoutController {

   private static final Logger LOG = Logger.getLogger(IngenicoOrderConfirmationCheckoutStepController.class);
   private static final String ORDER_CODE_PATH_VARIABLE_PATTERN = "{orderCode:.*}";
   private static final String CHECKOUT_ORDER_CONFIRMATION_CMS_PAGE_LABEL = "orderConfirmation";

   @Resource(name = "orderFacade")
   private OrderFacade orderFacade;

   @RequestMapping(value = "/orderConfirmation/" + ORDER_CODE_PATH_VARIABLE_PATTERN, method = RequestMethod.GET)
   @RequireHardLogIn
   public String orderConfirmation(@PathVariable("orderCode") final String orderCode, final HttpServletRequest request,
         final Model model, final RedirectAttributes redirectModel) throws CMSItemNotFoundException {

      final OrderData orderDetails;

      try
      {
         orderDetails = orderFacade.getOrderDetailsForCode(orderCode);
      }
      catch (final UnknownIdentifierException e)
      {
         LOG.warn("Attempted to load an order confirmation that does not exist or is not visible. Redirect to home page.");
         return REDIRECT_PREFIX + ROOT;
      }

      if (super.getUserFacade().isAnonymousUser()) {
         model.addAttribute("isAnonymousUser", true);
         model.addAttribute(new RegisterForm());
      }

      model.addAttribute("orderCode", orderCode);
      model.addAttribute("orderData", orderDetails);
      model.addAttribute("allItems", orderDetails.getEntries());
      model.addAttribute("deliveryAddress", orderDetails.getDeliveryAddress());
      model.addAttribute("deliveryMode", orderDetails.getDeliveryMode());
      model.addAttribute("paymentInfo", orderDetails.getPaymentInfo());
      model.addAttribute("pageType", PageType.ORDERCONFIRMATION.name());


      final ContentPageModel orderConfirmtionPage = getContentPageForLabelOrId(CHECKOUT_ORDER_CONFIRMATION_CMS_PAGE_LABEL);
      storeCmsPageInModel(model, orderConfirmtionPage);
      setUpMetaDataForContentPage(model, orderConfirmtionPage);
      model.addAttribute("metaRobots", "noindex,nofollow");


      return Ingenicoogonedirectb2ccheckoutaddonConstants.Views.Pages.MultiStepCheckout.ingenicoOrderConfirmationPage;
   }

}
