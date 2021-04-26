
package com.ingenico.ogone.direct.checkoutaddon.forms.validation;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import com.ingenico.ogone.direct.checkoutaddon.forms.IngenicoHostedTokenizationForm;


@Component("ingenicoHostedTokenizationValidator")
public class IngenicoHostedTokenizationValidator implements Validator {
    @Override
    public boolean supports(final Class<?> aClass) {
        return IngenicoHostedTokenizationForm.class.equals(aClass);
    }

    @Override
    public void validate(final Object object, final Errors errors) {
        final IngenicoHostedTokenizationForm form = (IngenicoHostedTokenizationForm) object;
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "hostedTokenizationId", "checkout.error.hostedTokenization.hostedTokenizationId.missing");
    }
}
