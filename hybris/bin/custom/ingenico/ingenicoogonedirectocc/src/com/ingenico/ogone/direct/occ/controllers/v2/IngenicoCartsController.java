package com.ingenico.ogone.direct.occ.controllers.v2;


import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

import de.hybris.platform.commercefacades.order.CartFacade;
import de.hybris.platform.commercefacades.order.CheckoutFacade;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.commercewebservicescommons.dto.user.AddressWsDTO;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.CartAddressException;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.CartException;
import de.hybris.platform.webservicescommons.mapping.DataMapper;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdUserIdAndCartIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.ingenico.direct.domain.CreateHostedCheckoutResponse;
import com.ingenico.direct.domain.CreateHostedTokenizationResponse;
import com.ingenico.direct.domain.GetPaymentProductsResponse;
import com.ingenico.direct.domain.PaymentProduct;
import com.ingenico.ogone.direct.enums.IngenicoCheckoutTypesEnum;
import com.ingenico.ogone.direct.exception.IngenicoNonValidPaymentProductException;
import com.ingenico.ogone.direct.facade.IngenicoCheckoutFacade;
import com.ingenico.ogone.direct.occ.controllers.v2.validator.IngenicoBrowserDataWsDTOValidator;
import com.ingenico.ogone.direct.occ.controllers.v2.validator.IngenicoPaymentDetailsWsDTOValidator;
import com.ingenico.ogone.direct.occ.helpers.IngenicoHelper;
import com.ingenico.ogone.direct.order.data.BrowserData;
import com.ingenico.ogone.direct.order.data.IngenicoPaymentInfoData;
import com.ingenico.ogone.direct.payment.dto.BrowserDataWsDTO;
import com.ingenico.ogone.direct.payment.dto.HostedCheckoutResponseWsDTO;
import com.ingenico.ogone.direct.payment.dto.HostedTokenizationResponseWsDTO;
import com.ingenico.ogone.direct.payment.dto.IngenicoCheckoutTypeWsDTO;
import com.ingenico.ogone.direct.payment.dto.IngenicoPaymentDetailsWsDTO;
import com.ingenico.ogone.direct.payment.dto.PaymentProductListWsDTO;

@Controller
@RequestMapping(value = "/{baseSiteId}/users/{userId}/carts")
@Api(tags = "Ingenico Carts")
public class IngenicoCartsController extends IngenicoBaseController {
    private final static Logger LOGGER = LoggerFactory.getLogger(IngenicoCartsController.class);

    private static final String ADDRESS_MAPPING = "firstName,lastName,titleCode,phone,line1,line2,town,postalCode,country(isocode),region(isocode),defaultAddress";
    private static final String BROWSER_MAPPING = "screenHeight,screenWidth,navigatorJavaEnabled,navigatorJavaScriptEnabled,timezoneOffsetUtcMinutes,colorDepth,acceptHeader,userAgent,locale,ipAddress";

    @Resource(name = "dataMapper")
    private DataMapper dataMapper;

    @Resource(name = "checkoutFacade")
    private CheckoutFacade checkoutFacade;

    @Resource(name = "commerceWebServicesCartFacade2")
    private CartFacade cartFacade;

    @Resource(name = "ingenicoHelper")
    private IngenicoHelper ingenicoHelper;

    @Resource(name = "ingenicoCheckoutFacade")
    private IngenicoCheckoutFacade ingenicoCheckoutFacade;

    @Resource(name = "ingenicoPaymentDetailsWsDTOValidator")
    private IngenicoPaymentDetailsWsDTOValidator ingenicoPaymentDetailsWsDTOValidator;

    @Resource(name = "ingenicoBrowserDataWsDTOValidator")
    private IngenicoBrowserDataWsDTOValidator browserDataWsDTOValidator;

    @Secured({"ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT"})
    @RequestMapping(value = "/{cartId}/paymentproducts", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(nickname = "getCartPaymentProducts", value = "Get all available payment products for the current store and delivery address.", notes =
            "Returns all payment products supported for the "
                    + "current base store and cart delivery address. A delivery address must be set for the cart, otherwise an empty list will be returned.")
    @ApiBaseSiteIdUserIdAndCartIdParam
    public PaymentProductListWsDTO getCartPaymentProducts(
            @ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields) {
        final List<PaymentProduct> availablePaymentMethods = ingenicoCheckoutFacade.getAvailablePaymentMethods();

        GetPaymentProductsResponse productsResponse = new GetPaymentProductsResponse();
        productsResponse.setPaymentProducts(availablePaymentMethods);
        final PaymentProductListWsDTO paymentProductListWsDTO = getDataMapper()
                .map(productsResponse, PaymentProductListWsDTO.class, fields);

        ingenicoHelper.fillIdealIssuers(paymentProductListWsDTO, availablePaymentMethods, fields);

        return paymentProductListWsDTO;
    }

    @Secured({"ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT"})
    @RequestMapping(value = "/{cartId}/paymentproducts", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE})
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    @ApiOperation(nickname = "selectCartPaymentProduct", value = "selectCartPaymentProduct", notes = "selectCartPaymentProduct")
    @ApiBaseSiteIdUserIdAndCartIdParam
    public void selectCartPaymentProduct(@ApiParam(value = "Request body parameter that contains details \n\nThe DTO is in XML or .json format.", required = true)
                                         @RequestBody final IngenicoPaymentDetailsWsDTO ingenicoPaymentDetailsWsDTO,
                                         @ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields) throws CartException, IngenicoNonValidPaymentProductException {
        if (!checkoutFacade.hasCheckoutCart()) {
            throw new CartException("No cart found.", CartException.NOT_FOUND);
        }

        validate(ingenicoPaymentDetailsWsDTO, "ingenicoPaymentDetailsWsDTO", ingenicoPaymentDetailsWsDTOValidator);
        final IngenicoPaymentInfoData ingenicoPaymentInfoData = new IngenicoPaymentInfoData();

        ingenicoCheckoutFacade.fillIngenicoPaymentInfoData(ingenicoPaymentInfoData, ingenicoPaymentDetailsWsDTO.getPaymentProductId(), ingenicoPaymentDetailsWsDTO.getIssuerId());

        final AddressData addressData;
        if (Boolean.TRUE.equals(ingenicoPaymentDetailsWsDTO.isUseDeliveryAddress())) {
            addressData = getCheckoutFacade().getCheckoutCart().getDeliveryAddress();
            if (addressData == null) {
                throw new CartAddressException("DeliveryAddress is empty", CartAddressException.NOT_VALID);
            }
            addressData.setBillingAddress(Boolean.TRUE);
        } else {
            final AddressWsDTO addressWsDTO = ingenicoPaymentDetailsWsDTO.getBillingAddress();
            addressData = getDataMapper().map(addressWsDTO, AddressData.class, ADDRESS_MAPPING);
            addressData.setBillingAddress(Boolean.TRUE);
        }

        ingenicoPaymentInfoData.setBillingAddress(addressData);
        ingenicoCheckoutFacade.handlePaymentInfo(ingenicoPaymentInfoData);
    }

    @Secured({"ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT"})
    @RequestMapping(value = "/{cartId}/checkoutType", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(nickname = "getIngenicoCheckoutType", value = "Get Ingenico Checkout Type for the current Cart.", notes = "Get Ingenico Checkout Type for the current Cart.")
    @ApiBaseSiteIdUserIdAndCartIdParam
    public IngenicoCheckoutTypeWsDTO getCurrentIngenicoCheckoutType() {
        if (!checkoutFacade.hasCheckoutCart()) {
            throw new CartException("No cart found.", CartException.NOT_FOUND);
        }
        final CartData cartData = cartFacade.getSessionCart();
        if (cartData.getIngenicoPaymentInfo() == null) {
            throw new CartException("No Ingenico Payment Info found.", CartException.INVALID);
        }
        return getDataMapper().map(cartData.getIngenicoPaymentInfo().getIngenicoCheckoutType(), IngenicoCheckoutTypeWsDTO.class, "ingenicoCheckoutType");
    }


    @Secured({"ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT"})
    @RequestMapping(value = "/{cartId}/hostedtokenization", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(nickname = "getHostedTokenization", value = "Get ingenico hosted tokenization.", notes =
            "Returns a hosted tokenization for the current base store and cart. " +
                    "A delivery address must be set for the cart, otherwise an error will be returned.")
    @ApiBaseSiteIdUserIdAndCartIdParam
    public HostedTokenizationResponseWsDTO getHostedTokenization(
            @ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields) {
        if (!checkoutFacade.hasCheckoutCart()) {
            throw new CartException("No cart found.", CartException.NOT_FOUND);
        }
        final CartData cartData = cartFacade.getSessionCart();
        if (cartData.getIngenicoPaymentInfo() == null) {
            throw new CartException("No Ingenico Payment Info found.", CartException.INVALID);
        } else if (!IngenicoCheckoutTypesEnum.HOSTED_TOKENIZATION.equals(cartData.getIngenicoPaymentInfo().getIngenicoCheckoutType())) {
            throw new CartException("Invalid Ingenico Checkout Type.", CartException.INVALID);
        }

        final CreateHostedTokenizationResponse hostedTokenization = ingenicoCheckoutFacade.createHostedTokenization();
        final HostedTokenizationResponseWsDTO hostedTokenizationResponseWsDTO = getDataMapper()
                .map(hostedTokenization, HostedTokenizationResponseWsDTO.class, fields);

        ingenicoHelper.fillSavedPaymentDetails(hostedTokenizationResponseWsDTO, fields);

        return hostedTokenizationResponseWsDTO;
    }


    @Secured({"ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT"})
    @RequestMapping(value = "/{cartId}/hostedcheckout", method = RequestMethod.POST)
    @ResponseBody
    @ApiOperation(nickname = "createHostedCheckout", value = "create ingenico hosted checkout.", notes =
            "Returns a hosted checkout data for the current base store and cart. " +
                    "A delivery address must be set for the cart, otherwise an error will be returned.")
    @ApiBaseSiteIdUserIdAndCartIdParam
    public HostedCheckoutResponseWsDTO createHostedCheckout(
            @ApiParam(value = "Request body parameter that contains details \n\nThe DTO is in XML or .json format.", required = true)
            @RequestBody final BrowserDataWsDTO browserDataWsDTO,
            @ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields,
            final HttpServletRequest request) {
        if (!checkoutFacade.hasCheckoutCart()) {
            throw new CartException("No cart found.", CartException.NOT_FOUND);
        }
        final CartData cartData = cartFacade.getSessionCart();
        if (cartData.getIngenicoPaymentInfo() == null) {
            throw new CartException("No Ingenico Payment Info found.", CartException.INVALID);
        } else if (!IngenicoCheckoutTypesEnum.HOSTED_CHECKOUT.equals(cartData.getIngenicoPaymentInfo().getIngenicoCheckoutType())) {
            throw new CartException("Invalid Ingenico Checkout Type.", CartException.INVALID);
        }

        validate(browserDataWsDTO, "browserData", browserDataWsDTOValidator);
        final BrowserData browserData = getDataMapper().map(browserDataWsDTO, BrowserData.class, BROWSER_MAPPING);

        final String returnURL = ingenicoHelper.buildReturnURL(request, "ingenico.occ.hostedCheckout.returnUrl");
        sessionService.setAttribute("hostedCheckoutReturnUrl", returnURL);

        final CreateHostedCheckoutResponse createHostedCheckoutResponse = ingenicoCheckoutFacade.createHostedCheckout(browserData);
        final HostedCheckoutResponseWsDTO hostedCheckoutResponseWsDTO = getDataMapper()
                .map(createHostedCheckoutResponse, HostedCheckoutResponseWsDTO.class, fields);

        return hostedCheckoutResponseWsDTO;
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
