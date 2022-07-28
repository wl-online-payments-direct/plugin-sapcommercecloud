package com.worldline.direct.b2bcheckoutaddon.utils;

import com.onlinepayments.domain.CreateHostedCheckoutResponse;
import com.worldline.direct.b2bcheckoutaddon.controllers.WorldlineWebConstants;
import com.worldline.direct.exception.WorldlineNonAuthorizedPaymentException;
import com.worldline.direct.facade.WorldlineCheckoutFacade;
import com.worldline.direct.facade.WorldlineRecurringCheckoutFacade;
import com.worldline.direct.order.data.BrowserData;
import com.worldline.direct.order.data.WorldlineHostedTokenizationData;
import de.hybris.platform.acceleratorservices.urlresolver.SiteBaseUrlResolutionService;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.util.GlobalMessages;
import de.hybris.platform.b2bacceleratorfacades.order.data.ScheduledCartData;
import de.hybris.platform.commercefacades.order.CheckoutFacade;
import de.hybris.platform.commercefacades.order.data.AbstractOrderData;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commerceservices.strategies.CheckoutCustomerStrategy;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.site.BaseSiteService;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;

import static com.worldline.direct.populator.hostedcheckout.WorldlineHostedCheckoutBasicPopulator.HOSTED_CHECKOUT_RETURN_URL;
import static com.worldline.direct.populator.hostedtokenization.WorldlineHostedTokenizationBasicPopulator.HOSTED_TOKENIZATION_RETURN_URL;
import static de.hybris.platform.acceleratorstorefrontcommons.controllers.AbstractController.REDIRECT_PREFIX;

@Component("worldlinePlaceOrderUtils")
public class WorldlinePlaceOrderUtils {

    private static final String REDIRECT_URL_REPLENISHMENT_CONFIRMATION = REDIRECT_PREFIX
            + "/checkout/replenishment/confirmation/";

    @Resource(name = "checkoutFacade")
    private CheckoutFacade checkoutFacade;
    @Resource(name = "worldlineCheckoutFacade")
    private WorldlineCheckoutFacade worldlineCheckoutFacade;
    @Resource(name = "worldlineRecurringCheckoutFacade")
    private WorldlineRecurringCheckoutFacade worldlineRecurringCheckoutFacade;
    @Resource(name = "sessionService")
    private SessionService sessionService;
    @Resource(name = "checkoutCustomerStrategy")
    private CheckoutCustomerStrategy checkoutCustomerStrategy;
    @Resource(name = "siteBaseUrlResolutionService")
    private SiteBaseUrlResolutionService siteBaseUrlResolutionService;
    @Resource(name = "baseSiteService")
    private BaseSiteService baseSiteService;

    public String submiOrder(AbstractOrderData abstractOrderData, BrowserData browserData, RedirectAttributes redirectAttributes) throws InvalidCartException {
        final CartData cartData = checkoutFacade.getCheckoutCart();
        switch (cartData.getWorldlinePaymentInfo().getWorldlineCheckoutType()) {
            case HOSTED_CHECKOUT:

                storeHOPReturnUrlInSession(getOrderCode(abstractOrderData), false);
                CreateHostedCheckoutResponse hostedCheckoutResponse = worldlineCheckoutFacade.createHostedCheckout(abstractOrderData.getCode(), browserData);
                return REDIRECT_PREFIX + hostedCheckoutResponse.getPartialRedirectUrl();
            case HOSTED_TOKENIZATION:
                try {

                    final String hostedTokenizationId = abstractOrderData.getWorldlinePaymentInfo().getHostedTokenizationId();
                    WorldlineHostedTokenizationData worldlineHostedTokenizationData = new WorldlineHostedTokenizationData();
                    worldlineHostedTokenizationData.setBrowserData(browserData);
                    worldlineHostedTokenizationData.setHostedTokenizationId(hostedTokenizationId);
                    worldlineHostedTokenizationData.setReturnUrl(sessionService.getAttribute(HOSTED_TOKENIZATION_RETURN_URL));
                    storeHTPReturnUrlInSession(getOrderCode(abstractOrderData));
                    worldlineCheckoutFacade.authorisePaymentForHostedTokenization(abstractOrderData.getCode(), worldlineHostedTokenizationData);
                    return redirectToOrderConfirmationPage(abstractOrderData);
                } catch (WorldlineNonAuthorizedPaymentException e) {
                    switch (e.getReason()) {
                        case NEED_3DS:
                            return REDIRECT_PREFIX + e.getMerchantAction().getRedirectData().getRedirectURL();
                        case REJECTED:
                            GlobalMessages.addFlashMessage(redirectAttributes,
                                    GlobalMessages.ERROR_MESSAGES_HOLDER,
                                    "checkout.error.payment.rejected");
                            return REDIRECT_PREFIX +
                                    WorldlineWebConstants.URL.Checkout.Payment.root +
                                    WorldlineWebConstants.URL.Checkout.Payment.select;
                        case CANCELLED:
                            GlobalMessages.addFlashMessage(redirectAttributes,
                                    GlobalMessages.INFO_MESSAGES_HOLDER,
                                    "checkout.error.payment.cancelled");
                            return REDIRECT_PREFIX +
                                    WorldlineWebConstants.URL.Checkout.Payment.root +
                                    WorldlineWebConstants.URL.Checkout.Payment.select;
                    }
                }
            default:
                break;
        }
        return StringUtils.EMPTY;


    }


    public String submitReplenishmentOrder(AbstractOrderData abstractOrderData, BrowserData browserData, RedirectAttributes redirectAttributes) throws InvalidCartException {
        final CartData cartData = checkoutFacade.getCheckoutCart();
        switch (cartData.getWorldlinePaymentInfo().getWorldlineCheckoutType()) {
            case HOSTED_CHECKOUT:

                storeHOPReturnUrlInSession(((ScheduledCartData) abstractOrderData).getJobCode(), true);
                CreateHostedCheckoutResponse hostedCheckoutResponse = worldlineRecurringCheckoutFacade.createReplenishmentHostedCheckout(((ScheduledCartData) abstractOrderData).getJobCode(), browserData);
                return REDIRECT_PREFIX + hostedCheckoutResponse.getPartialRedirectUrl();
            case HOSTED_TOKENIZATION:
                throw new UnsupportedOperationException();
        }
        return StringUtils.EMPTY;
    }

    private void storeHOPReturnUrlInSession(String code, Boolean isRecurring) {
        final String returnUrl = siteBaseUrlResolutionService.getWebsiteUrlForSite(baseSiteService.getCurrentBaseSite(),
                true, WorldlineWebConstants.URL.Checkout.Payment.HOP.root + "/" + (isRecurring ? WorldlineWebConstants.URL.Checkout.Payment.HOP.Option.replenishment : WorldlineWebConstants.URL.Checkout.Payment.HOP.Option.order) +
                        WorldlineWebConstants.URL.Checkout.Payment.HOP.handleResponse + code);
        sessionService.setAttribute(HOSTED_CHECKOUT_RETURN_URL, returnUrl);
    }

    private void storeHTPReturnUrlInSession(String code) {
        final String returnUrl = siteBaseUrlResolutionService.getWebsiteUrlForSite(baseSiteService.getCurrentBaseSite(),
                true, WorldlineWebConstants.URL.Checkout.Payment.HTP.root + "/" +
                        WorldlineWebConstants.URL.Checkout.Payment.HTP.handleResponse + code);
        sessionService.setAttribute(HOSTED_TOKENIZATION_RETURN_URL, returnUrl);
    }

    protected String getOrderCode(AbstractOrderData orderData) {
        return checkoutCustomerStrategy.isAnonymousCheckout() ? orderData.getGuid() : orderData.getCode();
    }

    protected String redirectToOrderConfirmationPage(AbstractOrderData orderData) {
        return REDIRECT_PREFIX + WorldlineWebConstants.URL.Checkout.OrderConfirmation.root + getOrderCode(orderData);
    }

    protected String redirectToReplenishmentConfirmationPage(AbstractOrderData orderData) {
        return REDIRECT_URL_REPLENISHMENT_CONFIRMATION + ((ScheduledCartData) orderData).getJobCode();
    }


}
