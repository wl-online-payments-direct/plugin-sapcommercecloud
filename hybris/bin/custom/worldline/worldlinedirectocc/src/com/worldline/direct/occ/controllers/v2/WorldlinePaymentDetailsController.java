package com.worldline.direct.occ.controllers.v2;


import javax.annotation.Resource;
import java.util.List;

import com.worldline.direct.facade.WorldlineUserFacade;
import de.hybris.platform.commercewebservicescommons.dto.order.PaymentDetailsListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.PaymentDetailsWsDTO;
import de.hybris.platform.webservicescommons.mapping.DataMapper;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdAndUserIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.worldline.direct.order.data.WorldlinePaymentInfoData;

@Controller
@RequestMapping(value = "/{baseSiteId}/users/{userId}/worldlinepaymentdetails")
@Api(tags = "Worldline PaymentDetails")
public class WorldlinePaymentDetailsController extends WorldlineBaseController {
    private final static Logger LOGGER = LoggerFactory.getLogger(WorldlinePaymentDetailsController.class);

    @Resource(name = "dataMapper")
    private DataMapper dataMapper;

    @Resource(name = "worldlineUserFacade")
    private WorldlineUserFacade worldlineUserFacade;

    @Secured({"ROLE_CUSTOMERGROUP", "ROLE_TRUSTED_CLIENT", "ROLE_CUSTOMERMANAGERGROUP"})
    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    @ApiOperation(nickname = "getSavedPaymentDetailsList", value = "Get saved customer's credit card payment details list.", notes = "Return saved customer's credit card payment details list.")
    @ApiBaseSiteIdAndUserIdParam
    public PaymentDetailsListWsDTO getSavedPaymentDetailsList(
            @ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields) {
        LOGGER.debug("[WORLDLINE] getSavedPaymentDetailsList");

        final List<WorldlinePaymentInfoData> worldlinePaymentInfos = worldlineUserFacade.getWorldlinePaymentInfos(true);
        getDataMapper().map(worldlinePaymentInfos.get(0), PaymentDetailsWsDTO.class, fields);
        final PaymentDetailsListWsDTO paymentDetailsListWsDTO = new PaymentDetailsListWsDTO();
        paymentDetailsListWsDTO.setPayments(getDataMapper().mapAsList(worldlinePaymentInfos, PaymentDetailsWsDTO.class, fields));

        return paymentDetailsListWsDTO;
    }

    @Secured({"ROLE_CUSTOMERGROUP", "ROLE_TRUSTED_CLIENT", "ROLE_CUSTOMERMANAGERGROUP"})
    @RequestMapping(value = "/{paymentDetailsId}", method = RequestMethod.DELETE)
    @ApiOperation(nickname = "removeSavedPaymentDetails", value = "Deletes saved customer's credit card payment details.", notes = "Deletes a saved customer's credit card payment details based on a specified paymentDetailsId.")
    @ApiBaseSiteIdAndUserIdParam
    @ResponseStatus(HttpStatus.OK)
    public void removePaymentDetails(
            @ApiParam(value = "Payment details identifier.", required = true) @PathVariable final String paymentDetailsId) {
        LOGGER.debug("[WORLDLINE] removePaymentDetails: id = {}", sanitize(paymentDetailsId));
        worldlineUserFacade.deleteSavedWorldlinePaymentInfo(paymentDetailsId);
    }


    public DataMapper getDataMapper() {
        return dataMapper;
    }

}
