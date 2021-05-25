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
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "screenHeight", "field.required",new Object[] {"screenHeight"});
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "screenWidth", "field.required",new Object[] {"screenWidth"});
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "navigatorJavaEnabled", "field.required",new Object[] {"navigatorJavaEnabled"});
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "navigatorJavaScriptEnabled", "field.required",new Object[] {"navigatorJavaScriptEnabled"});
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "timezoneOffsetUtcMinutes", "field.required",new Object[] {"timezoneOffsetUtcMinutes"});
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "colorDepth", "field.required",new Object[] {"colorDepth"});
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "acceptHeader", "field.required",new Object[] {"acceptHeader"});
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "userAgent", "field.required",new Object[] {"userAgent"});
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "ipAddress", "field.required",new Object[] {"ipAddress"});
    }
}
