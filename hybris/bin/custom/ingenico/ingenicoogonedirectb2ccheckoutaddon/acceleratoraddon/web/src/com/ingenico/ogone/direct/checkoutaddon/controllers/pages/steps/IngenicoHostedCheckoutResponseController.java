package com.ingenico.ogone.direct.checkoutaddon.controllers.pages.steps;

import javax.annotation.Resource;

import de.hybris.platform.acceleratorstorefrontcommons.annotations.RequireHardLogIn;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.pages.AbstractCheckoutController;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.util.GlobalMessages;
import de.hybris.platform.commercefacades.order.OrderFacade;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commerceservices.strategies.CheckoutCustomerStrategy;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;

import org.apache.commons.lang.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ingenico.ogone.direct.checkoutaddon.controllers.IngenicoWebConstants;
import com.ingenico.ogone.direct.checkoutaddon.controllers.IngenicoWebConstants.URL.Checkout.Payment.HOP;
import com.ingenico.ogone.direct.exception.IngenicoNonAuthorizedPaymentException;
import com.ingenico.ogone.direct.exception.IngenicoNonValidReturnMACException;
import com.ingenico.ogone.direct.facade.IngenicoCheckoutFacade;

@Controller
@RequestMapping(value = HOP.root)
public class IngenicoHostedCheckoutResponseController extends AbstractCheckoutController {
    private static final Logger LOGGER = LoggerFactory.getLogger(IngenicoHostedCheckoutResponseController.class);
    private static final String ORDER_CODE_PATH_VARIABLE_PATTERN = "{orderCode:.*}";

    @Resource(name = "ingenicoCheckoutFacade")
    private IngenicoCheckoutFacade ingenicoCheckoutFacade;

    @Resource(name = "orderFacade")
    private OrderFacade orderFacade;

    @Resource(name = "checkoutCustomerStrategy")
    private CheckoutCustomerStrategy checkoutCustomerStrategy;

    @RequireHardLogIn
    @RequestMapping(value = HOP.handleResponse + ORDER_CODE_PATH_VARIABLE_PATTERN, method = {RequestMethod.POST, RequestMethod.GET})
    public String handleHostedCheckoutPaymentResponse(@PathVariable(value = "orderCode") final String orderCode,
                                                      @RequestParam(value = "RETURNMAC", required = true) final String returnMAC,
                                                      @RequestParam(value = "hostedCheckoutId", required = true) final String hostedCheckoutId,
                                                      final Model model,
                                                      final RedirectAttributes redirectAttributes) throws InvalidCartException {

        final OrderData orderDetails;
        try {
            orderDetails = orderFacade.getOrderDetailsForCode(orderCode);
        } catch (final UnknownIdentifierException e) {
            LOGGER.warn("[INGENICO] Attempted to handle hosted checkout payment on an order that does not exist. Redirect to cart page.");
            return REDIRECT_PREFIX + "/cart";
        }

        try {
            ingenicoCheckoutFacade.validateReturnMAC(orderDetails, returnMAC);
            final OrderData orderData = ingenicoCheckoutFacade.authorisePaymentForHostedCheckout(orderDetails.getCode(), hostedCheckoutId);
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
                case IN_PROGRESS:
                    LOGGER.warn("[INGENICO] HostedCheckout is still in progress");
                    break;
            }
        } catch (IngenicoNonValidReturnMACException e) {
            LOGGER.debug("[ INGENICO ] invalid returnMAC:{}", returnMAC);
            return REDIRECT_PREFIX + "/cart";
        }

        return REDIRECT_PREFIX + "/cart";

    }

    protected String redirectToOrderConfirmationPage(OrderData orderData) {
        return REDIRECT_PREFIX + IngenicoWebConstants.URL.Checkout.OrderConfirmation.root + getOrderCode(orderData);
    }

    protected String getOrderCode(OrderData orderData) {
        return checkoutCustomerStrategy.isAnonymousCheckout() ? orderData.getGuid() : orderData.getCode();
    }

}
