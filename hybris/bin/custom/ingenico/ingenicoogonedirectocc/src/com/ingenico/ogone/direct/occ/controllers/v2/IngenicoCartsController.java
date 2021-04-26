package com.ingenico.ogone.direct.occ.controllers.v2;

import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_METHOD_IDEAL;
import static com.ingenico.ogone.direct.occ.controllers.IngenicoogonedirectoccControllerConstants.DEFAULT_FIELD_SET;

import javax.annotation.Resource;
import java.util.List;

import de.hybris.platform.webservicescommons.mapping.DataMapper;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdUserIdAndCartIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.ingenico.direct.domain.DirectoryEntry;
import com.ingenico.direct.domain.GetPaymentProductsResponse;
import com.ingenico.direct.domain.PaymentProduct;
import com.ingenico.ogone.direct.facade.IngenicoCheckoutFacade;
import com.ingenico.ogone.direct.occ.helpers.IngenicoHelper;
import com.ingenico.ogone.direct.payment.dto.DirectoryEntryListWsDTO;
import com.ingenico.ogone.direct.payment.dto.PaymentProductListWsDTO;

@Controller
@RequestMapping(value = "/{baseSiteId}/users/{userId}/carts")
@Api(tags = "Ingenico Carts")
public class IngenicoCartsController {
    private final static Logger LOGGER = LoggerFactory.getLogger(IngenicoCartsController.class);

    @Resource(name = "dataMapper")
    private DataMapper dataMapper;

    @Resource(name = "ingenicoHelper")
    private IngenicoHelper ingenicoHelper;

    @Resource(name = "ingenicoCheckoutFacade")
    private IngenicoCheckoutFacade ingenicoCheckoutFacade;

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
        final PaymentProductListWsDTO paymentProductListWsDTO = getDataMapper().map(productsResponse, PaymentProductListWsDTO.class, fields);

        ingenicoHelper.fillIdealIssuers(paymentProductListWsDTO, availablePaymentMethods, fields);

        return paymentProductListWsDTO;
    }


    public DataMapper getDataMapper() {
        return dataMapper;
    }
}
