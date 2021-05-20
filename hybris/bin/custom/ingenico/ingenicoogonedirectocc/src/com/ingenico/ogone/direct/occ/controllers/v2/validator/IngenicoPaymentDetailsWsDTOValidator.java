package com.ingenico.ogone.direct.occ.controllers.v2.validator;


import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants;
import com.ingenico.ogone.direct.payment.dto.IngenicoPaymentDetailsWsDTO;

@Component("ingenicoPaymentDetailsWsDTOValidator")
public class IngenicoPaymentDetailsWsDTOValidator implements Validator {
    @Override
    public boolean supports(final Class<?> aClass) {
        return IngenicoPaymentDetailsWsDTO.class.equals(aClass);
    }

    @Override
    public void validate(final Object object, final Errors errors) {
        final IngenicoPaymentDetailsWsDTO wsDTO = (IngenicoPaymentDetailsWsDTO) object;

        if (Boolean.FALSE.equals(wsDTO.isUseDeliveryAddress())) {
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "billingAddress.firstName", "address.firstName.invalid");
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "billingAddress.lastName", "address.lastName.invalid");
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "billingAddress.line1", "address.line1.invalid");
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "billingAddress.town", "address.townCity.invalid");
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "billingAddress.postalCode", "address.postcode.invalid");
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "billingAddress.country.isocode", "address.country.invalid");
        }

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "paymentProductId", "checkout.error.paymentProduct.id.missing");
        if (wsDTO.getPaymentProductId() != null && IngenicoogonedirectcoreConstants.PAYMENT_METHOD_IDEAL == wsDTO.getPaymentProductId()) {
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "issuerId", "checkout.error.issuer.id.missing");
        }

    }
}
