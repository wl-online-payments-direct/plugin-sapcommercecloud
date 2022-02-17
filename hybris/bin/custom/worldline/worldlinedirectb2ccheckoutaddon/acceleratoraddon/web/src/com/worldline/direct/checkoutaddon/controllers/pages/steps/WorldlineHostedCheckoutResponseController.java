package com.worldline.direct.checkoutaddon.controllers.pages.steps;

import javax.annotation.Resource;

import de.hybris.platform.acceleratorstorefrontcommons.annotations.RequireHardLogIn;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.pages.AbstractCheckoutController;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.util.GlobalMessages;
import de.hybris.platform.commercefacades.order.OrderFacade;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commerceservices.strategies.CheckoutCustomerStrategy;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.worldline.direct.checkoutaddon.controllers.WorldlineWebConstants;
import com.worldline.direct.checkoutaddon.controllers.WorldlineWebConstants.URL.Checkout.Payment.HOP;
import com.worldline.direct.exception.WorldlineNonAuthorizedPaymentException;
import com.worldline.direct.exception.WorldlineNonValidReturnMACException;
import com.worldline.direct.facade.WorldlineCheckoutFacade;

@Controller
@RequestMapping(value = HOP.root)
public class WorldlineHostedCheckoutResponseController extends AbstractCheckoutController {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldlineHostedCheckoutResponseController.class);
    private static final String ORDER_CODE_PATH_VARIABLE_PATTERN = "{orderCode:.*}";

    @Resource(name = "worldlineCheckoutFacade")
    private WorldlineCheckoutFacade worldlineCheckoutFacade;

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
            LOGGER.warn("[WORLDLINE] Attempted to handle hosted checkout payment on an order that does not exist. Redirect to cart page.");
            return REDIRECT_PREFIX + "/cart";
        }

        try {
            worldlineCheckoutFacade.validateReturnMAC(orderDetails, returnMAC);
            final OrderData orderData = worldlineCheckoutFacade.authorisePaymentForHostedCheckout(orderDetails.getCode(), hostedCheckoutId);
            return redirectToOrderConfirmationPage(orderData);
        } catch (WorldlineNonAuthorizedPaymentException e) {
            switch (e.getReason()) {
                case CANCELLED:
                    GlobalMessages.addFlashMessage(redirectAttributes,
                            GlobalMessages.INFO_MESSAGES_HOLDER,
                            "checkout.error.payment.cancelled");
                    return REDIRECT_PREFIX +
                            WorldlineWebConstants.URL.Checkout.Payment.root +
                            WorldlineWebConstants.URL.Checkout.Payment.select;
                case REJECTED:
                    GlobalMessages.addFlashMessage(redirectAttributes,
                            GlobalMessages.ERROR_MESSAGES_HOLDER,
                            "checkout.error.payment.rejected");
                    return REDIRECT_PREFIX +
                            WorldlineWebConstants.URL.Checkout.Payment.root +
                            WorldlineWebConstants.URL.Checkout.Payment.select;
                case IN_PROGRESS:
                    LOGGER.warn("[WORLDLINE] HostedCheckout is still in progress");
                    break;
            }
        } catch (WorldlineNonValidReturnMACException e) {
            LOGGER.debug("[ WORLDLINE ] invalid returnMAC:{}", returnMAC);
            return REDIRECT_PREFIX + "/cart";
        }

        return REDIRECT_PREFIX + "/cart";

    }

    protected String redirectToOrderConfirmationPage(OrderData orderData) {
        return REDIRECT_PREFIX + WorldlineWebConstants.URL.Checkout.OrderConfirmation.root + getOrderCode(orderData);
    }

    protected String getOrderCode(OrderData orderData) {
        return checkoutCustomerStrategy.isAnonymousCheckout() ? orderData.getGuid() : orderData.getCode();
    }

}
