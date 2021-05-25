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
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "billingAddress.firstName", "field.required", new Object[]{"billingAddress.firstName"});
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "billingAddress.lastName", "field.required", new Object[]{"billingAddress.lastName"});
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "billingAddress.line1", "field.required", new Object[]{"billingAddress.line1"});
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "billingAddress.town", "field.required", new Object[]{"billingAddress.town"});
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "billingAddress.postalCode", "field.required", new Object[]{"billingAddress.postalCode"});
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "billingAddress.country.isocode", "field.required", new Object[]{"billingAddress.country.isocode"});
        }

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "paymentProductId", "field.required", new Object[]{"paymentProductId"});
        if (wsDTO.getPaymentProductId() != null && IngenicoogonedirectcoreConstants.PAYMENT_METHOD_IDEAL == wsDTO.getPaymentProductId()) {
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "issuerId", "field.required", new Object[]{"issuerId"});
        }

    }
}
