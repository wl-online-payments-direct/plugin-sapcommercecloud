package com.ingenico.ogone.direct.occ.controllers.v2;


import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import de.hybris.platform.commercefacades.order.CartFacade;
import de.hybris.platform.commercefacades.order.CheckoutFacade;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.order.data.OrderData;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.ingenico.ogone.direct.enums.IngenicoCheckoutTypesEnum;
import com.ingenico.ogone.direct.exception.IngenicoNonAuthorizedPaymentException;
import com.ingenico.ogone.direct.facade.IngenicoCheckoutFacade;
import com.ingenico.ogone.direct.occ.controllers.v2.validator.IngenicoBrowserDataWsDTOValidator;
import com.ingenico.ogone.direct.occ.controllers.v2.validator.IngenicoHostedTokenizationRequestWsDTOValidator;
import com.ingenico.ogone.direct.occ.helpers.IngenicoHelper;
import com.ingenico.ogone.direct.order.data.IngenicoHostedTokenizationData;
import com.ingenico.ogone.direct.payment.dto.HostedTokenizationRequestWsDTO;

@Controller
@RequestMapping(value = "/{baseSiteId}/users/{userId}/orders")
@Api(tags = "Ingenico Orders")
public class IngenicoOrdersController extends IngenicoBaseController {
    private final static Logger LOGGER = LoggerFactory.getLogger(IngenicoOrdersController.class);

    private static final String HTP_MAPPING = "hostedTokenizationId,browserData(screenHeight,screenWidth,navigatorJavaEnabled,timezoneOffsetUtcMinutes,colorDepth,acceptHeader,userAgent,locale,ipAddress)";


    @Resource(name = "dataMapper")
    private DataMapper dataMapper;

    @Resource(name = "checkoutFacade")
    private CheckoutFacade checkoutFacade;

    @Resource(name = "commerceWebServicesCartFacade2")
    private CartFacade cartFacade;

    @Resource(name = "cartLoaderStrategy")
    private CartLoaderStrategy cartLoaderStrategy;

    @Resource(name = "ingenicoHelper")
    private IngenicoHelper ingenicoHelper;

    @Resource(name = "ingenicoCheckoutFacade")
    private IngenicoCheckoutFacade ingenicoCheckoutFacade;

    @Resource(name = "ingenicoHostedTokenizationRequestWsDTOValidator")
    private IngenicoHostedTokenizationRequestWsDTOValidator hostedTokenizationRequestWsDTOValidator;

    @Resource(name = "ingenicoBrowserDataWsDTOValidator")
    private IngenicoBrowserDataWsDTOValidator browserDataWsDTOValidator;

    @Secured({"ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT"})
    @RequestMapping(value = "/{cartId}/hostedtokenization", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(nickname = "place the order with hostedTokenization", value = "Place the order with hostedTokenization.",
            notes = "Authorizes the cart and places the order. The response contains the new order data.")
    @ApiBaseSiteIdAndUserIdParam
    public OrderWsDTO placeOrderHostedTokenization(
            @ApiParam(value = "Cart code for logged in user, cart GUID for guest checkout", required = true)
            @PathVariable final String cartId,
            @ApiParam(value = "Request body parameter that contains details. The DTO is in XML or .json format.", required = true)
            @RequestBody final HostedTokenizationRequestWsDTO hostedTokenizationRequestWsDTO,
            @ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields,
            final HttpServletRequest request)
            throws IngenicoNonAuthorizedPaymentException, InvalidCartException {

        cartLoaderStrategy.loadCart(cartId);

        final CartData cartData = cartFacade.getSessionCart();
        if (cartData.getIngenicoPaymentInfo() == null) {
            throw new CartException("No Ingenico Payment Info found.", CartException.INVALID);
        } else if (!IngenicoCheckoutTypesEnum.HOSTED_TOKENIZATION.equals(cartData.getIngenicoPaymentInfo().getIngenicoCheckoutType())) {
            throw new CartException("Invalid Ingenico Checkout Type.", CartException.INVALID);
        }

        validate(hostedTokenizationRequestWsDTO, "hostedTokenizationRequestWsDTO", hostedTokenizationRequestWsDTOValidator);
        validate(hostedTokenizationRequestWsDTO.getBrowserData(), "browserData", browserDataWsDTOValidator);

        final IngenicoHostedTokenizationData hostedTokenizationData = getDataMapper().map(
                hostedTokenizationRequestWsDTO,
                IngenicoHostedTokenizationData.class,
                HTP_MAPPING);

        final String returnURL = ingenicoHelper.buildReturnURL(request, "ingenico.occ.hostedTokenization.returnUrl");
        sessionService.setAttribute("hostedTokenizationReturnUrl", returnURL);

        final OrderData orderData = ingenicoCheckoutFacade.authorisePaymentForHostedTokenization(hostedTokenizationData);
        return getDataMapper().map(orderData, OrderWsDTO.class, fields);
    }

    @Secured({"ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT"})
    @RequestMapping(value = "/{cartId}/hostedtokenization/return3ds", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(nickname = "handle 3ds return for hostedTokenization", value = "handle 3ds return for hostedTokenization.")
    @ApiBaseSiteIdAndUserIdParam
    public OrderWsDTO handle3DS(
            @ApiParam(value = "Cart code for logged in user, cart GUID for guest checkout", required = true)
            @PathVariable final String cartId,
            @RequestParam(value = "RETURNMAC", required = true) final String returnMAC,
            @RequestParam(value = "REF", required = true) final String ref,
            @RequestParam(value = "paymentId", required = true) final String paymentId,
            @ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
            throws IngenicoNonAuthorizedPaymentException, InvalidCartException {

        cartLoaderStrategy.loadCart(cartId);

        final CartData cartData = cartFacade.getSessionCart();
        if (cartData.getIngenicoPaymentInfo() == null) {
            throw new CartException("No Ingenico Payment Info found.", CartException.INVALID);
        } else if (!IngenicoCheckoutTypesEnum.HOSTED_TOKENIZATION.equals(cartData.getIngenicoPaymentInfo().getIngenicoCheckoutType())) {
            throw new CartException("Invalid Ingenico Checkout Type.", CartException.INVALID);
        }

        final OrderData orderData = ingenicoCheckoutFacade.handle3dsResponse(cartId, returnMAC, paymentId);
        return getDataMapper().map(orderData, OrderWsDTO.class, fields);
    }


    @Secured({"ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT"})
    @RequestMapping(value = "/{cartId}/hostedcheckout", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(nickname = "place the order with HostedCheckout", value = "Place the order with HostedCheckout.",
            notes = "Authorizes the cart and places the order. The response contains the new order data.")
    @ApiBaseSiteIdAndUserIdParam
    public OrderWsDTO placeOrderHostedCheckout(
            @ApiParam(value = "Cart code for logged in user, cart GUID for guest checkout", required = true)
            @RequestParam final String cartId,
            @RequestParam(value = "RETURNMAC", required = true) final String returnMAC,
            @RequestParam(value = "hostedCheckoutId", required = true) final String hostedCheckoutId,
            @ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
            throws IngenicoNonAuthorizedPaymentException, InvalidCartException {

        cartLoaderStrategy.loadCart(cartId);

        final CartData cartData = cartFacade.getSessionCart();
        if (cartData.getIngenicoPaymentInfo() == null) {
            throw new CartException("No Ingenico Payment Info found.", CartException.INVALID);
        } else if (!IngenicoCheckoutTypesEnum.HOSTED_CHECKOUT.equals(cartData.getIngenicoPaymentInfo().getIngenicoCheckoutType())) {
            throw new CartException("Invalid Ingenico Checkout Type.", CartException.INVALID);
        }

        final OrderData orderData = ingenicoCheckoutFacade.authorisePaymentForHostedCheckout(hostedCheckoutId);
        return getDataMapper().map(orderData, OrderWsDTO.class, fields);
    }


    public DataMapper getDataMapper() {
        return dataMapper;
    }

    public CheckoutFacade getCheckoutFacade() {
        return checkoutFacade;
    }

    public void setCheckoutFacade(CheckoutFacade checkoutFacade) {
        this.checkoutFacade = checkoutFacade;
    }
}
