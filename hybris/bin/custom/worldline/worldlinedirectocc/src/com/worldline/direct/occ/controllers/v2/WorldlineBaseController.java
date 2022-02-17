package com.worldline.direct.occ.controllers.v2;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import de.hybris.platform.servicelayer.exceptions.ModelNotFoundException;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.webservicescommons.dto.error.ErrorListWsDTO;
import de.hybris.platform.webservicescommons.dto.error.ErrorWsDTO;
import de.hybris.platform.webservicescommons.errors.exceptions.WebserviceValidationException;
import de.hybris.platform.webservicescommons.mapping.FieldSetLevelHelper;
import de.hybris.platform.webservicescommons.util.YSanitizer;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.ingenico.direct.ApiException;
import com.worldline.direct.exception.WorldlineNonAuthorizedPaymentException;
import com.worldline.direct.order.data.BrowserData;
import com.worldline.direct.payment.dto.NonAuthorizedPaymentWsDTO;

public class WorldlineBaseController {

    private static final String ACCEPT = "accept";
    private static final String USER_AGENT = "user-agent";
    private static final String X_FORWARDED_FOR = "X-FORWARDED-FOR";
    protected static final String BROWSER_MAPPING = "screenHeight,screenWidth,navigatorJavaEnabled,navigatorJavaScriptEnabled,timezoneOffsetUtcMinutes,colorDepth";
    protected static final String ADDRESS_MAPPING = "firstName,lastName,titleCode,phone,line1,line2,town,postalCode,country(isocode),region(isocode),defaultAddress";

    @Resource(name = "sessionService")
    protected SessionService sessionService;


    private static final Logger LOGGER = LoggerFactory.getLogger(WorldlineBaseController.class);
    protected static final String DEFAULT_FIELD_SET = FieldSetLevelHelper.DEFAULT_LEVEL;

    protected static String logParam(final String paramName, final Long paramValue) {
        return paramName + " = " + paramValue;
    }

    protected static String logParam(final String paramName, final String paramValue) {
        return paramName + " = " + logValue(paramValue);
    }

    protected static String logValue(final String paramValue) {
        return "'" + sanitize(paramValue) + "'";
    }

    protected static String sanitize(final String input) {
        return YSanitizer.sanitize(input);
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ResponseBody
    @ExceptionHandler({ModelNotFoundException.class})
    public ErrorListWsDTO handleModelNotFoundException(final Exception ex) {
        LOGGER.info("Handling Exception for this request - {} - {}", ex.getClass().getSimpleName(), sanitize(ex.getMessage()));
        LOGGER.debug("An exception occurred!", ex);

        return handleErrorInternal(UnknownIdentifierException.class.getSimpleName(), ex.getMessage());
    }

    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    @ExceptionHandler({ApiException.class})
    public ErrorListWsDTO handleModelNotFoundException(final ApiException ex) {
        LOGGER.debug("An exception occurred while using Worldline API!", ex);
        return handleErrorInternal(ApiException.class.getSimpleName(), ex.getMessage());
    }


    @ResponseStatus(value = HttpStatus.OK)
    @ResponseBody
    @ExceptionHandler({WorldlineNonAuthorizedPaymentException.class})
    public NonAuthorizedPaymentWsDTO handleWorldlineNonAuthorizedPaymentException(final WorldlineNonAuthorizedPaymentException exception) {
        LOGGER.info("Handling Exception for this request - {} - {}", exception.getClass().getSimpleName(), sanitize(exception.getMessage()));
        NonAuthorizedPaymentWsDTO nonAuthorizedPaymentWsDTO = new NonAuthorizedPaymentWsDTO();
        nonAuthorizedPaymentWsDTO.setStatus(exception.getReason().toString());
        if(exception.getMerchantAction()!=null) {
            nonAuthorizedPaymentWsDTO.setRedirectTo(exception
                    .getMerchantAction()
                    .getRedirectData()
                    .getRedirectURL());
        }

        return nonAuthorizedPaymentWsDTO;
    }

    protected ErrorListWsDTO handleErrorInternal(final String type, final String message) {
        final ErrorListWsDTO errorListDto = new ErrorListWsDTO();
        final ErrorWsDTO error = new ErrorWsDTO();
        error.setType(type.replace("Exception", "Error"));
        error.setMessage(sanitize(message));
        errorListDto.setErrors(Lists.newArrayList(error));
        return errorListDto;
    }

    protected void validate(final Object object, final String objectName, final Validator validator) {
        final Errors errors = new BeanPropertyBindingResult(object, objectName);
        validator.validate(object, errors);
        if (errors.hasErrors()) {
            throw new WebserviceValidationException(errors);
        }
    }

    protected void fillBrowserData(HttpServletRequest request, BrowserData browserData) {
        browserData.setAcceptHeader(request.getHeader(ACCEPT));
        browserData.setUserAgent(request.getHeader(USER_AGENT));
        browserData.setLocale(request.getLocale().toString());
        browserData.setIpAddress(getIpAddress(request));
    }

    protected String getIpAddress(HttpServletRequest request) {
        String remoteAddr = "";
        if (request != null) {
            remoteAddr = request.getHeader(X_FORWARDED_FOR);
            if (StringUtils.isEmpty(remoteAddr)) {
                remoteAddr = request.getRemoteAddr();
            }
        }
        return remoteAddr;
    }

}
