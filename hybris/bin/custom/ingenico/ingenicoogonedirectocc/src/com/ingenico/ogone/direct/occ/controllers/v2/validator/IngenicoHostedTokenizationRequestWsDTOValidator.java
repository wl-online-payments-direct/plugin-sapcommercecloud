package com.ingenico.ogone.direct.occ.controllers.v2.validator;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import com.ingenico.ogone.direct.payment.dto.HostedTokenizationRequestWsDTO;

@Component("ingenicoHostedTokenizationRequestWsDTOValidator")
public class IngenicoHostedTokenizationRequestWsDTOValidator implements Validator {


    @Override
    public boolean supports(final Class<?> aClass) {
        return HostedTokenizationRequestWsDTO.class.equals(aClass);
    }

    @Override
    public void validate(final Object object, final Errors errors) {
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "hostedTokenizationId", "hostedTokenization.hostedTokenizationId.missing");
        ValidationUtils.rejectIfEmpty(errors, "browserData", "hostedTokenization.browserData.missing");
    }
}
