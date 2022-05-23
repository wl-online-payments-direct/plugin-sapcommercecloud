package com.worldline.direct.occ.controllers.v2;


import com.worldline.direct.facade.WorldlineUserFacade;
import com.worldline.direct.order.data.WorldlinePaymentInfoData;
import de.hybris.platform.commercefacades.user.UserFacade;
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
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Controller
@RequestMapping(value = "/{baseSiteId}/users/{userId}/worldlinepaymentdetails")
@Api(tags = "Worldline PaymentDetails")
public class WorldlinePaymentDetailsController extends WorldlineBaseController {
    private final static Logger LOGGER = LoggerFactory.getLogger(WorldlinePaymentDetailsController.class);

    @Resource(name = "dataMapper")
    private DataMapper dataMapper;

    @Resource(name = "userFacade")
    private UserFacade userFacade;
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

    @Secured({"ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT"})
    @RequestMapping(value = "/{paymentDetailsId}", method = RequestMethod.PATCH)
    @ApiBaseSiteIdAndUserIdParam
    @ResponseStatus(HttpStatus.OK)
    public void updatePaymentDetails(@ApiParam(value = "Payment details identifier.", required = true) @PathVariable final String paymentDetailsId) {
        LOGGER.debug("[WORLDLINE] updatePaymentDetails: id = {}", sanitize(paymentDetailsId));
        final WorldlinePaymentInfoData paymentInfoData = worldlineUserFacade.getWorldlinePaymentInfoByCode(paymentDetailsId);
        final boolean isAlreadyDefaultPaymentInfo = paymentInfoData.isDefaultPayment();
        if (paymentInfoData.isSaved() && !isAlreadyDefaultPaymentInfo ) {
            worldlineUserFacade.setDefaultPaymentInfo(paymentInfoData);
        }
    }


    public DataMapper getDataMapper() {
        return dataMapper;
    }

}
