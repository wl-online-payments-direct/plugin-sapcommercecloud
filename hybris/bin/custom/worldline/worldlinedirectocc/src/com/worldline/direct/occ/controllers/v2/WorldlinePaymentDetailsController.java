package com.worldline.direct.occ.controllers.v2;


import com.worldline.direct.facade.WorldlineUserFacade;
import com.worldline.direct.order.data.WorldlinePaymentInfoData;
import de.hybris.platform.commercefacades.user.UserFacade;
import de.hybris.platform.commercewebservicescommons.dto.order.PaymentDetailsListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.PaymentDetailsWsDTO;
import de.hybris.platform.webservicescommons.mapping.DataMapper;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdAndUserIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Controller
@RequestMapping(value = "/{baseSiteId}/users/{userId}/worldlinePaymentDetails")
@Tag(name = "Worldline PaymentDetails")
public class WorldlinePaymentDetailsController extends WorldlineBaseController {
    private final static Logger LOGGER = LoggerFactory.getLogger(WorldlinePaymentDetailsController.class);

    @Resource(name = "dataMapper")
    private DataMapper dataMapper;

    @Resource(name = "userFacade")
    private UserFacade userFacade;
    @Resource(name = "worldlineUserFacade")
    private WorldlineUserFacade worldlineUserFacade;

    @Secured({"ROLE_CUSTOMERGROUP", "ROLE_TRUSTED_CLIENT", "ROLE_CUSTOMERMANAGERGROUP"})
    @GetMapping
    @ResponseBody
    @Operation(operationId = "getSavedPaymentDetailsList", summary = "Get saved customer's credit card payment details list.", description = "Return saved customer's credit card payment details list.")
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
    @DeleteMapping(value = "/{paymentDetailsId}")
    @Operation(operationId = "removeSavedPaymentDetails", summary = "Deletes saved customer's credit card payment details.", description = "Deletes a saved customer's credit card payment details based on a specified paymentDetailsId.")
    @ApiBaseSiteIdAndUserIdParam
    @ResponseStatus(HttpStatus.OK)
    public void removePaymentDetails(
            @Parameter(description = "Payment details identifier.", required = true) @PathVariable final String paymentDetailsId) {
        LOGGER.debug("[WORLDLINE] removePaymentDetails: id = {}", sanitize(paymentDetailsId));
        worldlineUserFacade.deleteSavedWorldlinePaymentInfo(paymentDetailsId);
    }

    @Secured({"ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT"})
    @PatchMapping(value = "/{paymentDetailsId}")
    @ApiBaseSiteIdAndUserIdParam
    @ResponseStatus(HttpStatus.OK)
    public void updatePaymentDetails(@Parameter(description = "Payment details identifier.", required = true) @PathVariable final String paymentDetailsId) {
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
