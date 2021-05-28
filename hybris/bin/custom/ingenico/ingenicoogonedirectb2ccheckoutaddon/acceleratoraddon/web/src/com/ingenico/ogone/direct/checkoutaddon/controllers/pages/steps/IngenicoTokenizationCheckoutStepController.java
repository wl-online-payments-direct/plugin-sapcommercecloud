package com.ingenico.ogone.direct.checkoutaddon.controllers.pages.steps;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import de.hybris.platform.acceleratorservices.urlresolver.SiteBaseUrlResolutionService;
import de.hybris.platform.acceleratorstorefrontcommons.annotations.PreValidateCheckoutStep;
import de.hybris.platform.acceleratorstorefrontcommons.annotations.RequireHardLogIn;
import de.hybris.platform.acceleratorstorefrontcommons.checkout.steps.CheckoutStep;
import de.hybris.platform.acceleratorstorefrontcommons.constants.WebConstants;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.pages.checkout.steps.AbstractCheckoutStepController;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.util.GlobalMessages;
import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.cms2.model.pages.ContentPageModel;
import de.hybris.platform.commercefacades.order.OrderFacade;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commerceservices.order.CommerceCartModificationException;
import de.hybris.platform.commerceservices.strategies.CheckoutCustomerStrategy;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.servicelayer.config.ConfigurationService;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ingenico.direct.domain.CreateHostedTokenizationResponse;
import com.ingenico.ogone.direct.checkoutaddon.controllers.IngenicoWebConstants;
import com.ingenico.ogone.direct.checkoutaddon.controllers.IngenicoWebConstants.URL.Checkout.Payment.HTP;
import com.ingenico.ogone.direct.checkoutaddon.forms.IngenicoHostedTokenizationForm;
import com.ingenico.ogone.direct.checkoutaddon.forms.validation.IngenicoHostedTokenizationValidator;
import com.ingenico.ogone.direct.constants.IngenicoCheckoutConstants;
import com.ingenico.ogone.direct.exception.IngenicoNonAuthorizedPaymentException;
import com.ingenico.ogone.direct.facade.IngenicoCheckoutFacade;
import com.ingenico.ogone.direct.facade.IngenicoUserFacade;
import com.ingenico.ogone.direct.order.data.BrowserData;
import com.ingenico.ogone.direct.order.data.IngenicoHostedTokenizationData;

@Controller
@RequestMapping(value = HTP.root)
public class IngenicoTokenizationCheckoutStepController extends AbstractCheckoutStepController {
    private static final Logger LOGGER = Logger.getLogger(IngenicoTokenizationCheckoutStepController.class);

    private final static String PAYMENT = "do-payment";
    private static final String ACCEPT = "accept";
    private static final String USER_AGENT = "user-agent";
    private static final String X_FORWARDED_FOR = "X-FORWARDED-FOR";

    @Resource(name = "orderFacade")
    private OrderFacade orderFacade;

    @Resource(name = "ingenicoUserFacade")
    private IngenicoUserFacade ingenicoUserFacade;

    @Resource(name = "ingenicoCheckoutFacade")
    private IngenicoCheckoutFacade ingenicoCheckoutFacade;

    @Resource(name = "configurationService")
    private ConfigurationService configurationService;

    @Resource(name = "ingenicoHostedTokenizationValidator")
    private IngenicoHostedTokenizationValidator ingenicoHostedTokenizationValidator;

    @Resource(name = "siteBaseUrlResolutionService")
    private SiteBaseUrlResolutionService siteBaseUrlResolutionService;

    @Resource(name = "checkoutCustomerStrategy")
    private CheckoutCustomerStrategy checkoutCustomerStrategy;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @RequestMapping(value = HTP.view, method = RequestMethod.GET)
    @RequireHardLogIn
    @Override
    @PreValidateCheckoutStep(checkoutStep = PAYMENT)
    public String enterStep(final Model model, final RedirectAttributes redirectAttributes) throws CMSItemNotFoundException, // NOSONAR
            CommerceCartModificationException {


        final CreateHostedTokenizationResponse hostedTokenization = ingenicoCheckoutFacade.createHostedTokenization();
        model.addAttribute("hostedTokenization", hostedTokenization);
        model.addAttribute("savedPaymentInfos", ingenicoUserFacade.getIngenicoPaymentInfos(Boolean.TRUE));
        final CartData cartData = getCheckoutFacade().getCheckoutCart();
        model.addAttribute("cartData", cartData);
        model.addAttribute("ingenicoDoPaymentForm", new IngenicoHostedTokenizationForm());

        final ContentPageModel multiCheckoutSummaryPage = getContentPageForLabelOrId(MULTI_CHECKOUT_SUMMARY_CMS_PAGE_LABEL);
        storeCmsPageInModel(model, multiCheckoutSummaryPage);
        setUpMetaDataForContentPage(model, multiCheckoutSummaryPage);

        model.addAttribute(WebConstants.BREADCRUMBS_KEY, getResourceBreadcrumbBuilder().getBreadcrumbs("checkout.multi.summary.breadcrumb"));
        model.addAttribute("metaRobots", "noindex,nofollow");
        setCheckoutStepLinksForModel(model, getCheckoutStep());

        return IngenicoCheckoutConstants.Views.Pages.MultiStepCheckout.ingenicoDoPaymentPage;
    }

    @RequestMapping(value = HTP.create, method = RequestMethod.POST)
    @RequireHardLogIn
    public String doPayment(@ModelAttribute("ingenicoHostedTokenizationForm") final IngenicoHostedTokenizationForm ingenicoHostedTokenizationForm,
                            final Model model,
                            final HttpServletRequest request,
                            final BindingResult bindingResult,
                            final RedirectAttributes redirectAttributes) throws CMSItemNotFoundException, CommerceCartModificationException, InvalidCartException {
        ingenicoHostedTokenizationValidator.validate(ingenicoHostedTokenizationForm, bindingResult);
        if (bindingResult.hasErrors()) {
            GlobalMessages.addErrorMessage(model, "checkout.error.paymentethod.formentry.invalid");
            return enterStep(model, redirectAttributes);
        }

        IngenicoHostedTokenizationData ingenicoHostedTokenizationData = fillIngenicoHostedTokenizationData(request, ingenicoHostedTokenizationForm);

        try {
            storeReturnUrlInSession();
            final OrderData orderData = ingenicoCheckoutFacade.authorisePaymentForHostedTokenization(ingenicoHostedTokenizationData);
            return redirectToOrderConfirmationPage(orderData);
        } catch (IngenicoNonAuthorizedPaymentException e) {
            switch (e.getReason()) {
                case NEED_3DS:
                    final CartData cartData = getCheckoutFacade().getCheckoutCart();
                    getSessionService().setAttribute(cartData.getCode() + "_ingenico_htp_returnmac", e.getMerchantAction().getRedirectData().getRETURNMAC());
                    getSessionService().setAttribute(cartData.getCode() + "_ingenico_htp_id", e.getPaymentResponse().getId());
                    return REDIRECT_PREFIX + e.getMerchantAction().getRedirectData().getRedirectURL();
                case REJECTED:
                    GlobalMessages.addFlashMessage(redirectAttributes,
                            GlobalMessages.ERROR_MESSAGES_HOLDER,
                            "checkout.error.payment.rejected");
                    return REDIRECT_PREFIX + IngenicoWebConstants.URL.Checkout.Payment.select;
                case CANCELLED:
                    GlobalMessages.addFlashMessage(redirectAttributes,
                            GlobalMessages.INFO_MESSAGES_HOLDER,
                            "checkout.error.payment.cancelled");
                    return REDIRECT_PREFIX + IngenicoWebConstants.URL.Checkout.Payment.select;
            }
        }
        return enterStep(model, redirectAttributes);
    }

    private void storeReturnUrlInSession() {
        final String returnUrl = siteBaseUrlResolutionService.getWebsiteUrlForSite(getBaseSiteService().getCurrentBaseSite(),
                true, HTP.root + HTP.handleResponse);
        getSessionService().setAttribute("hostedTokenizationReturnUrl", returnUrl);
    }

    @RequestMapping(value = HTP.handleResponse, method = RequestMethod.GET)
    @RequireHardLogIn
    public String handle3ds(@RequestParam(value = "REF", required = true) final String ref,
                            @RequestParam(value = "RETURNMAC", required = true) final String returnMAC,
                            @RequestParam(value = "paymentId", required = true) final String paymentId,
                            final Model model,
                            final HttpServletRequest request,
                            final RedirectAttributes redirectAttributes) throws CMSItemNotFoundException, CommerceCartModificationException, InvalidCartException {
        LOGGER.info("[ INGENICO ] handle3ds with ref:" + ref + " returnmac:" + returnMAC + " paymentId:" + paymentId);

        try {
            final OrderData orderData = ingenicoCheckoutFacade.handle3dsResponse(ref, returnMAC, paymentId);
            return redirectToOrderConfirmationPage(orderData);
        } catch (IngenicoNonAuthorizedPaymentException e) {
            switch (e.getReason()) {
                case CANCELLED:
                    GlobalMessages.addFlashMessage(redirectAttributes,
                            GlobalMessages.INFO_MESSAGES_HOLDER,
                            "checkout.error.payment.cancelled");
                    return REDIRECT_PREFIX +
                            IngenicoWebConstants.URL.Checkout.Payment.root +
                            IngenicoWebConstants.URL.Checkout.Payment.select;
                case REJECTED:
                    GlobalMessages.addFlashMessage(redirectAttributes,
                            GlobalMessages.ERROR_MESSAGES_HOLDER,
                            "checkout.error.payment.rejected");
                    return REDIRECT_PREFIX +
                            IngenicoWebConstants.URL.Checkout.Payment.root +
                            IngenicoWebConstants.URL.Checkout.Payment.select;
            }
            return REDIRECT_PREFIX + IngenicoWebConstants.URL.Checkout.Payment.select;
        }
    }

    private IngenicoHostedTokenizationData fillIngenicoHostedTokenizationData(HttpServletRequest request, IngenicoHostedTokenizationForm ingenicoHostedTokenizationForm) {
        IngenicoHostedTokenizationData ingenicoHostedTokenizationData = new IngenicoHostedTokenizationData();
        ingenicoHostedTokenizationData.setHostedTokenizationId(ingenicoHostedTokenizationForm.getHostedTokenizationId());
        BrowserData browserData = new BrowserData();
        browserData.setColorDepth(ingenicoHostedTokenizationForm.getColorDepth());
        browserData.setNavigatorJavaEnabled(ingenicoHostedTokenizationForm.getNavigatorJavaEnabled());
        browserData.setNavigatorJavaScriptEnabled(ingenicoHostedTokenizationForm.getNavigatorJavaScriptEnabled());
        browserData.setScreenHeight(ingenicoHostedTokenizationForm.getScreenHeight());
        browserData.setScreenWidth(ingenicoHostedTokenizationForm.getScreenWidth());
        browserData.setTimezoneOffsetUtcMinutes(ingenicoHostedTokenizationForm.getTimezoneOffset());

        browserData.setAcceptHeader(request.getHeader(ACCEPT));
        browserData.setUserAgent(request.getHeader(USER_AGENT));
        browserData.setLocale(request.getLocale().toString());
        browserData.setIpAddress(getIpAddress(request));

        ingenicoHostedTokenizationData.setBrowserData(browserData);
        return ingenicoHostedTokenizationData;
    }

    private String getIpAddress(HttpServletRequest request) {
        String remoteAddr = "";
        if (request != null) {
            remoteAddr = request.getHeader(X_FORWARDED_FOR);
            if (StringUtils.isEmpty(remoteAddr)) {
                remoteAddr = request.getRemoteAddr();
            }
        }
        return remoteAddr;
    }

    @Override
    protected String redirectToOrderConfirmationPage(OrderData orderData) {
        return REDIRECT_PREFIX + IngenicoWebConstants.URL.Checkout.OrderConfirmation.root + getOrderCode(orderData);
    }

    protected String getOrderCode(OrderData orderData) {
        return checkoutCustomerStrategy.isAnonymousCheckout() ? orderData.getGuid() : orderData.getCode();
    }


    @RequestMapping(value = "/back", method = RequestMethod.GET)
    @RequireHardLogIn
    @Override
    public String back(final RedirectAttributes redirectAttributes) {
        return getCheckoutStep().previousStep();
    }

    @RequestMapping(value = "/next", method = RequestMethod.GET)
    @RequireHardLogIn
    @Override
    public String next(final RedirectAttributes redirectAttributes) {
        return getCheckoutStep().nextStep();
    }

    protected CheckoutStep getCheckoutStep() {
        return getCheckoutStep(PAYMENT);
    }
}
