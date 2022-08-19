package com.worldline.direct.occ.controllers.v2;


import com.onlinepayments.domain.CreateHostedCheckoutResponse;
import com.worldline.direct.enums.WorldlineCheckoutTypesEnum;
import com.worldline.direct.exception.WorldlineNonAuthorizedPaymentException;
import com.worldline.direct.facade.WorldlineRecurringCheckoutFacade;
import com.worldline.direct.occ.controllers.v2.validator.WorldlineBrowserDataWsDTOValidator;
import com.worldline.direct.occ.helpers.WorldlineHelper;
import com.worldline.direct.order.data.BrowserData;
import com.worldline.direct.order.data.WorldlineHostedTokenizationData;
import com.worldline.direct.payment.dto.BrowserDataWsDTO;
import com.worldline.direct.payment.dto.HostedCheckoutResponseWsDTO;
import com.worldline.direct.payment.dto.RecurringDataWSDTO;
import de.hybris.platform.b2bacceleratorfacades.api.cart.CheckoutFacade;
import de.hybris.platform.b2bacceleratorfacades.checkout.data.PlaceOrderData;
import de.hybris.platform.b2bacceleratorfacades.order.data.ScheduledCartData;
import de.hybris.platform.b2bwebservicescommons.dto.order.ScheduleReplenishmentFormWsDTO;
import de.hybris.platform.commercefacades.order.CartFacade;
import de.hybris.platform.commercefacades.order.OrderFacade;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.order.data.CartModificationData;
import de.hybris.platform.commercefacades.order.data.CartModificationDataList;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commercefacades.user.UserFacade;
import de.hybris.platform.commerceservices.order.CommerceCartModificationException;
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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static com.worldline.direct.populator.hostedtokenization.WorldlineHostedTokenizationBasicPopulator.HOSTED_TOKENIZATION_RETURN_URL;
import static de.hybris.platform.util.localization.Localization.getLocalizedString;

@Controller
@RequestMapping(value = "/{baseSiteId}/orgUsers/{userId}/orders")
@Api(tags = "Worldline B2B Orders")
public class WorldlineB2BOrdersController extends WorldlineBaseController {
    private static final String CART_CHECKOUT_TERM_UNCHECKED = "cart.term.unchecked";
    private static final String OBJECT_NAME_SCHEDULE_REPLENISHMENT_FORM = "ScheduleReplenishmentForm";


    @Resource(name = "dataMapper")
    private DataMapper dataMapper;

    @Resource(name = "worldLineExtendedB2BCheckoutFacade")
    private CheckoutFacade extendedCheckoutFacade;

    @Resource(name = "commerceWebServicesCartFacade2")
    private CartFacade cartFacade;

    @Resource(name = "cartLoaderStrategy")
    private CartLoaderStrategy cartLoaderStrategy;

    @Resource(name = "orderFacade")
    private OrderFacade orderFacade;

    @Resource(name = "worldlineHelper")
    private WorldlineHelper worldlineHelper;

    @Resource(name = "worldlineRecurringCheckoutFacade")
    private WorldlineRecurringCheckoutFacade worldlineRecurringCheckoutFacade;

    @Resource(name = "worldlineBrowserDataWsDTOValidator")
    private WorldlineBrowserDataWsDTOValidator browserDataWsDTOValidator;

    @Resource(name = "worldlineB2BPlaceOrderCartValidator")
    private Validator worldlineB2BPlaceOrderCartValidator;

    @Resource(name = "scheduleReplenishmentFormWsDTOValidator")
    private Validator scheduleReplenishmentFormWsDTOValidator;


    @Resource(name = "userFacade")
    private UserFacade userFacade;

    public WorldlineB2BOrdersController() {
    }

    @Secured({"ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT"})
    @RequestMapping(value = "/hostedTokenization", method = RequestMethod.POST)
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

        OrderData orderData = extendedCheckoutFacade.placeOrder(new PlaceOrderData());

        final WorldlineHostedTokenizationData hostedTokenizationData = new WorldlineHostedTokenizationData();
        hostedTokenizationData.setHostedTokenizationId(orderData.getWorldlinePaymentInfo().getHostedTokenizationId());
        hostedTokenizationData.setBrowserData(browserData);
        hostedTokenizationData.setReturnUrl(sessionService.getAttribute(HOSTED_TOKENIZATION_RETURN_URL));

        storeHTPReturnUrlInSession(orderData.getCode(), request);

        worldlineRecurringCheckoutFacade.authorisePaymentForHostedTokenization(orderData.getCode(), hostedTokenizationData);
        orderData = orderFacade.getOrderDetailsForCodeWithoutUser(orderData.getCode());

        return getDataMapper().map(orderData, OrderWsDTO.class, fields);
    }

    private void storeHTPReturnUrlInSession(String code, HttpServletRequest request) {
        final String returnURL = worldlineHelper.buildReturnURL(request, "worldline.occ.hostedTokenization.returnUrl");
        sessionService.setAttribute("hostedTokenizationReturnUrl", returnURL.replace("_orderCode_", code));
    }


    @Secured({"ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT"})
    @RequestMapping(value = "/hostedCheckout", method = RequestMethod.POST)
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

        OrderData orderData = extendedCheckoutFacade.placeOrder(new PlaceOrderData());

        storeHOPReturnUrlInSession(orderData.getCode(), request, false);

        final CreateHostedCheckoutResponse createHostedCheckoutResponse = worldlineRecurringCheckoutFacade.createHostedCheckout(orderData.getCode(), browserData);
        return getDataMapper().map(createHostedCheckoutResponse, HostedCheckoutResponseWsDTO.class, fields);

    }

    private void storeHOPReturnUrlInSession(String code, HttpServletRequest request, Boolean isRecurring) {
        final String returnURL = worldlineHelper.buildRecurringReturnURL(request, "worldline.occ.hostedCheckout.returnUrl", isRecurring);
        sessionService.setAttribute("hostedCheckoutReturnUrl", returnURL.replace("_cartId_", code));
    }

    @RequestMapping(value = "/recurringHostedCheckout", method = RequestMethod.POST, consumes =
            {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    @ApiBaseSiteIdAndUserIdParam
    @ApiOperation(nickname = "placeReplenishmentOrderHostedCheckout", value = "Place the Replenishment order with HostedCheckout", notes = "Place the Replenishment order with HostedCheckout", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public HostedCheckoutResponseWsDTO placeReplenishmentOrderHostedCheckout(
            @ApiParam(value = "Cart identifier: cart code for logged in user, cart guid for anonymous user, 'current' for the last modified cart", required = true) @RequestParam(required = true) final String cartId,
            @ApiParam(value = "Whether terms were accepted or not.", required = true) @RequestParam(required = true) final boolean termsChecked,
            @ApiParam(value = "Request body parameter that contains details. The DTO is in XML or .json format.", required = true) @RequestBody final RecurringDataWSDTO recurringDataWSDTO,
            @ApiFieldsParam @RequestParam(required = false, defaultValue = FieldSetLevelHelper.DEFAULT_LEVEL) final String fields, final HttpServletRequest request)
            throws InvalidCartException {

        validateTerms(termsChecked);
        validateUser();

        cartLoaderStrategy.loadCart(cartId);
        final CartData cartData = cartFacade.getSessionCart();

        validateCart(cartData);

        validateScheduleReplenishmentForm(recurringDataWSDTO.getScheduleReplenishment());
        final PlaceOrderData placeOrderData = createPlaceOrderData(recurringDataWSDTO.getScheduleReplenishment());
        ScheduledCartData scheduledCartData = extendedCheckoutFacade.placeOrder(placeOrderData);
        storeHOPReturnUrlInSession(scheduledCartData.getJobCode(), request, true);
        validate(recurringDataWSDTO.getBrowserData(), "browserDataWsDTO", browserDataWsDTOValidator);

        final BrowserData browserData = getDataMapper().map(recurringDataWSDTO.getBrowserData(), BrowserData.class, BROWSER_MAPPING);

        final CreateHostedCheckoutResponse createHostedCheckoutResponse = worldlineRecurringCheckoutFacade.createReplenishmentHostedCheckout(scheduledCartData.getJobCode(), browserData);
        return dataMapper.map(createHostedCheckoutResponse, HostedCheckoutResponseWsDTO.class, fields);
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


    public DataMapper getDataMapper() {
        return dataMapper;
    }

}
