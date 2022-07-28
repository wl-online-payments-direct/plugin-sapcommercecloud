package com.worldline.direct.webhook.errors;


import com.onlinepayments.webhooks.ApiVersionMismatchException;
import com.onlinepayments.webhooks.SignatureValidationException;
import com.worldline.direct.exception.WorldlineNonValidWebhooksEventException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;


/**
 * Error Handler Base Controller
 */
@ControllerAdvice(basePackages = {"com.worldline.direct.webhook.controllers"})
public class WorldlineErrorsHandlerController {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldlineErrorsHandlerController.class);


    @ExceptionHandler(ApiVersionMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public String handleException(final ApiVersionMismatchException ex) {
        LOGGER.error("[WORLDLINE] Webhook : " + HttpStatus.BAD_REQUEST.getReasonPhrase(), ex);
        return ex.getMessage();
    }

    @ExceptionHandler(SignatureValidationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    public String handleException(final SignatureValidationException ex) {
        LOGGER.error("[WORLDLINE] Webhook : " + HttpStatus.UNAUTHORIZED.getReasonPhrase(), ex);

        return ex.getMessage();
    }

    @ExceptionHandler(WorldlineNonValidWebhooksEventException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public String handleException(final WorldlineNonValidWebhooksEventException ex) {
        LOGGER.error("[WORLDLINE] Webhook - invalid : " + ex.getMessage());
        return ex.getMessage();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public String handleException(final Exception ex) {
        LOGGER.error("[WORLDLINE] Webhook : " + HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), ex);
        return ex.getMessage();
    }
}
