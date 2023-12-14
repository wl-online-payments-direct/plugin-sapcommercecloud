package com.worldline.direct.occ.controllers.v2;


import com.onlinepayments.domain.CreateHostedTokenizationResponse;
import com.onlinepayments.domain.GetPaymentProductsResponse;
import com.onlinepayments.domain.PaymentProduct;
import com.worldline.direct.enums.WorldlinePaymentProductFilterEnum;
import com.worldline.direct.exception.WorldlineNonValidPaymentProductException;
import com.worldline.direct.facade.WorldlineCheckoutFacade;
import com.worldline.direct.facade.WorldlineRecurringCheckoutFacade;
import com.worldline.direct.facade.WorldlineUserFacade;
import com.worldline.direct.factory.WorldlinePaymentProductFilterStrategyFactory;
import com.worldline.direct.occ.controllers.v2.validator.WorldlinePaymentDetailsWsDTOValidator;
import com.worldline.direct.occ.helpers.WorldlineHelper;
import com.worldline.direct.order.data.WorldlinePaymentInfoData;
import com.worldline.direct.payment.dto.HostedTokenizationResponseWsDTO;
import com.worldline.direct.payment.dto.PaymentProductListWsDTO;
import com.worldline.direct.payment.dto.WorldlineCheckoutTypeWsDTO;
import com.worldline.direct.payment.dto.WorldlinePaymentDetailsWsDTO;
import com.worldline.direct.service.WorldlineConfigurationService;
import com.worldline.direct.util.WorldlinePaymentProductUtils;
import de.hybris.platform.b2b.enums.CheckoutPaymentType;
import de.hybris.platform.b2bwebservicescommons.dto.order.DayOfWeekWsDTO;
import de.hybris.platform.b2bwebservicescommons.dto.order.ScheduleReplenishmentFormWsDTO;
import de.hybris.platform.commercefacades.order.CartFacade;
import de.hybris.platform.commercefacades.order.CheckoutFacade;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.user.UserFacade;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.commerceservices.strategies.CheckoutCustomerStrategy;
import de.hybris.platform.commercewebservicescommons.dto.order.PaymentDetailsListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.PaymentDetailsWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.user.AddressWsDTO;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.CartAddressException;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.CartException;
import de.hybris.platform.cronjob.enums.DayOfWeek;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.webservicescommons.mapping.DataMapper;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdUserIdAndCartIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

import static com.worldline.direct.occ.controllers.v2.WorldlineB2BOrdersController.OBJECT_NAME_SCHEDULE_REPLENISHMENT_FORM;

@Controller
@RequestMapping(value = "/{baseSiteId}/users/{userId}/carts")
@Tag(name = "Worldline Carts")
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

    @Resource(name = "worldlinePaymentProductFilterStrategyFactory")
    private WorldlinePaymentProductFilterStrategyFactory worldlinePaymentProductFilterStrategyFactory;

    @Resource(name = "worldlineConfigurationService")
    private WorldlineConfigurationService worldlineConfigurationService;

    @Resource(name = "checkoutCustomerStrategy")
    private CheckoutCustomerStrategy checkoutCustomerStrategy;

    @Resource(name = "userFacade")
    private UserFacade userFacade;

    @Resource(name = "worldlineRecurringCheckoutFacade")
    private WorldlineRecurringCheckoutFacade worldlineRecurringCheckoutFacade;

    @Resource(name = "scheduleReplenishmentFormWsDTOValidator")
    private Validator scheduleReplenishmentFormWsDTOValidator;

    @Secured({"ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT"})
    @GetMapping(value = "/{cartId}/paymentProducts")
    @ResponseBody
    @Operation(operationId = "getCartPaymentProducts", summary = "Get all available payment products for the current store and delivery address.", description =
            "Returns all payment products supported for the "
                    + "current base store and cart delivery address. A delivery address must be set for the cart, otherwise an empty list will be returned.")
    @ApiBaseSiteIdUserIdAndCartIdParam
    public PaymentProductListWsDTO getCartPaymentProducts(
            @ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields) {
        final List<PaymentProduct> availablePaymentMethods = worldlinePaymentProductFilterStrategyFactory.filter(worldlineCheckoutFacade.getAvailablePaymentMethods(), WorldlinePaymentProductFilterEnum.ACTIVE_PAYMENTS, WorldlinePaymentProductFilterEnum.CHECKOUT_TYPE,WorldlinePaymentProductFilterEnum.GROUP_CARDS).get();

        GetPaymentProductsResponse productsResponse = new GetPaymentProductsResponse();
        productsResponse.setPaymentProducts(availablePaymentMethods);
        final PaymentProductListWsDTO paymentProductListWsDTO = getDataMapper()
                .map(productsResponse, PaymentProductListWsDTO.class, fields);

        worldlineHelper.fillIdealIssuers(paymentProductListWsDTO, availablePaymentMethods, fields);

        return paymentProductListWsDTO;
    }

    @Secured({"ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT"})
    @GetMapping(value = "/{cartId}/worldlinePaymentdetails")
    @ResponseBody
    @Operation(operationId = "getSavedPaymentDetailsListForCheckout", summary = "Get saved customer's credit card payment details list for checkout.", description = "Return saved customer's credit card payment details list for checkout.")
    @ApiBaseSiteIdUserIdAndCartIdParam
    public PaymentDetailsListWsDTO getSavedPaymentDetailsListForCheckout(@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields) {
        final List<PaymentProduct> availablePaymentMethods = worldlineCheckoutFacade.getAvailablePaymentMethods();
        final List<WorldlinePaymentInfoData> worldlinePaymentInfoDataList = worldlineUserFacade.getWorldlinePaymentInfosForPaymentProducts(availablePaymentMethods, Boolean.TRUE);
        final PaymentDetailsListWsDTO paymentDetailsListWsDTO = new PaymentDetailsListWsDTO();
        paymentDetailsListWsDTO.setPayments(dataMapper.mapAsList(worldlinePaymentInfoDataList, PaymentDetailsWsDTO.class, fields));

        return paymentDetailsListWsDTO;
    }

    @Secured({"ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT"})
    @PostMapping(value = "/{cartId}/paymentProducts", consumes = {MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE})
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    @Operation(operationId = "selectCartPaymentProduct", summary = "selectCartPaymentProduct", description = "selectCartPaymentProduct")
    @ApiBaseSiteIdUserIdAndCartIdParam
    public void selectCartPaymentProduct(@Parameter(description = "Request body parameter that contains details \n\nThe DTO is in XML or .json format.", required = true)
                                         @RequestBody final WorldlinePaymentDetailsWsDTO worldlinePaymentDetailsWsDTO,
                                         @ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields) throws CartException, WorldlineNonValidPaymentProductException {
        if (!checkoutFacade.hasCheckoutCart()) {
            throw new CartException("No cart found.", CartException.NOT_FOUND);
        }

        validate(worldlinePaymentDetailsWsDTO, "worldlinePaymentDetailsWsDTO", worldlinePaymentDetailsWsDTOValidator);
        final WorldlinePaymentInfoData worldlinePaymentInfoData = new WorldlinePaymentInfoData();

        worldlineCheckoutFacade.fillWorldlinePaymentInfoData(worldlinePaymentInfoData,
                worldlinePaymentDetailsWsDTO.getSavedCardCode(),
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
    @GetMapping(value = "/{cartId}/checkoutType")
    @ResponseBody
    @Operation(operationId = "getWorldlineCheckoutType", summary = "Get Worldline Checkout Type for the current Cart.", description = "Get Worldline Checkout Type for the current Cart.")
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
    @GetMapping(value = "/{cartId}/hostedTokenization")
    @ResponseBody
    @Operation(operationId = "getHostedTokenization", summary = "Get worldline hosted tokenization.", description =
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

        hostedTokenizationResponseWsDTO.setCheckoutType(getDataMapper().map(worldlineCheckoutFacade.getWorldlineCheckoutType(), WorldlineCheckoutTypeWsDTO.class, "worldlineCheckoutType"));
        worldlineHelper.fillSavedPaymentDetails(hostedTokenizationResponseWsDTO, fields);

        return hostedTokenizationResponseWsDTO;
    }

    @Secured({"ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT"})
    @GetMapping(value = "/{cartId}/supportRecurring")
    @ResponseBody
    @Operation(operationId = "supportRecurring", summary = "Check if Cart is allowed to create replenishment.", description =
            "return True if the payment Type is Account or if the payment mode allow recurring payment.")
    @ApiBaseSiteIdUserIdAndCartIdParam
    public Boolean supportRecurringPayment(
            @ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields) {
        if (!checkoutCustomerStrategy.isAnonymousCheckout()) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    @Secured({"ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT"})
    @GetMapping(value = "/{cartId}/displaySurcharge")
    @ResponseBody
    @Operation(operationId = "displaySurcharge", summary = "Check if surcharge should be applied.", description =
          "return True if the payment can be paid by card and if the surcharge could be applied.")
    @ApiBaseSiteIdUserIdAndCartIdParam
    public Boolean displaySurcharge (
          @ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields) {
        if (!checkoutFacade.hasCheckoutCart()) {
            throw new CartException("No cart found.", CartException.NOT_FOUND);
        }
        final List<PaymentProduct> availablePaymentMethods = worldlinePaymentProductFilterStrategyFactory.filter(worldlineCheckoutFacade.getAvailablePaymentMethods(), WorldlinePaymentProductFilterEnum.ACTIVE_PAYMENTS, WorldlinePaymentProductFilterEnum.CHECKOUT_TYPE, WorldlinePaymentProductFilterEnum.GROUP_CARDS).get();

        return worldlineConfigurationService.getCurrentWorldlineConfiguration().isApplySurcharge() &&
               worldlineCheckoutFacade.checkForCardPaymentMethods(availablePaymentMethods);
    }

    @Secured({"ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT"})
    @GetMapping(value = "/{cartId}/isTokenTemporary")
    @ResponseBody
    @Operation(operationId = "isTokenTemporary", summary = "Check if payment details are going to be saved.", description =
          "return True if the payment can be paid by card and if the payment details are marked to be saved.")
    @ApiBaseSiteIdUserIdAndCartIdParam
    public Boolean isTokenTemporary () {
        if (!checkoutFacade.hasCheckoutCart()) {
            throw new CartException("No cart found.", CartException.NOT_FOUND);
        }
        final CartData cartData = cartFacade.getSessionCart();
        if (cartData.getWorldlinePaymentInfo() != null) {
            return WorldlinePaymentProductUtils.isCreditCard(cartData.getWorldlinePaymentInfo()) && !worldlineCheckoutFacade.isTemporaryToken(cartData.getWorldlinePaymentInfo().getHostedTokenizationId());
        }
        return Boolean.FALSE;
    }

    @PostMapping(value = "/{cartId}/saveReplenishmentOrderData", consumes = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    @ApiBaseSiteIdUserIdAndCartIdParam
    @Operation(operationId = "saveReplenishmentOrderData", summary = "Save the Replenishment order data", description = "Save the Replenishment order data")
        public void saveReplenishmentOrderData (
              @Parameter(description = "Request body parameter that contains details. The DTO is in XML or .json format.", required = true) @RequestBody final ScheduleReplenishmentFormWsDTO scheduleReplenishmentFormWsDTO,
              final HttpServletRequest request)
              throws InvalidCartException {

            validateUser();

            validateScheduleReplenishmentForm(scheduleReplenishmentFormWsDTO);
            worldlineRecurringCheckoutFacade.saveReplenishmentData(Boolean.TRUE,
                  scheduleReplenishmentFormWsDTO.getReplenishmentStartDate(),
                  scheduleReplenishmentFormWsDTO.getReplenishmentEndDate(),
                  scheduleReplenishmentFormWsDTO.getNumberOfDays(),
                  scheduleReplenishmentFormWsDTO.getNumberOfWeeks(),
                  scheduleReplenishmentFormWsDTO.getNumberOfMonths(),
                  scheduleReplenishmentFormWsDTO.getNthDayOfMonth(),
                  scheduleReplenishmentFormWsDTO.getDaysOfWeek().isEmpty() ? new ArrayList<>() : convertDTO(scheduleReplenishmentFormWsDTO.getDaysOfWeek()),
                  scheduleReplenishmentFormWsDTO.getRecurrencePeriod());
        }

    @GetMapping(value = "/{cartId}/getReplenishmentOrderData")
    @ResponseBody
    @ApiBaseSiteIdUserIdAndCartIdParam
    @Operation(operationId = "getReplenishmentOrderData", summary = "Get the Replenishment order data", description = "Get the Replenishment order data")
    public ScheduleReplenishmentFormWsDTO getReplenishmentData() {
        final CartData cartData = cartFacade.getSessionCart();
        ScheduleReplenishmentFormWsDTO replenishmentData = new ScheduleReplenishmentFormWsDTO();
        if (cartData.isReplenishmentOrder()) {
            replenishmentData.setIsReplenishmentOrder(cartData.isReplenishmentOrder());
            replenishmentData.setDaysOfWeek(convertDayOfWeek(cartData.getNDaysOfWeek()));
            replenishmentData.setNumberOfWeeks(cartData.getNWeeks());
            replenishmentData.setNumberOfDays(cartData.getNDays());
            replenishmentData.setRecurrencePeriod(cartData.getReplenishmentRecurrence().name());
            replenishmentData.setNumberOfMonths(cartData.getNMonths());
            replenishmentData.setReplenishmentEndDate(cartData.getReplenishmentEndDate());
            replenishmentData.setReplenishmentStartDate(cartData.getReplenishmentStartDate());
        }
        return replenishmentData;
    }

    private List<DayOfWeekWsDTO> convertDayOfWeek(List<DayOfWeek> daysOfWeek) {
        if (!daysOfWeek.isEmpty()) {
            List<DayOfWeekWsDTO> resultList = new ArrayList<>();
            for (DayOfWeek currentDay : daysOfWeek) {
                resultList.add(DayOfWeekWsDTO.valueOf(currentDay.name()));
            }
            return resultList;
        }
        return null;
    }

    private List<String> convertDTO(List<DayOfWeekWsDTO> daysOfWeek) {
        List<String> stringDays = new ArrayList<>();
        for (DayOfWeekWsDTO currentDay : daysOfWeek) {
            stringDays.add(currentDay.name());
        }
        return stringDays;
    }
    protected void validateUser() {
        if (userFacade.isAnonymousUser()) {
            throw new AccessDeniedException("Access is denied");
        }
    }

    protected void validateScheduleReplenishmentForm(ScheduleReplenishmentFormWsDTO scheduleReplenishmentForm) {
        validate(scheduleReplenishmentForm, OBJECT_NAME_SCHEDULE_REPLENISHMENT_FORM, scheduleReplenishmentFormWsDTOValidator);
    }

    public DataMapper getDataMapper() {
        return dataMapper;
    }

    public CheckoutFacade getCheckoutFacade() {
        return checkoutFacade;
    }

}
