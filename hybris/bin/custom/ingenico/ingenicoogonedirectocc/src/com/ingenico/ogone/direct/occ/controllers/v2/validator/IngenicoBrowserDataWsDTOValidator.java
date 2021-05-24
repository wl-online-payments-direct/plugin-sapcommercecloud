package com.ingenico.ogone.direct.occ.controllers.v2.validator;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import com.ingenico.ogone.direct.payment.dto.BrowserDataWsDTO;

@Component("ingenicoBrowserDataWsDTOValidator")
public class IngenicoBrowserDataWsDTOValidator implements Validator {

    @Override
    public boolean supports(final Class<?> aClass) {
        return BrowserDataWsDTO.class.equals(aClass);
    }

    @Override
    public void validate(final Object object, final Errors errors) {
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "screenHeight", "hostedTokenization.hostedTokenizationId.missing");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "screenWidth", "hostedTokenization.hostedTokenizationId.missing");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "navigatorJavaEnabled", "hostedTokenization.hostedTokenizationId.missing");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "timezoneOffsetUtcMinutes", "hostedTokenization.hostedTokenizationId.missing");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "colorDepth", "hostedTokenization.hostedTokenizationId.missing");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "acceptHeader", "hostedTokenization.hostedTokenizationId.missing");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "userAgent", "hostedTokenization.hostedTokenizationId.missing");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "ipAddress", "hostedTokenization.hostedTokenizationId.missing");
    }
}
