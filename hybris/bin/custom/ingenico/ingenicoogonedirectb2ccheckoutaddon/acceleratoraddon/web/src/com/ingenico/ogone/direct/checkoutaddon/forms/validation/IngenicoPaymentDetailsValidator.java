
package com.ingenico.ogone.direct.checkoutaddon.forms.validation;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import com.ingenico.ogone.direct.checkoutaddon.forms.IngenicoPaymentDetailsForm;
import com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants;


@Component("ingenicoPaymentDetailsValidator")
public class IngenicoPaymentDetailsValidator implements Validator {
    @Override
    public boolean supports(final Class<?> aClass) {
        return IngenicoPaymentDetailsForm.class.equals(aClass);
    }

    @Override
    public void validate(final Object object, final Errors errors) {
        final IngenicoPaymentDetailsForm form = (IngenicoPaymentDetailsForm) object;

        if (Boolean.FALSE.equals(form.isUseDeliveryAddress())) {
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "billingAddress.firstName", "address.firstName.invalid");
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "billingAddress.lastName", "address.lastName.invalid");
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "billingAddress.line1", "address.line1.invalid");
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "billingAddress.townCity", "address.townCity.invalid");
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "billingAddress.postcode", "address.postcode.invalid");
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "billingAddress.countryIso", "address.country.invalid");
        }

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "paymentProductId", "checkout.error.paymentProduct.id.missing");
        if (form.getPaymentProductId() != null) {
            if (IngenicoogonedirectcoreConstants.PAYMENT_METHOD_IDEAL == form.getPaymentProductId()) {
                ValidationUtils.rejectIfEmptyOrWhitespace(errors, "issuerId", "checkout.error.issuer.id.missing");
            } else if (IngenicoogonedirectcoreConstants.PAYMENT_METHOD_HTP == form.getPaymentProductId()) {
                ValidationUtils.rejectIfEmptyOrWhitespace(errors, "hostedTokenizationId", "checkout.error.hostedTokenization.hostedTokenizationId.missing");
            }
        }

    }
}
