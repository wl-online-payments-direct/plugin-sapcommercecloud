package com.worldline.direct.occ.controllers.v2;


import com.worldline.direct.enums.OrderType;
import com.worldline.direct.exception.WorldlineNonAuthorizedPaymentException;
import com.worldline.direct.exception.WorldlineNonValidReturnMACException;
import com.worldline.direct.facade.WorldlineCheckoutFacade;
import com.worldline.direct.facade.WorldlineCustomerAccountFacade;
import com.worldline.direct.facade.WorldlineRecurringCheckoutFacade;
import de.hybris.platform.b2bacceleratorfacades.order.data.ScheduledCartData;
import de.hybris.platform.b2bwebservicescommons.dto.order.ReplenishmentOrderWsDTO;
import de.hybris.platform.commercefacades.order.OrderFacade;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commercewebservices.core.strategies.OrderCodeIdentificationStrategy;
import de.hybris.platform.commercewebservicescommons.dto.order.AbstractOrderWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.OrderWsDTO;
import de.hybris.platform.commercewebservicescommons.strategies.CartLoaderStrategy;
import de.hybris.platform.order.CartService;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.webservicescommons.mapping.DataMapper;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdAndUserIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang.BooleanUtils;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Controller
@RequestMapping(value = "/{baseSiteId}/worldline/{userId}/hosted")
@Tag(name = "Worldline Hosted Checkout And Tokenization")
public class WorldlineHostedController extends WorldlineBaseController {


    @Resource(name = "dataMapper")
    private DataMapper dataMapper;

    @Resource(name = "cartLoaderStrategy")
    private CartLoaderStrategy cartLoaderStrategy;

    @Resource(name = "orderFacade")
    private OrderFacade orderFacade;

    @Resource(name = "orderCodeIdentificationStrategy")
    private OrderCodeIdentificationStrategy orderCodeIdentificationStrategy;

    @Resource(name = "worldlineCheckoutFacade")
    private WorldlineCheckoutFacade worldlineCheckoutFacade;

    @Resource(name = "worldlineRecurringCheckoutFacade")
    private WorldlineRecurringCheckoutFacade worldlineRecurringCheckoutFacade;

    @Resource(name = "worldlineCustomerAccountFacade")
    private WorldlineCustomerAccountFacade worldlineCustomerAccountFacade;

    @Resource(name = "cartService")
    private CartService cartService;


    @Secured({ "ROLE_CUSTOMERGROUP", "ROLE_CLIENT", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT" })
    @PostMapping(value = "/{orderCode}/hostedTokenization/return")
    @ResponseBody
    @Operation(operationId = "Handle return for hostedTokenization", summary = "handle return for hostedTokenization.")
    @ApiBaseSiteIdAndUserIdParam
    public OrderWsDTO handleHostedTokenizationReturn(
            @Parameter(description = "Order GUID (Globally Unique Identifier) or order CODE", required = true)
            @PathVariable(value = "orderCode") final String orderCode,
            @RequestParam(value = "RETURNMAC", required = true) final String returnMAC,
            @RequestParam(value = "REF", required = true) final String ref,
            @RequestParam(value = "paymentId", required = true) final String paymentId,
            @Parameter(description = "Cart identifier: cart code for logged in user, cart guid for anonymous user, 'current' for the last modified cart", required = true)
            @RequestParam final String cartId,
            @ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
            throws WorldlineNonAuthorizedPaymentException, InvalidCartException, WorldlineNonValidReturnMACException {
        cartLoaderStrategy.loadCart(cartId);
        OrderData orderData;
        if (orderCodeIdentificationStrategy.isID(orderCode)) {
            orderData = orderFacade.getOrderDetailsForGUID(orderCode);
        } else {
            orderData = orderFacade.getOrderDetailsForCodeWithoutUser(orderCode);
        }

        worldlineRecurringCheckoutFacade.validateReturnMAC(orderData, returnMAC);
        worldlineRecurringCheckoutFacade.handle3dsResponse(orderData.getCode(), paymentId);
        orderData = orderFacade.getOrderDetailsForCodeWithoutUser(orderData.getCode());

        return getDataMapper().map(orderData, OrderWsDTO.class, fields);
    }


    @Secured({ "ROLE_CUSTOMERGROUP", "ROLE_CLIENT", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT" })
    @PostMapping(value = "/{orderCode}/hostedCheckout/return")
    @ResponseBody
    @Operation(operationId = "Handle return for HostedCheckout", summary = "Handle return for HostedCheckout.",
            description = "Authorizes the cart and places the order. The response contains the new order data.")
    @ApiBaseSiteIdAndUserIdParam
    public AbstractOrderWsDTO handleHostedCheckoutReturn(
            @Parameter(description = "Order GUID (Globally Unique Identifier) or order CODE", required = true)
            @PathVariable final String orderCode,
            @RequestParam(value = "RETURNMAC") final String returnMAC,
            @RequestParam(value = "hostedCheckoutId") final String hostedCheckoutId,
            @RequestParam(value = "orderType") final OrderType orderType,
            @Parameter(description = "Cart identifier: cart code for logged in user, cart guid for anonymous user, 'current' for the last modified cart", required = true)
            @RequestParam final String cartId,
            @ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
            throws WorldlineNonAuthorizedPaymentException, InvalidCartException, WorldlineNonValidReturnMACException {

        cartLoaderStrategy.loadCart(cartId);
        switch (orderType) {
            case PLACE_ORDER: {
                OrderData orderData;
                if (orderCodeIdentificationStrategy.isID(orderCode)) {
                    orderData = orderFacade.getOrderDetailsForGUID(orderCode);
                } else {
                    orderData = orderFacade.getOrderDetailsForCodeWithoutUser(orderCode);
                }
                worldlineCheckoutFacade.validateReturnMAC(orderData, returnMAC);
                worldlineCheckoutFacade.authorisePaymentForHostedCheckout(orderData.getCode(), hostedCheckoutId);
                orderData = orderFacade.getOrderDetailsForCodeWithoutUser(orderData.getCode());
                return getDataMapper().map(orderData, OrderWsDTO.class, fields);
            }
            case SCHEDULE_REPLENISHMENT_ORDER:
            default: {
                ScheduledCartData scheduledCartData;
                if (BooleanUtils.isTrue(cartService.getSessionCart().getStore().getWorldlineConfiguration().isFirstRecurringPayment())) {
                    OrderData orderData;
                    if (orderCodeIdentificationStrategy.isID(orderCode)) {
                        orderData = orderFacade.getOrderDetailsForGUID(orderCode);
                    } else {
                        orderData = orderFacade.getOrderDetailsForCodeWithoutUser(orderCode);
                    }
                    worldlineCheckoutFacade.validateReturnMAC(orderData, returnMAC);
                    scheduledCartData = worldlineRecurringCheckoutFacade.authorisePaymentForImmediateReplenishmentHostedCheckout(orderData.getCode(), hostedCheckoutId);
                } else {
                    scheduledCartData = worldlineCustomerAccountFacade.getCartToOrderCronJob(orderCode);
                    worldlineCheckoutFacade.validateReturnMAC(scheduledCartData, returnMAC);
                    worldlineRecurringCheckoutFacade.authorisePaymentForSchudledReplenishmentHostedCheckout(scheduledCartData.getJobCode(), hostedCheckoutId);
                    scheduledCartData = worldlineCustomerAccountFacade.getCartToOrderCronJob(orderCode);
                }
                return getDataMapper().map(scheduledCartData, ReplenishmentOrderWsDTO.class, fields);
            }
        }
    }

    public DataMapper getDataMapper() {
        return dataMapper;
    }

}
