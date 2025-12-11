package com.worldline.direct.constraints;

import com.worldline.direct.enums.WorldlineExemptionType;
import com.worldline.direct.model.WorldlineConfigurationModel;
import de.hybris.platform.util.localization.Localization;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.math.BigDecimal;

public class WorldlineConfigurationValidator implements ConstraintValidator<WorldlineConfigurationValid, WorldlineConfigurationModel>
{
    private static final BigDecimal LOW_VALUE_LIMIT = BigDecimal.valueOf(30);
    private static final BigDecimal TRANSACTION_RISK_ANALYSIS_LIMIT = BigDecimal.valueOf(100);

    private static final String LOW_VALUE_ERROR_KEY = "com.worldline.direct.error.3ds.lowvalue.message";
    private static final String TRANSACTION_RISK_ANALYSIS_LIMIT_KEY = "com.worldline.direct.error.3ds.transactionriskanalysis.message";

    @Override
    public void initialize(final WorldlineConfigurationValid constraintAnnotation)
    {
        // Initialization if needed
    }

    @Override
    public boolean isValid(final WorldlineConfigurationModel model, final ConstraintValidatorContext context)
    {
        if (model == null)
        {
            return true;
        }

        BigDecimal exemptionLimit = model.getExemptionLimit3DS();
        WorldlineExemptionType exemptionType = model.getExemptionType3DS();

        /*
         * The manual localisation of Strings here is a bit weird. For some reason if I don't do it this way, the
         * Hybris service fails to do the fallback from en_GB to en (and I assume others) correctly. Please ensure
         * fallback works if you try to 'fix' this.
         */
        String localizedMsg = null;
        switch(exemptionType) {
            case LOW_VALUE:
                if (exemptionLimit.compareTo(LOW_VALUE_LIMIT) > 0) {
                    localizedMsg  = Localization.getLocalizedString(LOW_VALUE_ERROR_KEY);
                }
            case TRANSACTION_RISK_ANALYSIS:
                if (exemptionLimit.compareTo(TRANSACTION_RISK_ANALYSIS_LIMIT) > 0) {
                    localizedMsg = Localization.getLocalizedString(TRANSACTION_RISK_ANALYSIS_LIMIT_KEY);
                }
        }

        if(localizedMsg != null) {
            context.disableDefaultConstraintViolation();

            context.buildConstraintViolationWithTemplate(localizedMsg)
                    .addPropertyNode("exemptionLimit3DS") // Highlight the 'limit' field in Backoffice
                    .addConstraintViolation();

            return false;
        }
        return true;
    }
}