package com.worldline.gopay.attributehandlers;

import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.servicelayer.model.attribute.DynamicAttributeHandler;
import de.hybris.platform.util.localization.Localization;
import org.apache.commons.lang.StringUtils;

/**
 * Read-only display value for a 3DS result stored on the payment info. Worldline's raw values are mapped to a
 * friendly, localised label; unknown values are shown as received so nothing is hidden.
 */
public abstract class AbstractWorldlineThreeDSecureDisplayAttributeHandler implements DynamicAttributeHandler<String, WorldlinePaymentInfoModel> {

    @Override
    public String get(final WorldlinePaymentInfoModel paymentInfo) {
        final String rawValue = getRawValue(paymentInfo);
        if (StringUtils.isBlank(rawValue)) {
            return localize(getEmptyKey(), getEmptyDefault());
        }
        final String key = getKeyForValue(rawValue.trim().toLowerCase());
        return key == null ? rawValue : localize(key, getDefaultForKey(key));
    }

    @Override
    public void set(final WorldlinePaymentInfoModel paymentInfo, final String value) {
        throw new UnsupportedOperationException("This attribute is read-only");
    }

    protected abstract String getRawValue(WorldlinePaymentInfoModel paymentInfo);

    /**
     * @return the localisation key for a known Worldline value, or null if the value is not known.
     */
    protected abstract String getKeyForValue(String rawValue);

    protected abstract String getDefaultForKey(String key);

    protected abstract String getEmptyKey();

    protected abstract String getEmptyDefault();

    /*
     * Not every locale file has these keys yet, so fall back to the English text rather than showing the key.
     */
    protected String localize(final String key, final String defaultValue) {
        final String localized = Localization.getLocalizedString(key);
        return StringUtils.isBlank(localized) || localized.contains(key) ? defaultValue : localized;
    }
}
