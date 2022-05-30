package com.worldline.direct.occ.controllers.v2;


import com.ingenico.direct.domain.CreateHostedTokenizationResponse;
import com.ingenico.direct.domain.GetPaymentProductsResponse;
import com.ingenico.direct.domain.PaymentProduct;
import com.worldline.direct.exception.WorldlineNonValidPaymentProductException;
import com.worldline.direct.facade.WorldlineCheckoutFacade;
import com.worldline.direct.facade.WorldlineUserFacade;
import com.worldline.direct.occ.controllers.v2.validator.WorldlinePaymentDetailsWsDTOValidator;
import com.worldline.direct.occ.helpers.WorldlineHelper;
import com.worldline.direct.order.data.WorldlinePaymentInfoData;
import com.worldline.direct.payment.dto.HostedTokenizationResponseWsDTO;
import com.worldline.direct.payment.dto.PaymentProductListWsDTO;
import com.worldline.direct.payment.dto.WorldlineCheckoutTypeWsDTO;
import com.worldline.direct.payment.dto.WorldlinePaymentDetailsWsDTO;
import com.worldline.direct.util.WorldlinePaymentProductUtils;

import de.hybris.platform.commercefacades.order.CartFacade;
import de.hybris.platform.commercefacades.order.CheckoutFacade;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.commercewebservicescommons.dto.order.PaymentDetailsListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.PaymentDetailsWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.user.AddressWsDTO;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.CartAddressException;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.CartException;
import de.hybris.platform.webservicescommons.mapping.DataMapper;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdUserIdAndCartIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Controller
@RequestMapping(value = "/{baseSiteId}/users/{userId}/carts")
@Api(tags = "Worldline Carts")
public class WorldlineCartsController extends WorldlineBaseController {
    private final static Logger LOGGER = LoggerFactory.getLogger(WorldlineCartsController.class);

    @Resource(name = "dataMapper")
    private DataMapper dataMapper;

    @Resource(name = "checkoutFacade")
    private CheckoutFacade checkoutFacade;

    @Resource(name = "commerceWebServicesCartFacade2")
    private CartFacade cartFacade;

    @Resource(name = "worldlineHelper")
    private WorldlineHelper worldlineHelper;

    @Resource(name = "worldlineCheckoutFacade")
    private WorldlineCheckoutFacade worldlineCheckoutFacade;

    @Resource(name = "worldlineUserFacade")
    private WorldlineUserFacade worldlineUserFacade;

    @Resource(name = "worldlinePaymentDetailsWsDTOValidator")
    private WorldlinePaymentDetailsWsDTOValidator worldlinePaymentDetailsWsDTOValidator;

    @Resource(name = "worldlinePaymentProductUtils")
    private WorldlinePaymentProductUtils worldlinePaymentProductUtils;


    @Secured({"ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT"})
    @RequestMapping(value = "/{cartId}/paymentproducts", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(nickname = "getCartPaymentProducts", value = "Get all available payment products for the current store and delivery address.", notes =
            "Returns all payment products supported for the "
                    + "current base store and cart delivery address. A delivery address must be set for the cart, otherwise an empty list will be returned.")
    @ApiBaseSiteIdUserIdAndCartIdParam
    public PaymentProductListWsDTO getCartPaymentProducts(
            @ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields) {
        final List<PaymentProduct> availablePaymentMethods = worldlinePaymentProductUtils.filterByCheckoutType(worldlinePaymentProductUtils.filterByAvailablePaymentModes(worldlineCheckoutFacade.getAvailablePaymentMethods()));

        GetPaymentProductsResponse productsResponse = new GetPaymentProductsResponse();
        productsResponse.setPaymentProducts(availablePaymentMethods);
        final PaymentProductListWsDTO paymentProductListWsDTO = getDataMapper()
                .map(productsResponse, PaymentProductListWsDTO.class, fields);

        worldlineHelper.fillIdealIssuers(paymentProductListWsDTO, availablePaymentMethods, fields);

        return paymentProductListWsDTO;
    }

    @Secured({"ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT"})
    @RequestMapping(value = "/{cartId}/worldlinepaymentdetails", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(nickname = "getSavedPaymentDetailsListForCheckout", value = "Get saved customer's credit card payment details list for checkout.", notes = "Return saved customer's credit card payment details list for checkout.")
    @ApiBaseSiteIdUserIdAndCartIdParam
    public PaymentDetailsListWsDTO getSavedPaymentDetailsListForCheckout(@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields) {
        final List<PaymentProduct> availablePaymentMethods = worldlineCheckoutFacade.getAvailablePaymentMethods();
        final List<WorldlinePaymentInfoData> worldlinePaymentInfoDataList = worldlineUserFacade.getWorldlinePaymentInfosForPaymentProducts(availablePaymentMethods, Boolean.TRUE);
        final PaymentDetailsListWsDTO paymentDetailsListWsDTO = new PaymentDetailsListWsDTO();
        paymentDetailsListWsDTO.setPayments(dataMapper.mapAsList(worldlinePaymentInfoDataList, PaymentDetailsWsDTO.class, fields));

        return paymentDetailsListWsDTO;
    }

    @Secured({"ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT"})
    @RequestMapping(value = "/{cartId}/paymentproducts", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE})
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    @ApiOperation(nickname = "selectCartPaymentProduct", value = "selectCartPaymentProduct", notes = "selectCartPaymentProduct")
    @ApiBaseSiteIdUserIdAndCartIdParam
    public void selectCartPaymentProduct(@ApiParam(value = "Request body parameter that contains details \n\nThe DTO is in XML or .json format.", required = true)
                                         @RequestBody final WorldlinePaymentDetailsWsDTO worldlinePaymentDetailsWsDTO,
                                         @ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields) throws CartException, WorldlineNonValidPaymentProductException {
        if (!checkoutFacade.hasCheckoutCart()) {
            throw new CartException("No cart found.", CartException.NOT_FOUND);
        }

        validate(worldlinePaymentDetailsWsDTO, "worldlinePaymentDetailsWsDTO", worldlinePaymentDetailsWsDTOValidator);
        final WorldlinePaymentInfoData worldlinePaymentInfoData = new WorldlinePaymentInfoData();

        worldlineCheckoutFacade.fillWorldlinePaymentInfoData(worldlinePaymentInfoData,
                StringUtils.EMPTY,
                worldlinePaymentDetailsWsDTO.getPaymentProductId(),
                worldlinePaymentDetailsWsDTO.getIssuerId(),
                worldlinePaymentDetailsWsDTO.getHostedTokenizationId());

        final AddressData addressData;
        if (Boolean.TRUE.equals(worldlinePaymentDetailsWsDTO.isUseDeliveryAddress())) {
            addressData = getCheckoutFacade().getCheckoutCart().getDeliveryAddress();
            if (addressData == null) {
                throw new CartAddressException("DeliveryAddress is empty", CartAddressException.NOT_VALID);
            }
            addressData.setBillingAddress(Boolean.TRUE);
        } else {
            final AddressWsDTO addressWsDTO = worldlinePaymentDetailsWsDTO.getBillingAddress();
            addressData = getDataMapper().map(addressWsDTO, AddressData.class, ADDRESS_MAPPING);
            addressData.setBillingAddress(Boolean.TRUE);
        }

        worldlinePaymentInfoData.setBillingAddress(addressData);
        worldlineCheckoutFacade.handlePaymentInfo(worldlinePaymentInfoData);
    }

    @Secured({"ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT"})
    @RequestMapping(value = "/{cartId}/checkoutType", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(nickname = "getWorldlineCheckoutType", value = "Get Worldline Checkout Type for the current Cart.", notes = "Get Worldline Checkout Type for the current Cart.")
    @ApiBaseSiteIdUserIdAndCartIdParam
    public WorldlineCheckoutTypeWsDTO getCurrentWorldlineCheckoutType() {
        if (!checkoutFacade.hasCheckoutCart()) {
            throw new CartException("No cart found.", CartException.NOT_FOUND);
        }
        final CartData cartData = cartFacade.getSessionCart();
        if (cartData.getWorldlinePaymentInfo() == null) {
            throw new CartException("No Worldline Payment Info found.", CartException.INVALID);
        }
        return getDataMapper().map(cartData.getWorldlinePaymentInfo().getWorldlineCheckoutType(), WorldlineCheckoutTypeWsDTO.class, "worldlineCheckoutType");
    }


    @Secured({"ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT"})
    @RequestMapping(value = "/{cartId}/hostedtokenization", method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(nickname = "getHostedTokenization", value = "Get worldline hosted tokenization.", notes =
            "Returns a hosted tokenization for the current base store and cart. " +
                    "A delivery address must be set for the cart, otherwise an error will be returned.")
    @ApiBaseSiteIdUserIdAndCartIdParam
    public HostedTokenizationResponseWsDTO getHostedTokenization(
            @ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields) {
        if (!checkoutFacade.hasCheckoutCart()) {
            throw new CartException("No cart found.", CartException.NOT_FOUND);
        }

        final CreateHostedTokenizationResponse hostedTokenization = worldlineCheckoutFacade.createHostedTokenization();
        final HostedTokenizationResponseWsDTO hostedTokenizationResponseWsDTO = getDataMapper()
                .map(hostedTokenization, HostedTokenizationResponseWsDTO.class, fields);

        hostedTokenizationResponseWsDTO.setCheckoutType( getDataMapper().map(worldlineCheckoutFacade.getWorldlineCheckoutType(),WorldlineCheckoutTypeWsDTO.class,"worldlineCheckoutType"));
        worldlineHelper.fillSavedPaymentDetails(hostedTokenizationResponseWsDTO, fields);

        return hostedTokenizationResponseWsDTO;
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
