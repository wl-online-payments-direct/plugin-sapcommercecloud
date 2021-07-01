package com.ingenico.ogone.direct.acceleratorfacades.flow.impl;

import de.hybris.platform.acceleratorfacades.flow.impl.DefaultCheckoutFlowFacade;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commerceservices.order.CommercePaymentProviderStrategy;

import org.apache.commons.lang.StringUtils;

import com.ingenico.ogone.direct.enums.IngenicoCheckoutTypesEnum;

public class IngenicoCheckoutFlowFacadeImpl extends DefaultCheckoutFlowFacade {
    private static final String INGENICO_PROVIDER = "INGENICO";
    private CommercePaymentProviderStrategy commercePaymentProviderStrategy;

    @Override
    public boolean hasNoPaymentInfo() {
        if (INGENICO_PROVIDER.equals(commercePaymentProviderStrategy.getPaymentProvider())) {
            final CartData cartData = getCheckoutCart();
            return cartData == null ||
                    cartData.getIngenicoPaymentInfo() == null ||
                    (IngenicoCheckoutTypesEnum.HOSTED_TOKENIZATION.equals(cartData.getIngenicoPaymentInfo().getIngenicoCheckoutType())
                            && StringUtils.isEmpty(cartData.getIngenicoPaymentInfo().getHostedTokenizationId()));
        }
        return super.hasNoPaymentInfo();
    }

    public void setCommercePaymentProviderStrategy(CommercePaymentProviderStrategy commercePaymentProviderStrategy) {
        this.commercePaymentProviderStrategy = commercePaymentProviderStrategy;
    }
}
