package com.ingenico.ogone.direct.webhook.errors;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.ingenico.direct.webhooks.ApiVersionMismatchException;
import com.ingenico.direct.webhooks.SignatureValidationException;


/**
 * Error Handler Base Controller
 */
@ControllerAdvice(basePackages = {"com.ingenico.ogone.direct.webhook.controllers"})
public class IngenicoErrorsHandlerController {
    private static final Logger LOGGER = LoggerFactory.getLogger(IngenicoErrorsHandlerController.class);


    @ExceptionHandler(ApiVersionMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public String handleException(final ApiVersionMismatchException ex) {
        LOGGER.error("[INGENICO] Webhook : " + HttpStatus.BAD_REQUEST.getReasonPhrase(), ex);
        return ex.getMessage();
    }

    @ExceptionHandler(SignatureValidationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    public String handleException(final SignatureValidationException ex) {
        LOGGER.error("[INGENICO] Webhook : " + HttpStatus.UNAUTHORIZED.getReasonPhrase(), ex);

        return ex.getMessage();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public String handleException(final Exception ex) {
        LOGGER.error("[INGENICO] Webhook : " + HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), ex);
        return ex.getMessage();
    }
}
