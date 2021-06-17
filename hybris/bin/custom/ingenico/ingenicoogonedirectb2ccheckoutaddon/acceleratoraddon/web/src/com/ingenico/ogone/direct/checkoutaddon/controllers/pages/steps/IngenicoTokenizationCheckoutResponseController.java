package com.ingenico.ogone.direct.checkoutaddon.controllers.pages.steps;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import de.hybris.platform.acceleratorstorefrontcommons.annotations.RequireHardLogIn;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.pages.AbstractCheckoutController;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.util.GlobalMessages;
import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commerceservices.order.CommerceCartModificationException;
import de.hybris.platform.commerceservices.strategies.CheckoutCustomerStrategy;
import de.hybris.platform.order.InvalidCartException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ingenico.ogone.direct.checkoutaddon.controllers.IngenicoWebConstants;
import com.ingenico.ogone.direct.checkoutaddon.controllers.IngenicoWebConstants.URL.Checkout.Payment.HTP;
import com.ingenico.ogone.direct.exception.IngenicoNonAuthorizedPaymentException;
import com.ingenico.ogone.direct.exception.IngenicoNonValidReturnMACException;
import com.ingenico.ogone.direct.facade.IngenicoCheckoutFacade;

@Controller
@RequestMapping(value = HTP.root)
public class IngenicoTokenizationCheckoutResponseController extends AbstractCheckoutController {
    private static final Logger LOGGER = LoggerFactory.getLogger(IngenicoTokenizationCheckoutResponseController.class);


    @Resource(name = "ingenicoCheckoutFacade")
    private IngenicoCheckoutFacade ingenicoCheckoutFacade;

    @Resource(name = "checkoutCustomerStrategy")
    private CheckoutCustomerStrategy checkoutCustomerStrategy;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @RequestMapping(value = HTP.handleResponse, method = RequestMethod.GET)
    @RequireHardLogIn
    public String handle3ds(@RequestParam(value = "REF", required = true) final String ref,
                            @RequestParam(value = "RETURNMAC", required = true) final String returnMAC,
                            @RequestParam(value = "paymentId", required = true) final String paymentId,
                            final Model model,
                            final HttpServletRequest request,
                            final RedirectAttributes redirectAttributes) throws CMSItemNotFoundException, CommerceCartModificationException, InvalidCartException {
        try {
            ingenicoCheckoutFacade.validateReturnMAC(returnMAC);
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
        } catch (IngenicoNonValidReturnMACException e) {
            LOGGER.debug("[ INGENICO ] invalid returnMAC:{}", returnMAC);
            return REDIRECT_PREFIX + "/cart";
        }
    }

    @Override
    protected String redirectToOrderConfirmationPage(OrderData orderData) {
        return REDIRECT_PREFIX + IngenicoWebConstants.URL.Checkout.OrderConfirmation.root + getOrderCode(orderData);
    }

    protected String getOrderCode(OrderData orderData) {
        return checkoutCustomerStrategy.isAnonymousCheckout() ? orderData.getGuid() : orderData.getCode();
    }

}
