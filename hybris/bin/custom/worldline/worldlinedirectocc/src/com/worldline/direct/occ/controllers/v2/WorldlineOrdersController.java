package com.worldline.direct.occ.controllers.v2;


import com.onlinepayments.domain.CreateHostedCheckoutResponse;
import com.worldline.direct.enums.OrderType;
import com.worldline.direct.enums.RecurringPaymentEnum;
import com.worldline.direct.enums.WorldlineCheckoutTypesEnum;
import com.worldline.direct.exception.WorldlineNonAuthorizedPaymentException;
import com.worldline.direct.exception.WorldlineNonValidReturnMACException;
import com.worldline.direct.facade.WorldlineCheckoutFacade;
import com.worldline.direct.facade.WorldlineDirectCheckoutFacade;
import com.worldline.direct.facade.WorldlineRecurringCheckoutFacade;
import com.worldline.direct.occ.controllers.v2.validator.WorldlineBrowserDataWsDTOValidator;
import com.worldline.direct.occ.helpers.WorldlineHelper;
import com.worldline.direct.order.data.BrowserData;
import com.worldline.direct.order.data.WorldlineHostedTokenizationData;
import com.worldline.direct.payment.dto.BrowserDataWsDTO;
import com.worldline.direct.payment.dto.HostedCheckoutResponseWsDTO;
import com.worldline.direct.payment.dto.RecurringDataWSDTO;
import de.hybris.platform.b2bacceleratorfacades.checkout.data.PlaceOrderData;
import de.hybris.platform.b2bacceleratorfacades.order.data.ScheduledCartData;
import de.hybris.platform.b2bwebservicescommons.dto.order.ScheduleReplenishmentFormWsDTO;
import de.hybris.platform.commercefacades.order.CartFacade;
import de.hybris.platform.commercefacades.order.OrderFacade;
import de.hybris.platform.commercefacades.order.data.*;
import de.hybris.platform.commercefacades.user.UserFacade;
import de.hybris.platform.commerceservices.order.CommerceCartModificationException;
import de.hybris.platform.commercewebservices.core.strategies.OrderCodeIdentificationStrategy;
import de.hybris.platform.commercewebservicescommons.dto.order.OrderWsDTO;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.CartException;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.RequestParameterException;
import de.hybris.platform.commercewebservicescommons.strategies.CartLoaderStrategy;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.webservicescommons.errors.exceptions.WebserviceValidationException;
import de.hybris.platform.webservicescommons.mapping.DataMapper;
import de.hybris.platform.webservicescommons.mapping.FieldSetLevelHelper;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdAndUserIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static com.worldline.direct.occ.controllers.v2.WorldlineB2BOrdersController.CART_CHECKOUT_TERM_UNCHECKED;
import static com.worldline.direct.occ.controllers.v2.WorldlineB2BOrdersController.OBJECT_NAME_SCHEDULE_REPLENISHMENT_FORM;
import static com.worldline.direct.populator.hostedtokenization.WorldlineHostedTokenizationBasicPopulator.HOSTED_TOKENIZATION_RETURN_URL;
import static de.hybris.platform.util.localization.Localization.getLocalizedString;

@Controller
@RequestMapping(value = "/{baseSiteId}/users/{userId}/orders")
@Tag(name = "Worldline Orders")
public class WorldlineOrdersController extends WorldlineBaseController {
    private final static Logger LOGGER = LoggerFactory.getLogger(WorldlineOrdersController.class);

    private static final String HTP_MAPPING = "hostedTokenizationId,browserData(screenHeight,screenWidth,navigatorJavaEnabled,navigatorJavaScriptEnabled,timezoneOffsetUtcMinutes,colorDepth)";


    @Resource(name = "dataMapper")
    private DataMapper dataMapper;

    @Resource(name = "worldlineExtendedCheckoutFacade")
    private WorldlineDirectCheckoutFacade extendedCheckoutFacade;

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

    @Resource(name = "worldlineRecurringCheckoutFacade")
    private WorldlineRecurringCheckoutFacade worldlineRecurringCheckoutFacade;

    @Resource(name = "worldlineB2BPlaceOrderCartValidator")
    private Validator worldlineB2BPlaceOrderCartValidator;

    @Resource(name = "scheduleReplenishmentFormWsDTOValidator")
    private Validator scheduleReplenishmentFormWsDTOValidator;


    @Resource(name = "userFacade")
    private UserFacade userFacade;



    @PostMapping(value = "/hostedTokenization")
    @ResponseBody
    @Operation(operationId = "place the order with hostedTokenization", summary = "Place the order with hostedTokenization.",
            description = "Authorizes the cart and places the order. The response contains the new order data.")
    @ApiBaseSiteIdAndUserIdParam
    public OrderWsDTO placeOrderHostedTokenization(
            @Parameter(description = "Cart identifier: cart code for logged in user, cart guid for anonymous user, 'current' for the last modified cart", required = true)
            @RequestParam final String cartId,
            @Parameter(description = "Request body parameter that contains details. The DTO is in XML or .json format.", required = true)
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

    @PostMapping(value = "/{orderCode}/hostedTokenization/return")
    @ResponseBody
    @Operation(description = "Handle return for hostedTokenization", summary = "handle return for hostedTokenization.")
    @ApiBaseSiteIdAndUserIdParam
    public OrderWsDTO handleHostedTokenizationReturn(
            @Parameter(description= "Order GUID (Globally Unique Identifier) or order CODE", required = true)
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

        worldlineCheckoutFacade.validateReturnMAC(orderData, returnMAC);
        worldlineCheckoutFacade.handle3dsResponse(orderData.getCode(), paymentId);
        orderData = orderFacade.getOrderDetailsForCodeWithoutUser(orderCode);

        return getDataMapper().map(orderData, OrderWsDTO.class, fields);
    }

    @PostMapping(value = "/hostedCheckout")
    @ResponseBody
    @Operation(operationId = "placeOrderHostedCheckout", summary = "Place the order with HostedCheckout.", description =
            "Returns a hosted checkout data for the current base store. " +
                    "cart must be valid, otherwise an error will be returned.")
    @ApiBaseSiteIdAndUserIdParam
    public HostedCheckoutResponseWsDTO placeOrderHostedCheckout(
            @Parameter(description = "Request body parameter that contains details \n\nThe DTO is in XML or .json format.", required = true)
            @RequestBody final BrowserDataWsDTO browserDataWsDTO,
            @Parameter(description = "Cart identifier: cart code for logged in user, cart guid for anonymous user, 'current' for the last modified cart", required = true)
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

        storeHOPReturnUrlInSession(orderData.getCode(), request,OrderType.PLACE_ORDER);

        final CreateHostedCheckoutResponse createHostedCheckoutResponse = worldlineCheckoutFacade.createHostedCheckout(orderData.getCode(), browserData);
        return getDataMapper().map(createHostedCheckoutResponse, HostedCheckoutResponseWsDTO.class, fields);

    }
    @PostMapping(value = "/recurringHostedCheckout", consumes =
            {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    @ApiBaseSiteIdAndUserIdParam
    @Operation(operationId = "placeReplenishmentOrderHostedCheckout", summary = "Place the Replenishment order with HostedCheckout", description = "Place the Replenishment order with HostedCheckout")
    public HostedCheckoutResponseWsDTO placeReplenishmentOrderHostedCheckout(
            @Parameter(description = "Cart identifier: cart code for logged in user, cart guid for anonymous user, 'current' for the last modified cart", required = true) @RequestParam(required = true) final String cartId,
            @Parameter(description = "Whether terms were accepted or not.", required = true) @RequestParam(required = true) final boolean termsChecked,
            @Parameter(description = "Request body parameter that contains details. The DTO is in XML or .json format.", required = true) @RequestBody final RecurringDataWSDTO recurringDataWSDTO,
            @ApiFieldsParam @RequestParam(required = false, defaultValue = FieldSetLevelHelper.DEFAULT_LEVEL) final String fields, final HttpServletRequest request)
            throws InvalidCartException {

        validateTerms(termsChecked);
        validateUser();

        cartLoaderStrategy.loadCart(cartId);
        final CartData cartData = cartFacade.getSessionCart();

        validateCart(cartData);

        validateScheduleReplenishmentForm(recurringDataWSDTO.getScheduleReplenishment());
        final PlaceOrderData placeOrderData = createPlaceOrderData(recurringDataWSDTO.getScheduleReplenishment());
        AbstractOrderData abstractOrderData = extendedCheckoutFacade.placeOrder(placeOrderData);
        final CreateHostedCheckoutResponse createHostedCheckoutResponse;
        if (abstractOrderData instanceof ScheduledCartData) {
            storeHOPReturnUrlInSession(((ScheduledCartData) abstractOrderData).getJobCode(), request, OrderType.SCHEDULE_REPLENISHMENT_ORDER);
            validate(recurringDataWSDTO.getBrowserData(), "browserDataWsDTO", browserDataWsDTOValidator);
            final BrowserData browserData = getDataMapper().map(recurringDataWSDTO.getBrowserData(), BrowserData.class, BROWSER_MAPPING);
            createHostedCheckoutResponse = worldlineRecurringCheckoutFacade.createReplenishmentHostedCheckout(abstractOrderData, browserData, RecurringPaymentEnum.SCHEDULED);
        }else {
            storeHOPReturnUrlInSession(abstractOrderData.getCode(), request, OrderType.SCHEDULE_REPLENISHMENT_ORDER);
            validate(recurringDataWSDTO.getBrowserData(), "browserDataWsDTO", browserDataWsDTOValidator);
            final BrowserData browserData = getDataMapper().map(recurringDataWSDTO.getBrowserData(), BrowserData.class, BROWSER_MAPPING);
            createHostedCheckoutResponse = worldlineRecurringCheckoutFacade.createReplenishmentHostedCheckout(abstractOrderData, browserData, RecurringPaymentEnum.IMMEDIATE);

        }
        return dataMapper.map(createHostedCheckoutResponse, HostedCheckoutResponseWsDTO.class, fields);
    }


    @PostMapping(value = "/{orderCode}/hostedCheckout/return")
    @ResponseBody
    @Operation(operationId = "Handle return for HostedCheckout", summary = "Handle return for HostedCheckout.",
            description = "Authorizes the cart and places the order. The response contains the new order data.")
    @ApiBaseSiteIdAndUserIdParam
    public OrderWsDTO handleHostedCheckoutReturn(
            @Parameter(description = "Order GUID (Globally Unique Identifier) or order CODE", required = true)
            @PathVariable final String orderCode,
            @RequestParam(value = "RETURNMAC", required = true) final String returnMAC,
            @RequestParam(value = "hostedCheckoutId", required = true) final String hostedCheckoutId,
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

        worldlineCheckoutFacade.validateReturnMAC(orderData, returnMAC);
        worldlineCheckoutFacade.authorisePaymentForHostedCheckout(orderData.getCode(), hostedCheckoutId);
        orderData = orderFacade.getOrderDetailsForCodeWithoutUser(orderCode);
        return getDataMapper().map(orderData, OrderWsDTO.class, fields);
    }



    protected void validateTerms(final boolean termsChecked) {
        if (!termsChecked) {
            throw new RequestParameterException(getLocalizedString(CART_CHECKOUT_TERM_UNCHECKED));
        }
    }

    protected void validateUser() {
        if (userFacade.isAnonymousUser()) {
            throw new AccessDeniedException("Access is denied");
        }
    }

    protected void validateCart(final CartData cartData) throws InvalidCartException {
        final Errors errors = new BeanPropertyBindingResult(cartData, "sessionCart");
        worldlineB2BPlaceOrderCartValidator.validate(cartData, errors);
        if (errors.hasErrors()) {
            throw new WebserviceValidationException(errors);
        }

        try {
            final List<CartModificationData> modificationList = cartFacade.validateCartData();
            if (CollectionUtils.isNotEmpty(modificationList)) {
                final CartModificationDataList cartModificationDataList = new CartModificationDataList();
                cartModificationDataList.setCartModificationList(modificationList);
                throw new WebserviceValidationException(cartModificationDataList);
            }
        } catch (final CommerceCartModificationException e) {
            throw new InvalidCartException(e);
        }
    }

    protected void validateScheduleReplenishmentForm(ScheduleReplenishmentFormWsDTO scheduleReplenishmentForm) {
        validate(scheduleReplenishmentForm, OBJECT_NAME_SCHEDULE_REPLENISHMENT_FORM, scheduleReplenishmentFormWsDTOValidator);
    }

    protected PlaceOrderData createPlaceOrderData(final ScheduleReplenishmentFormWsDTO scheduleReplenishmentForm) {
        final PlaceOrderData placeOrderData = new PlaceOrderData();
        dataMapper.map(scheduleReplenishmentForm, placeOrderData, false);
        if (scheduleReplenishmentForm != null) {
            placeOrderData.setReplenishmentOrder(Boolean.TRUE);
        }
        placeOrderData.setTermsCheck(Boolean.TRUE);
        return placeOrderData;
    }



    private void storeHOPReturnUrlInSession(String code, HttpServletRequest request, OrderType orderType) {
        final String returnURL = worldlineHelper.buildRecurringReturnURL(request, "worldline.occ.hostedCheckout.returnUrl", orderType);
        sessionService.setAttribute("hostedCheckoutReturnUrl", returnURL.replace("_cartId_", code));
    }


    public DataMapper getDataMapper() {
        return dataMapper;
    }

}
