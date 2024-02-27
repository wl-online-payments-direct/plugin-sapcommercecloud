package com.worldline.direct.checkoutaddon.controllers.pages.checkout;

import com.worldline.direct.checkoutaddon.controllers.WorldlineWebConstants;
import com.worldline.direct.enums.OrderType;
import com.worldline.direct.exception.WorldlineNonAuthorizedPaymentException;
import com.worldline.direct.exception.WorldlineNonValidReturnMACException;
import com.worldline.direct.facade.WorldlineCheckoutFacade;
import com.worldline.direct.facade.WorldlineCustomerAccountFacade;
import com.worldline.direct.facade.WorldlineRecurringCheckoutFacade;
import com.worldline.direct.model.WorldlineConfigurationModel;
import com.worldline.direct.service.WorldlineConfigurationService;
import de.hybris.platform.acceleratorstorefrontcommons.annotations.RequireHardLogIn;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.pages.AbstractCheckoutController;
import de.hybris.platform.acceleratorstorefrontcommons.controllers.util.GlobalMessages;
import de.hybris.platform.b2bacceleratorfacades.order.data.ScheduledCartData;
import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.commercefacades.order.OrderFacade;
import de.hybris.platform.commercefacades.order.data.AbstractOrderData;
import de.hybris.platform.commerceservices.order.CommerceCartModificationException;
import de.hybris.platform.commerceservices.strategies.CheckoutCustomerStrategy;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping(value = WorldlineWebConstants.URL.Checkout.Payment.HTP.root)
public class WorldlineTokenizationCheckoutResponseController extends AbstractCheckoutController {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldlineTokenizationCheckoutResponseController.class);
    private static final String ORDER_CODE_PATH_VARIABLE_PATTERN = "{orderCode:.*}";

    private static final String ORDER_CONFIRMATION_PATH_VARIABLE = "/{orderType:.*}";

    private static final String REDIRECT_URL_REPLENISHMENT_CONFIRMATION = REDIRECT_PREFIX
          + "/checkout/replenishment/confirmation/";

    @Resource(name = "worldlineCheckoutFacade")
    private WorldlineCheckoutFacade worldlineCheckoutFacade;

    @Resource(name = "checkoutCustomerStrategy")
    private CheckoutCustomerStrategy checkoutCustomerStrategy;

    @Resource(name = "orderFacade")
    private OrderFacade orderFacade;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Resource(name = "worldlineConfigurationService")
    private WorldlineConfigurationService worldlineConfigurationService;

    @Resource(name = "worldlineCustomerAccountFacade")
    private WorldlineCustomerAccountFacade worldlineCustomerAccountFacade;

    @Resource(name = "worldlineRecurringCheckoutFacade")
    private WorldlineRecurringCheckoutFacade worldlineRecurringCheckoutFacade;

    @RequestMapping(value = ORDER_CONFIRMATION_PATH_VARIABLE + WorldlineWebConstants.URL.Checkout.Payment.HTP.handleResponse + ORDER_CODE_PATH_VARIABLE_PATTERN, method = RequestMethod.GET)
    @RequireHardLogIn
    public String handle3ds(@PathVariable(value = "orderCode") final String orderCode,
                            @PathVariable(value = "orderType") final OrderType orderType,
                            @RequestParam(value = "REF", required = true) final String ref,
                            @RequestParam(value = "RETURNMAC", required = true) final String returnMAC,
                            @RequestParam(value = "paymentId", required = true) final String paymentId,
                            final Model model,
                            final HttpServletRequest request,
                            final RedirectAttributes redirectAttributes) throws CMSItemNotFoundException, CommerceCartModificationException, InvalidCartException {
        AbstractOrderData orderDetails;
        try {
            orderDetails = orderFacade.getOrderDetailsForCode(orderCode);
        } catch (final UnknownIdentifierException e) {
            LOGGER.warn("[WORLDLIINE] Attempted to handle hosted Tokenization payment on an order that does not exist. Redirect to cart page.");
            return REDIRECT_PREFIX + "/cart";
        }
        try {
            worldlineCheckoutFacade.validateReturnMAC(orderDetails, returnMAC);
            if (orderType.equals(OrderType.PLACE_ORDER)) {
                worldlineCheckoutFacade.handle3dsResponse(orderDetails.getCode(), paymentId, Boolean.FALSE);
            } else {
                orderDetails = worldlineRecurringCheckoutFacade.handleRecurring3DsHostedTokenizationPayment(orderCode, paymentId);
            }

            return redirectToOrderConfirmationPage(orderDetails);
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
            }
            return REDIRECT_PREFIX + WorldlineWebConstants.URL.Checkout.Payment.select;
        } catch (WorldlineNonValidReturnMACException e) {
            LOGGER.debug("[ WORLDLINE ] invalid returnMAC:{}", returnMAC);
            return REDIRECT_PREFIX + "/cart";
        }
    }

    protected String redirectToOrderConfirmationPage(AbstractOrderData abstractOrderData) {
        if (abstractOrderData instanceof ScheduledCartData) {
            return REDIRECT_URL_REPLENISHMENT_CONFIRMATION + ((ScheduledCartData) abstractOrderData).getJobCode();
        } else {
            return REDIRECT_PREFIX + WorldlineWebConstants.URL.Checkout.OrderConfirmation.root + getOrderCode(abstractOrderData);
        }
    }

    protected String getOrderCode(AbstractOrderData orderData) {
        return checkoutCustomerStrategy.isAnonymousCheckout() ? orderData.getGuid() : orderData.getCode();
    }

}
