package com.ingenico.ogone.direct.checkoutaddon.checkout.steps.validation.impl;

import de.hybris.platform.acceleratorstorefrontcommons.checkout.steps.validation.AbstractCheckoutStepValidator;
import de.hybris.platform.acceleratorstorefrontcommons.checkout.steps.validation.ValidationResults;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.util.GlobalMessages;
import de.hybris.platform.commercefacades.order.data.CartData;

import org.apache.log4j.Logger;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ingenico.ogone.direct.acceleratorfacades.flow.IngenicoCheckoutFlowFacade;


public class IngenicoPaymentWithTokenCheckoutStepValidator extends AbstractCheckoutStepValidator {
    private static final Logger LOGGER = Logger.getLogger(IngenicoPaymentWithTokenCheckoutStepValidator.class);

    private IngenicoCheckoutFlowFacade ingenicoCheckoutFlowFacade;

    @Override
    public ValidationResults validateOnEnter(final RedirectAttributes redirectAttributes) {
        final ValidationResults cartResult = checkCartAndDelivery(redirectAttributes);
        if (cartResult != null) {
            return cartResult;
        }

        final ValidationResults paymentResult = checkPaymentMethodAndPickup(redirectAttributes);
        if (paymentResult != null) {
            return paymentResult;
        }

        return ValidationResults.SUCCESS;
    }

    protected ValidationResults checkPaymentMethodAndPickup(RedirectAttributes redirectAttributes) {
        if (getCheckoutFlowFacade().hasNoPaymentInfo()) {
            GlobalMessages.addFlashMessage(redirectAttributes, GlobalMessages.INFO_MESSAGES_HOLDER,
                    "checkout.multi.paymentDetails.notprovided");
            return ValidationResults.REDIRECT_TO_PAYMENT_METHOD;
        }

        if (ingenicoCheckoutFlowFacade.isNotCheckoutWithTokenization()) {
            GlobalMessages.addFlashMessage(redirectAttributes, GlobalMessages.INFO_MESSAGES_HOLDER,
                    "checkout.multi.payment.not.tokenization.flow");
            return ValidationResults.REDIRECT_TO_SUMMARY;
        }

        final CartData cartData = getCheckoutFacade().getCheckoutCart();

        if (!getCheckoutFacade().hasShippingItems()) {
            cartData.setDeliveryAddress(null);
        }

        if (!getCheckoutFacade().hasPickUpItems() && "pickup".equals(cartData.getDeliveryMode().getCode())) {
            return ValidationResults.REDIRECT_TO_DELIVERY_ADDRESS;
        }
        return null;
    }

    protected ValidationResults checkCartAndDelivery(RedirectAttributes redirectAttributes) {
        if (!getCheckoutFlowFacade().hasValidCart()) {
            LOGGER.info("Missing, empty or unsupported cart");
            return ValidationResults.REDIRECT_TO_CART;
        }

        if (getCheckoutFlowFacade().hasNoDeliveryAddress()) {
            GlobalMessages.addFlashMessage(redirectAttributes, GlobalMessages.INFO_MESSAGES_HOLDER,
                    "checkout.multi.deliveryAddress.notprovided");
            return ValidationResults.REDIRECT_TO_DELIVERY_ADDRESS;
        }

        if (getCheckoutFlowFacade().hasNoDeliveryMode()) {
            GlobalMessages.addFlashMessage(redirectAttributes, GlobalMessages.INFO_MESSAGES_HOLDER,
                    "checkout.multi.deliveryMethod.notprovided");
            return ValidationResults.REDIRECT_TO_DELIVERY_METHOD;
        }
        return null;
    }

    public void setIngenicoCheckoutFlowFacade(IngenicoCheckoutFlowFacade ingenicoCheckoutFlowFacade) {
        this.ingenicoCheckoutFlowFacade = ingenicoCheckoutFlowFacade;
    }
}
