package com.worldline.gopay.editors;

import com.hybris.cockpitng.editor.defaultenum.EnumValueResolver;
import com.worldline.gopay.enums.WorldlineCheckoutTypesEnum;
import org.springframework.beans.factory.annotation.Required;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Limits the base store checkout type editor to the checkout types a store can be configured with.
 * GOOGLE_PAY and PAY_BY_LINK stay in the enum because they are set per payment on WorldlinePaymentInfo.
 */
public class WorldlineStoreCheckoutTypeValueResolver implements EnumValueResolver {
    private static final Set<WorldlineCheckoutTypesEnum> STORE_CHECKOUT_TYPES =
            Set.of(WorldlineCheckoutTypesEnum.HOSTED_CHECKOUT, WorldlineCheckoutTypesEnum.HOSTED_TOKENIZATION);

    private EnumValueResolver enumValueResolver;

    @Override
    public List<Object> getAllValues(String valueType, Object initialValue) {
        return enumValueResolver.getAllValues(valueType, initialValue).stream()
                .filter(value -> !(value instanceof WorldlineCheckoutTypesEnum) || STORE_CHECKOUT_TYPES.contains(value))
                .collect(Collectors.toList());
    }

    @Required
    public void setEnumValueResolver(EnumValueResolver enumValueResolver) {
        this.enumValueResolver = enumValueResolver;
    }
}
