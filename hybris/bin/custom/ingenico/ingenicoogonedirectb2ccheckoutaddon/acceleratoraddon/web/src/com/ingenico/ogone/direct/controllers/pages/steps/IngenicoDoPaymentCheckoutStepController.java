package com.ingenico.ogone.direct.controllers.pages.steps;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

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
import de.hybris.platform.commerceservices.order.CommerceCartModificationException;
import de.hybris.platform.payment.AdapterException;
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
import com.ingenico.direct.domain.CreatePaymentResponse;
import com.ingenico.ogone.direct.constants.Ingenicoogonedirectb2ccheckoutaddonConstants;
import com.ingenico.ogone.direct.facade.IngenicoCheckoutFacade;
import com.ingenico.ogone.direct.facade.IngenicoUserFacade;
import com.ingenico.ogone.direct.forms.validation.IngenicoHostedTokenizationValidator;
import com.ingenico.ogone.direct.forms.IngenicoHostedTokenizationForm;

import com.ingenico.ogone.direct.order.data.IngenicoHostedTokenizationData;

@Controller
@RequestMapping(value = "/checkout/multi/ingenico/payment")
public class IngenicoDoPaymentCheckoutStepController extends AbstractCheckoutStepController {
    private static final Logger LOGGER = Logger.getLogger(IngenicoDoPaymentCheckoutStepController.class);

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

    @Autowired
    private HttpServletRequest httpServletRequest;

    @RequestMapping(value = "/view", method = RequestMethod.GET)
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

        return Ingenicoogonedirectb2ccheckoutaddonConstants.Views.Pages.MultiStepCheckout.ingenicoDoPaymentPage;
    }

    @RequestMapping({"/do"})
    @RequireHardLogIn
    public String doPayment(@ModelAttribute("ingenicoHostedTokenizationForm") final IngenicoHostedTokenizationForm ingenicoHostedTokenizationForm,
                            final Model model,
                            final HttpServletRequest request,
                            final BindingResult bindingResult,
                            final RedirectAttributes redirectAttributes) throws CMSItemNotFoundException, CommerceCartModificationException {
        ingenicoHostedTokenizationValidator.validate(ingenicoHostedTokenizationForm, bindingResult);
        if (bindingResult.hasErrors()) {
            GlobalMessages.addErrorMessage(model, "checkout.error.paymentethod.formentry.invalid");
            return enterStep(model, redirectAttributes);
        }

        IngenicoHostedTokenizationData ingenicoHostedTokenizationData = fillIngenicoHostedTokenizationData(request, ingenicoHostedTokenizationForm);

        final CreatePaymentResponse paymentResponse = ingenicoCheckoutFacade.authorisePayment(ingenicoHostedTokenizationData);
        boolean isPaymentAuthorized = paymentResponse != null;
        // authorize, if failure occurs don't allow to place the order
        if (!isPaymentAuthorized) {
            GlobalMessages.addErrorMessage(model, "checkout.error.authorization.failed");
            return enterStep(model, redirectAttributes);
        }

        if (paymentResponse.getMerchantAction() != null) {

            if ("REDIRECT".equals(paymentResponse.getMerchantAction().getActionType())) {
                final CartData cartData = getCheckoutFacade().getCheckoutCart();
                getSessionService().setAttribute(cartData.getCode() + "_ingenico_htp_returnmac", paymentResponse.getMerchantAction().getRedirectData().getRETURNMAC());
                getSessionService().setAttribute(cartData.getCode() + "_ingenico_htp_id", paymentResponse.getPayment().getId());
                return REDIRECT_PREFIX + paymentResponse.getMerchantAction().getRedirectData().getRedirectURL();
            } else {
                LOGGER.error("[ INGENICO ] Merchant action " + paymentResponse.getMerchantAction().getActionType() + " not implemented!!");
            }
        }
        return enterStep(model, redirectAttributes);
    }

    @RequestMapping(value = "/handle3ds", method = RequestMethod.GET)
    @RequireHardLogIn
    public String handle3ds(@RequestParam(value = "REF", required = true) final String ref,
                            @RequestParam(value = "RETURNMAC", required = true) final String returnMAC,
                            @RequestParam(value = "paymentId", required = true) final String paymentId) {
        LOGGER.info("[ INGENICO ] handle3ds with ref:" + ref + " returnmac:" + returnMAC + " paymentId:" + paymentId);
        final CartData cartData = getCheckoutFacade().getCheckoutCart();
        final String storedReturnMAC = getSessionService().getAttribute(cartData.getCode() + "_ingenico_htp_returnmac");
        if (returnMAC.equals(storedReturnMAC)) {
            return REDIRECT_PREFIX + "/ok";
        } else {
            return REDIRECT_PREFIX + "/nok";
        }
    }

    private IngenicoHostedTokenizationData fillIngenicoHostedTokenizationData(HttpServletRequest request, IngenicoHostedTokenizationForm ingenicoHostedTokenizationForm) {
        IngenicoHostedTokenizationData ingenicoHostedTokenizationData = new IngenicoHostedTokenizationData();
        ingenicoHostedTokenizationData.setHostedTokenizationId(ingenicoHostedTokenizationForm.getHostedTokenizationId());
        ingenicoHostedTokenizationData.setColorDepth(ingenicoHostedTokenizationForm.getColorDepth());
        ingenicoHostedTokenizationData.setNavigatorJavaEnabled(ingenicoHostedTokenizationForm.getNavigatorJavaEnabled());
        ingenicoHostedTokenizationData.setScreenHeight(ingenicoHostedTokenizationForm.getScreenHeight());
        ingenicoHostedTokenizationData.setScreenWidth(ingenicoHostedTokenizationForm.getScreenWidth());
        ingenicoHostedTokenizationData.setTimezoneOffsetUtcMinutes(ingenicoHostedTokenizationForm.getTimezoneOffset());

        ingenicoHostedTokenizationData.setAcceptHeader(request.getHeader(ACCEPT));
        ingenicoHostedTokenizationData.setUserAgent(request.getHeader(USER_AGENT));
        ingenicoHostedTokenizationData.setLocale(request.getLocale().toString());
        ingenicoHostedTokenizationData.setIpAddress(getIpAddress(request));
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
