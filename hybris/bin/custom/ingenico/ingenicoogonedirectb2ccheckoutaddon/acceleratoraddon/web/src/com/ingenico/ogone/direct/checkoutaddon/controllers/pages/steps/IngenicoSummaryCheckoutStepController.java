package com.ingenico.ogone.direct.checkoutaddon.controllers.pages.steps;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

import de.hybris.platform.acceleratorservices.urlresolver.SiteBaseUrlResolutionService;
import de.hybris.platform.acceleratorstorefrontcommons.annotations.PreValidateCheckoutStep;
import de.hybris.platform.acceleratorstorefrontcommons.annotations.RequireHardLogIn;
import de.hybris.platform.acceleratorstorefrontcommons.checkout.steps.CheckoutStep;
import de.hybris.platform.acceleratorstorefrontcommons.constants.WebConstants;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.pages.checkout.steps.AbstractCheckoutStepController;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.util.GlobalMessages;
import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.cms2.model.pages.ContentPageModel;
import de.hybris.platform.commercefacades.order.CheckoutFacade;
import de.hybris.platform.commercefacades.order.OrderFacade;
import de.hybris.platform.commercefacades.order.data.AbstractOrderData;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commercefacades.order.data.OrderEntryData;
import de.hybris.platform.commercefacades.product.ProductOption;
import de.hybris.platform.commercefacades.product.data.ProductData;
import de.hybris.platform.commerceservices.order.CommerceCartModificationException;
import de.hybris.platform.commerceservices.strategies.CheckoutCustomerStrategy;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.servicelayer.config.ConfigurationService;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ingenico.direct.domain.CreateHostedCheckoutResponse;
import com.ingenico.direct.domain.PaymentProduct;
import com.ingenico.ogone.direct.checkoutaddon.controllers.IngenicoWebConstants;
import com.ingenico.ogone.direct.checkoutaddon.controllers.IngenicoWebConstants.URL.Checkout.Summary;
import com.ingenico.ogone.direct.checkoutaddon.forms.IngenicoPlaceOrderForm;
import com.ingenico.ogone.direct.constants.IngenicoCheckoutConstants;
import com.ingenico.ogone.direct.exception.IngenicoNonAuthorizedPaymentException;
import com.ingenico.ogone.direct.facade.IngenicoCheckoutFacade;
import com.ingenico.ogone.direct.order.data.BrowserData;
import com.ingenico.ogone.direct.order.data.IngenicoHostedTokenizationData;

@Controller
@RequestMapping(value = Summary.root)
public class IngenicoSummaryCheckoutStepController extends AbstractCheckoutStepController {
    private static final Logger LOGGER = Logger.getLogger(IngenicoSummaryCheckoutStepController.class);

    private final static String SUMMARY = "summary";
    private static final String ACCEPT = "accept";
    private static final String USER_AGENT = "user-agent";
    private static final String X_FORWARDED_FOR = "X-FORWARDED-FOR";

    @Resource(name = "orderFacade")
    private OrderFacade orderFacade;

    @Resource(name = "ingenicoCheckoutFacade")
    private IngenicoCheckoutFacade ingenicoCheckoutFacade;

    @Resource(name = "configurationService")
    private ConfigurationService configurationService;

    @Resource(name = "siteBaseUrlResolutionService")
    private SiteBaseUrlResolutionService siteBaseUrlResolutionService;

    @Resource(name = "checkoutCustomerStrategy")
    private CheckoutCustomerStrategy checkoutCustomerStrategy;

    @Resource(name = "ingenicoExtendedCheckoutFacade")
    private CheckoutFacade extendedCheckoutFacade;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @RequestMapping(value = Summary.view, method = RequestMethod.GET)
    @RequireHardLogIn
    @Override
    @PreValidateCheckoutStep(checkoutStep = SUMMARY)
    public String enterStep(final Model model, final RedirectAttributes redirectAttributes) throws CMSItemNotFoundException, // NOSONAR
            CommerceCartModificationException {

        final CartData cartData = getCheckoutFacade().getCheckoutCart();
        if (cartData.getEntries() != null && !cartData.getEntries().isEmpty()) {
            for (final OrderEntryData entry : cartData.getEntries()) {
                final String productCode = entry.getProduct().getCode();
                final ProductData product = getProductFacade().getProductForCodeAndOptions(productCode, Arrays.asList(
                        ProductOption.BASIC, ProductOption.PRICE, ProductOption.VARIANT_MATRIX_BASE, ProductOption.PRICE_RANGE));
                entry.setProduct(product);
            }
        }

        PaymentProduct paymentProduct;
        if (cartData.getIngenicoPaymentInfo().getId() == null) {
            GlobalMessages.addFlashMessage(redirectAttributes, GlobalMessages.INFO_MESSAGES_HOLDER,
                    "checkout.multi.paymentDetails.notprovided");
            return back(redirectAttributes);
        }

        paymentProduct = ingenicoCheckoutFacade.getPaymentMethodById(cartData.getIngenicoPaymentInfo().getId());
        // TODO TRY CATCH  "not found paymentproduct ingenico exception" instead of == null
        if (paymentProduct == null) {
            GlobalMessages.addFlashMessage(redirectAttributes, GlobalMessages.INFO_MESSAGES_HOLDER,
                    "checkout.multi.paymentDetails.notprovided");
            return back(redirectAttributes);
        }

        model.addAttribute("cartData", cartData);
        model.addAttribute("allItems", cartData.getEntries());
        model.addAttribute("deliveryAddress", cartData.getDeliveryAddress());
        model.addAttribute("deliveryMode", cartData.getDeliveryMode());
        model.addAttribute("paymentProduct", paymentProduct);

        model.addAttribute(new IngenicoPlaceOrderForm());

        final ContentPageModel multiCheckoutSummaryPage = getContentPageForLabelOrId(MULTI_CHECKOUT_SUMMARY_CMS_PAGE_LABEL);
        storeCmsPageInModel(model, multiCheckoutSummaryPage);
        setUpMetaDataForContentPage(model, multiCheckoutSummaryPage);

        model.addAttribute(WebConstants.BREADCRUMBS_KEY, getResourceBreadcrumbBuilder().getBreadcrumbs("checkout.multi.summary.breadcrumb"));
        model.addAttribute("metaRobots", "noindex,nofollow");
        setCheckoutStepLinksForModel(model, getCheckoutStep());

        return IngenicoCheckoutConstants.Views.Pages.MultiStepCheckout.ingenicoCheckoutSummaryPage;
    }

    @RequestMapping(value = Summary.placeOrder)
    @RequireHardLogIn
    public String placeOrder(@ModelAttribute("ingenicoPlaceOrderForm") final IngenicoPlaceOrderForm ingenicoPlaceOrderForm,
                             final Model model,
                             final HttpServletRequest request,
                             final RedirectAttributes redirectModel) throws CMSItemNotFoundException, CommerceCartModificationException, InvalidCartException {

        if (validateOrderForm(ingenicoPlaceOrderForm, model)) {
            return enterStep(model, redirectModel);
        }

        //Validate the cart
        if (validateCart(redirectModel)) {
            // Invalid cart. Bounce back to the cart page.
            return REDIRECT_PREFIX + "/cart";
        }

        final CartData cartData = getCheckoutFacade().getCheckoutCart();
        final BrowserData browserData = fillBrowserData(request, ingenicoPlaceOrderForm);
        switch (cartData.getIngenicoPaymentInfo().getIngenicoCheckoutType()) {
            case HOSTED_CHECKOUT:
                final OrderData orderHOP = extendedCheckoutFacade.placeOrder();
                storeHOPReturnUrlInSession(getOrderCode(orderHOP));
                CreateHostedCheckoutResponse hostedCheckoutResponse = ingenicoCheckoutFacade.createHostedCheckout(orderHOP.getCode(), browserData);
                return REDIRECT_PREFIX + hostedCheckoutResponse.getPartialRedirectUrl();
            case HOSTED_TOKENIZATION:
                try {
                    OrderData orderHTP = extendedCheckoutFacade.placeOrder();
                    storeHTPReturnUrlInSession(getOrderCode(orderHTP));
                    final String hostedTokenizationId = orderHTP.getIngenicoPaymentInfo().getHostedTokenizationId();
                    IngenicoHostedTokenizationData ingenicoHostedTokenizationData = new IngenicoHostedTokenizationData();
                    ingenicoHostedTokenizationData.setBrowserData(browserData);
                    ingenicoHostedTokenizationData.setHostedTokenizationId(hostedTokenizationId);

                    orderHTP = ingenicoCheckoutFacade.authorisePaymentForHostedTokenization(orderHTP.getCode(), ingenicoHostedTokenizationData);
                    return redirectToOrderConfirmationPage(orderHTP);
                } catch (IngenicoNonAuthorizedPaymentException e) {
                    switch (e.getReason()) {
                        case NEED_3DS:
                            return REDIRECT_PREFIX + e.getMerchantAction().getRedirectData().getRedirectURL();
                        case REJECTED:
                            GlobalMessages.addFlashMessage(redirectModel,
                                    GlobalMessages.ERROR_MESSAGES_HOLDER,
                                    "checkout.error.payment.rejected");
                            return REDIRECT_PREFIX + IngenicoWebConstants.URL.Checkout.Payment.select;
                        case CANCELLED:
                            GlobalMessages.addFlashMessage(redirectModel,
                                    GlobalMessages.INFO_MESSAGES_HOLDER,
                                    "checkout.error.payment.cancelled");
                            return REDIRECT_PREFIX + IngenicoWebConstants.URL.Checkout.Payment.select;
                    }
                }
            default:
                break;
        }

        GlobalMessages.addErrorMessage(model, "checkout.error.checkoutType.unknown");
        return enterStep(model, redirectModel);
    }

    private void storeHOPReturnUrlInSession(String code) {
        final String returnUrl = siteBaseUrlResolutionService.getWebsiteUrlForSite(getBaseSiteService().getCurrentBaseSite(),
                true, IngenicoWebConstants.URL.Checkout.Payment.HOP.root +
                        IngenicoWebConstants.URL.Checkout.Payment.HOP.handleResponse + code);
        getSessionService().setAttribute("hostedCheckoutReturnUrl", returnUrl);
    }

    private void storeHTPReturnUrlInSession(String code) {
        final String returnUrl = siteBaseUrlResolutionService.getWebsiteUrlForSite(getBaseSiteService().getCurrentBaseSite(),
                true, IngenicoWebConstants.URL.Checkout.Payment.HTP.root +
                        IngenicoWebConstants.URL.Checkout.Payment.HTP.handleResponse + code);
        getSessionService().setAttribute("hostedTokenizationReturnUrl", returnUrl);
    }

    protected boolean validateOrderForm(final IngenicoPlaceOrderForm ingenicoPlaceOrderForm, final Model model) {
        boolean invalid = false;

        if (getCheckoutFlowFacade().hasNoDeliveryAddress()) {
            GlobalMessages.addErrorMessage(model, "checkout.deliveryAddress.notSelected");
            invalid = true;
        }

        if (getCheckoutFlowFacade().hasNoDeliveryMode()) {
            GlobalMessages.addErrorMessage(model, "checkout.deliveryMethod.notSelected");
            invalid = true;
        }

        if (getCheckoutFlowFacade().hasNoPaymentInfo()) {
            GlobalMessages.addErrorMessage(model, "checkout.paymentMethod.notSelected");
            invalid = true;
        }


        if (!ingenicoPlaceOrderForm.isTermsCheck()) {
            GlobalMessages.addErrorMessage(model, "checkout.error.terms.not.accepted");
            invalid = true;
            return invalid;
        }
        final CartData cartData = getCheckoutFacade().getCheckoutCart();

        if (!getCheckoutFacade().containsTaxValues()) {
            LOGGER.error(String.format(
                    "Cart %s does not have any tax values, which means the tax cacluation was not properly done, placement of order can't continue",
                    cartData.getCode()));
            GlobalMessages.addErrorMessage(model, "checkout.error.tax.missing");
            invalid = true;
        }

        if (!cartData.isCalculated()) {
            LOGGER.error(
                    String.format("Cart %s has a calculated flag of FALSE, placement of order can't continue", cartData.getCode()));
            GlobalMessages.addErrorMessage(model, "checkout.error.cart.notcalculated");
            invalid = true;
        }

        return invalid;
    }

    protected String getOrderCode(AbstractOrderData orderData) {
        return checkoutCustomerStrategy.isAnonymousCheckout() ? orderData.getGuid() : orderData.getCode();
    }

    @Override
    protected String redirectToOrderConfirmationPage(OrderData orderData) {
        return REDIRECT_PREFIX + IngenicoWebConstants.URL.Checkout.OrderConfirmation.root + getOrderCode(orderData);
    }

    private BrowserData fillBrowserData(HttpServletRequest request, IngenicoPlaceOrderForm ingenicoPlaceOrderForm) {

        BrowserData browserData = new BrowserData();
        browserData.setColorDepth(ingenicoPlaceOrderForm.getColorDepth());
        browserData.setNavigatorJavaEnabled(ingenicoPlaceOrderForm.getNavigatorJavaEnabled());
        browserData.setNavigatorJavaScriptEnabled(ingenicoPlaceOrderForm.getNavigatorJavaScriptEnabled());
        browserData.setScreenHeight(ingenicoPlaceOrderForm.getScreenHeight());
        browserData.setScreenWidth(ingenicoPlaceOrderForm.getScreenWidth());
        browserData.setTimezoneOffsetUtcMinutes(ingenicoPlaceOrderForm.getTimezoneOffset());

        browserData.setAcceptHeader(request.getHeader(ACCEPT));
        browserData.setUserAgent(request.getHeader(USER_AGENT));
        browserData.setLocale(request.getLocale().toString());
        browserData.setIpAddress(getIpAddress(request));

        return browserData;
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
        return getCheckoutStep(SUMMARY);
    }
}
