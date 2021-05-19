package com.ingenico.ogone.direct.occ.controllers.v2;

import javax.annotation.Resource;

import de.hybris.platform.servicelayer.exceptions.ModelNotFoundException;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.webservicescommons.dto.error.ErrorListWsDTO;
import de.hybris.platform.webservicescommons.dto.error.ErrorWsDTO;
import de.hybris.platform.webservicescommons.errors.exceptions.WebserviceValidationException;
import de.hybris.platform.webservicescommons.mapping.DataMapper;
import de.hybris.platform.webservicescommons.util.YSanitizer;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

public class IngenicoBaseController {

    @Resource(name = "sessionService")
    protected SessionService sessionService;


    private final static Logger LOGGER = LoggerFactory.getLogger(IngenicoBaseController.class);


    protected static String logParam(final String paramName, final Long paramValue)
    {
        return paramName + " = " + paramValue;
    }

    protected static String logParam(final String paramName, final String paramValue)
    {
        return paramName + " = " + logValue(paramValue);
    }

    protected static String logValue(final String paramValue)
    {
        return "'" + sanitize(paramValue) + "'";
    }

    protected static String sanitize(final String input)
    {
        return YSanitizer.sanitize(input);
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ResponseBody
    @ExceptionHandler({ ModelNotFoundException.class })
    public ErrorListWsDTO handleModelNotFoundException(final Exception ex)
    {
        LOGGER.info("Handling Exception for this request - {} - {}", ex.getClass().getSimpleName(), sanitize(ex.getMessage()));
        LOGGER.debug("An exception occurred!", ex);

        return handleErrorInternal(UnknownIdentifierException.class.getSimpleName(), ex.getMessage());
    }

    protected ErrorListWsDTO handleErrorInternal(final String type, final String message)
    {
        final ErrorListWsDTO errorListDto = new ErrorListWsDTO();
        final ErrorWsDTO error = new ErrorWsDTO();
        error.setType(type.replace("Exception", "Error"));
        error.setMessage(sanitize(message));
        errorListDto.setErrors(Lists.newArrayList(error));
        return errorListDto;
    }

    protected void validate(final Object object, final String objectName, final Validator validator)
    {
        final Errors errors = new BeanPropertyBindingResult(object, objectName);
        validator.validate(object, errors);
        if (errors.hasErrors())
        {
            throw new WebserviceValidationException(errors);
        }
    }

}
