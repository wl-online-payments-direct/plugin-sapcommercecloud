package com.worldline.direct.occ.controllers.v2;


import com.onlinepayments.domain.CreateHostedCheckoutResponse;
import com.worldline.direct.enums.WorldlineCheckoutTypesEnum;
import com.worldline.direct.exception.WorldlineNonAuthorizedPaymentException;
import com.worldline.direct.exception.WorldlineNonValidReturnMACException;
import com.worldline.direct.facade.WorldlineCheckoutFacade;
import com.worldline.direct.occ.controllers.v2.validator.WorldlineBrowserDataWsDTOValidator;
import com.worldline.direct.occ.helpers.WorldlineHelper;
import com.worldline.direct.order.data.BrowserData;
import com.worldline.direct.order.data.WorldlineHostedTokenizationData;
import com.worldline.direct.payment.dto.BrowserDataWsDTO;
import com.worldline.direct.payment.dto.HostedCheckoutResponseWsDTO;
import de.hybris.platform.commercefacades.order.CartFacade;
import de.hybris.platform.commercefacades.order.CheckoutFacade;
import de.hybris.platform.commercefacades.order.OrderFacade;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commercewebservices.core.strategies.OrderCodeIdentificationStrategy;
import de.hybris.platform.commercewebservicescommons.dto.order.OrderWsDTO;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.CartException;
import de.hybris.platform.commercewebservicescommons.strategies.CartLoaderStrategy;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.webservicescommons.mapping.DataMapper;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdAndUserIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import static com.worldline.direct.populator.hostedtokenization.WorldlineHostedTokenizationBasicPopulator.HOSTED_TOKENIZATION_RETURN_URL;

@Controller
@RequestMapping(value = "/{baseSiteId}/users/{userId}/orders")
@Api(tags = "Worldline Orders")
public class WorldlineOrdersController extends WorldlineBaseController {
    private final static Logger LOGGER = LoggerFactory.getLogger(WorldlineOrdersController.class);

    private static final String HTP_MAPPING = "hostedTokenizationId,browserData(screenHeight,screenWidth,navigatorJavaEnabled,navigatorJavaScriptEnabled,timezoneOffsetUtcMinutes,colorDepth)";


    @Resource(name = "dataMapper")
    private DataMapper dataMapper;

    @Resource(name = "worldlineExtendedCheckoutFacade")
    private CheckoutFacade extendedCheckoutFacade;

    @Resource(name = "commerceWebServicesCartFacade2")
    private CartFacade cartFacade;

    @Resource(name = "cartLoaderStrategy")
    private CartLoaderStrategy cartLoaderStrategy;

    @Resource(name = "orderFacade")
    private OrderFacade orderFacade;

    @Resource(name = "orderCodeIdentificationStrategy")
    private OrderCodeIdentificationStrategy orderCodeIdentificationStrategy;

    @Resource(name = "worldlineHelper")
    private WorldlineHelper worldlineHelper;

    @Resource(name = "worldlineCheckoutFacade")
    private WorldlineCheckoutFacade worldlineCheckoutFacade;

    @Resource(name = "worldlineBrowserDataWsDTOValidator")
    private WorldlineBrowserDataWsDTOValidator browserDataWsDTOValidator;

    @Secured({"ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT"})
    @RequestMapping(value = "/hostedtokenization", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(nickname = "place the order with hostedTokenization", value = "Place the order with hostedTokenization.",
            notes = "Authorizes the cart and places the order. The response contains the new order data.")
    @ApiBaseSiteIdAndUserIdParam
    public OrderWsDTO placeOrderHostedTokenization(
            @ApiParam(value = "Cart identifier: cart code for logged in user, cart guid for anonymous user, 'current' for the last modified cart", required = true)
            @RequestParam final String cartId,
            @ApiParam(value = "Request body parameter that contains details. The DTO is in XML or .json format.", required = true)
            @RequestBody final BrowserDataWsDTO browserDataWsDTO,
            @ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields,
            final HttpServletRequest request)
            throws WorldlineNonAuthorizedPaymentException, InvalidCartException {

        cartLoaderStrategy.loadCart(cartId);

        final CartData cartData = cartFacade.getSessionCart();
        if (cartData.getWorldlinePaymentInfo() == null) {
            throw new CartException("No Worldline Payment Info found.", CartException.INVALID);
        } else if (!WorldlineCheckoutTypesEnum.HOSTED_TOKENIZATION.equals(cartData.getWorldlinePaymentInfo().getWorldlineCheckoutType())) {
            throw new CartException("Invalid Worldline Checkout Type.", CartException.INVALID);
        } else if (StringUtils.isBlank(cartData.getWorldlinePaymentInfo().getHostedTokenizationId()) && StringUtils.isBlank(cartData.getWorldlinePaymentInfo().getPaymentProductDirectoryId())) {
            throw new CartException("Invalid Worldline HostedTokenizationId.", CartException.INVALID);
        }

        validate(browserDataWsDTO, "browserDataWsDTO", browserDataWsDTOValidator);

        final BrowserData browserData = getDataMapper().map(browserDataWsDTO, BrowserData.class, BROWSER_MAPPING);
        fillBrowserData(request, browserData);

        OrderData orderData = extendedCheckoutFacade.placeOrder();

        final WorldlineHostedTokenizationData hostedTokenizationData = new WorldlineHostedTokenizationData();
        hostedTokenizationData.setHostedTokenizationId(orderData.getWorldlinePaymentInfo().getHostedTokenizationId());
        hostedTokenizationData.setBrowserData(browserData);
        hostedTokenizationData.setReturnUrl(sessionService.getAttribute(HOSTED_TOKENIZATION_RETURN_URL));

        storeHTPReturnUrlInSession(orderData.getCode(), request);

        worldlineCheckoutFacade.authorisePaymentForHostedTokenization(orderData.getCode(), hostedTokenizationData);
        orderData = orderFacade.getOrderDetailsForCodeWithoutUser(orderData.getCode());

        return getDataMapper().map(orderData, OrderWsDTO.class, fields);
    }

    private void storeHTPReturnUrlInSession(String code, HttpServletRequest request) {
        final String returnURL = worldlineHelper.buildReturnURL(request, "worldline.occ.hostedTokenization.returnUrl");
        sessionService.setAttribute("hostedTokenizationReturnUrl", returnURL.replace("_orderCode_", code));
    }

    @Secured({"ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT"})
    @RequestMapping(value = "/{orderCode}/hostedtokenization/return", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(nickname = "Handle return for hostedTokenization", value = "handle return for hostedTokenization.")
    @ApiBaseSiteIdAndUserIdParam
    public OrderWsDTO handleHostedTokenizationReturn(
            @ApiParam(value = "Order GUID (Globally Unique Identifier) or order CODE", required = true)
            @PathVariable(value = "orderCode") final String orderCode,
            @RequestParam(value = "RETURNMAC", required = true) final String returnMAC,
            @RequestParam(value = "REF", required = true) final String ref,
            @RequestParam(value = "paymentId", required = true) final String paymentId,
            @ApiParam(value = "Cart identifier: cart code for logged in user, cart guid for anonymous user, 'current' for the last modified cart", required = true)
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

        worldlineCheckoutFacade.validateReturnMAC(orderData, returnMAC);
        worldlineCheckoutFacade.handle3dsResponse(orderData.getCode(), paymentId);
        orderData = orderFacade.getOrderDetailsForCodeWithoutUser(orderCode);

        return getDataMapper().map(orderData, OrderWsDTO.class, fields);
    }

    @Secured({"ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT"})
    @RequestMapping(value = "/hostedcheckout", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(nickname = "placeOrderHostedCheckout", value = "Place the order with HostedCheckout.", notes =
            "Returns a hosted checkout data for the current base store. " +
                    "cart must be valid, otherwise an error will be returned.")
    @ApiBaseSiteIdAndUserIdParam
    public HostedCheckoutResponseWsDTO placeOrderHostedCheckout(
            @ApiParam(value = "Request body parameter that contains details \n\nThe DTO is in XML or .json format.", required = true)
            @RequestBody final BrowserDataWsDTO browserDataWsDTO,
            @ApiParam(value = "Cart identifier: cart code for logged in user, cart guid for anonymous user, 'current' for the last modified cart", required = true)
            @RequestParam final String cartId,
            @ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields,
            final HttpServletRequest request) throws InvalidCartException {

        cartLoaderStrategy.loadCart(cartId);

        final CartData cartData = cartFacade.getSessionCart();
        if (cartData.getWorldlinePaymentInfo() == null) {
            throw new CartException("No Worldline Payment Info found.", CartException.INVALID);
        } else if (!WorldlineCheckoutTypesEnum.HOSTED_CHECKOUT.equals(cartData.getWorldlinePaymentInfo().getWorldlineCheckoutType())) {
            throw new CartException("Invalid Worldline Checkout Type.", CartException.INVALID);
        }

        validate(browserDataWsDTO, "browserData", browserDataWsDTOValidator);
        final BrowserData browserData = getDataMapper().map(browserDataWsDTO, BrowserData.class, BROWSER_MAPPING);
        fillBrowserData(request, browserData);

        OrderData orderData = extendedCheckoutFacade.placeOrder();

        storeHOPReturnUrlInSession(orderData.getCode(), request);

        final CreateHostedCheckoutResponse createHostedCheckoutResponse = worldlineCheckoutFacade.createHostedCheckout(orderData.getCode(), browserData);
        return getDataMapper().map(createHostedCheckoutResponse, HostedCheckoutResponseWsDTO.class, fields);

    }


    @Secured({"ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT"})
    @RequestMapping(value = "/{orderCode}/hostedcheckout/return", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(nickname = "Handle return for HostedCheckout", value = "Handle return for HostedCheckout.",
            notes = "Authorizes the cart and places the order. The response contains the new order data.")
    @ApiBaseSiteIdAndUserIdParam
    public OrderWsDTO handleHostedCheckoutReturn(
            @ApiParam(value = "Order GUID (Globally Unique Identifier) or order CODE", required = true)
            @PathVariable final String orderCode,
            @RequestParam(value = "RETURNMAC", required = true) final String returnMAC,
            @RequestParam(value = "hostedCheckoutId", required = true) final String hostedCheckoutId,
            @ApiParam(value = "Cart identifier: cart code for logged in user, cart guid for anonymous user, 'current' for the last modified cart", required = true)
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

        worldlineCheckoutFacade.validateReturnMAC(orderData, returnMAC);
        worldlineCheckoutFacade.authorisePaymentForHostedCheckout(orderData.getCode(), hostedCheckoutId);
        orderData = orderFacade.getOrderDetailsForCodeWithoutUser(orderCode);
        return getDataMapper().map(orderData, OrderWsDTO.class, fields);
    }

    private void storeHOPReturnUrlInSession(String code, HttpServletRequest request) {
        final String returnURL = worldlineHelper.buildReturnURL(request, "worldline.occ.hostedCheckout.returnUrl");
        sessionService.setAttribute("hostedCheckoutReturnUrl", returnURL.replace("_orderCode_", code));
    }


    public DataMapper getDataMapper() {
        return dataMapper;
    }

}
