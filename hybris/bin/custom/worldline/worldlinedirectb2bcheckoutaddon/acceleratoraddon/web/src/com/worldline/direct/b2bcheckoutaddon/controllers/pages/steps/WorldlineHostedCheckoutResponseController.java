package com.worldline.direct.b2bcheckoutaddon.controllers.pages.steps;

import com.worldline.direct.b2bcheckoutaddon.controllers.WorldlineWebConstants;
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
import de.hybris.platform.commercefacades.order.OrderFacade;
import de.hybris.platform.commercefacades.order.data.AbstractOrderData;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commerceservices.strategies.CheckoutCustomerStrategy;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;

@Controller
@RequestMapping(value = WorldlineWebConstants.URL.Checkout.Payment.HOP.root)
public class WorldlineHostedCheckoutResponseController extends AbstractCheckoutController {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldlineHostedCheckoutResponseController.class);
    private static final String ORDER_CODE_PATH_VARIABLE_PATTERN = "{orderCode:.*}";
    private static final String ORDER_CONFIRMATION_PATH_VARIABLE = "/{orderType:.*}";
    private static final String REDIRECT_URL_REPLENISHMENT_CONFIRMATION = REDIRECT_PREFIX
            + "/checkout/replenishment/confirmation/";

    @Resource(name = "worldlineCheckoutFacade")
    private WorldlineCheckoutFacade worldlineCheckoutFacade;

    @Resource(name = "worldlineRecurringCheckoutFacade")
    private WorldlineRecurringCheckoutFacade worldlineRecurringCheckoutFacade;

    @Resource(name = "orderFacade")
    private OrderFacade orderFacade;

    @Resource(name = "checkoutCustomerStrategy")
    private CheckoutCustomerStrategy checkoutCustomerStrategy;

    @Resource(name = "worldlineCustomerAccountFacade")
    private WorldlineCustomerAccountFacade worldlineCustomerAccountFacade;

    @Resource(name = "worldlineConfigurationService")
    private WorldlineConfigurationService worldlineConfigurationService;

    @RequireHardLogIn
    @RequestMapping(value = ORDER_CONFIRMATION_PATH_VARIABLE + WorldlineWebConstants.URL.Checkout.Payment.HOP.handleResponse + ORDER_CODE_PATH_VARIABLE_PATTERN, method = {RequestMethod.POST, RequestMethod.GET})
    public String handleHostedCheckoutPaymentResponse(@PathVariable(value = "orderCode") final String orderCode,
                                                      @PathVariable(value = "orderType") final OrderType orderType,
                                                      @RequestParam(value = "RETURNMAC") final String returnMAC,
                                                      @RequestParam(value = "hostedCheckoutId") final String hostedCheckoutId,
                                                      final RedirectAttributes redirectAttributes) throws InvalidCartException {

        AbstractOrderData orderDetails;
        WorldlineConfigurationModel currentWorldlineConfiguration = worldlineConfigurationService.getCurrentWorldlineConfiguration();
        try {
            switch (orderType) {
                case PLACE_ORDER:
                    orderDetails = orderFacade.getOrderDetailsForCode(orderCode);
                    break;
                case SCHEDULE_REPLENISHMENT_ORDER:
                default:
                    orderDetails = orderFacade.getOrderDetailsForCode(orderCode);
                    break;
            }

        } catch (final UnknownIdentifierException e) {
            LOGGER.warn("[WORLDLINE] Attempted to handle hosted checkout payment on an order that does not exist. Redirect to cart page.");
            return REDIRECT_PREFIX + "/cart";
        }

        try {
            worldlineCheckoutFacade.validateReturnMAC(orderDetails, returnMAC);
            switch (orderType) {
                case PLACE_ORDER:
                    if (orderDetails instanceof OrderData) {
                        worldlineCheckoutFacade.authorisePaymentForHostedCheckout(orderDetails.getCode(), hostedCheckoutId, Boolean.FALSE);
                    }
                    break;
                case SCHEDULE_REPLENISHMENT_ORDER:
                    orderDetails = worldlineRecurringCheckoutFacade.authorisePaymentForImmediateReplenishmentHostedCheckout(orderDetails.getCode(), hostedCheckoutId);
                    break;
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
