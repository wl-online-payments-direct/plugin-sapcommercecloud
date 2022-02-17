package com.worldline.direct.acceleratorfacades.flow.impl;

import com.worldline.direct.enums.WorldlineCheckoutTypesEnum;
import de.hybris.platform.acceleratorfacades.flow.impl.DefaultCheckoutFlowFacade;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commerceservices.order.CommercePaymentProviderStrategy;

import org.apache.commons.lang.StringUtils;

public class WorldlineCheckoutFlowFacadeImpl extends DefaultCheckoutFlowFacade {
    private static final String WORLDLINE_PROVIDER = "WORLDLINE";
    private CommercePaymentProviderStrategy commercePaymentProviderStrategy;

    @Override
    public boolean hasNoPaymentInfo() {
        if (WORLDLINE_PROVIDER.equals(commercePaymentProviderStrategy.getPaymentProvider())) {
            final CartData cartData = getCheckoutCart();
            return cartData == null ||
                    cartData.getWorldlinePaymentInfo() == null ||
                    (WorldlineCheckoutTypesEnum.HOSTED_TOKENIZATION.equals(cartData.getWorldlinePaymentInfo().getWorldlineCheckoutType())
                            && StringUtils.isEmpty(cartData.getWorldlinePaymentInfo().getHostedTokenizationId()));
        }
        return super.hasNoPaymentInfo();
    }

    public void setCommercePaymentProviderStrategy(CommercePaymentProviderStrategy commercePaymentProviderStrategy) {
        this.commercePaymentProviderStrategy = commercePaymentProviderStrategy;
    }
}
