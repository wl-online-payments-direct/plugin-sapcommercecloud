package com.worldline.direct.occ.controllers.v2.validator;


import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import com.worldline.direct.payment.dto.WorldlinePaymentDetailsWsDTO;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

@Component("worldlinePaymentDetailsWsDTOValidator")
public class WorldlinePaymentDetailsWsDTOValidator implements Validator {
    @Override
    public boolean supports(final Class<?> aClass) {
        return WorldlinePaymentDetailsWsDTO.class.equals(aClass);
    }

    @Override
    public void validate(final Object object, final Errors errors) {
        final WorldlinePaymentDetailsWsDTO wsDTO = (WorldlinePaymentDetailsWsDTO) object;

        if (Boolean.FALSE.equals(wsDTO.isUseDeliveryAddress())) {
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "billingAddress.firstName", "field.required", new Object[]{"billingAddress.firstName"});
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "billingAddress.lastName", "field.required", new Object[]{"billingAddress.lastName"});
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "billingAddress.line1", "field.required", new Object[]{"billingAddress.line1"});
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "billingAddress.town", "field.required", new Object[]{"billingAddress.town"});
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "billingAddress.postalCode", "field.required", new Object[]{"billingAddress.postalCode"});
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "billingAddress.country.isocode", "field.required", new Object[]{"billingAddress.country.isocode"});
        }

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "paymentProductId", "field.required", new Object[]{"paymentProductId"});
        if (wsDTO.getPaymentProductId() != null) {
            if (WorldlinedirectcoreConstants.PAYMENT_METHOD_IDEAL == wsDTO.getPaymentProductId()) {
                ValidationUtils.rejectIfEmptyOrWhitespace(errors, "issuerId", "field.required", new Object[]{"issuerId"});
            } else if (WorldlinedirectcoreConstants.PAYMENT_METHOD_HTP == wsDTO.getPaymentProductId()) {
                ValidationUtils.rejectIfEmptyOrWhitespace(errors, "hostedTokenizationId", "field.required", new Object[]{"hostedTokenizationId"});
            } else if (WorldlinedirectcoreConstants.PAYMENT_METHOD_HCP == wsDTO.getPaymentProductId()) {
                ValidationUtils.rejectIfEmptyOrWhitespace(errors, "hostedCheckoutId", "field.required.missing", new Object[]{"hostedCheckoutId"});
            }
        }

    }
}
