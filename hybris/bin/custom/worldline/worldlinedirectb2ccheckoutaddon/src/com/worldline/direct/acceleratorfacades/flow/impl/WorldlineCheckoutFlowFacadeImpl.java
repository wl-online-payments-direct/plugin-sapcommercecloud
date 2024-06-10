package com.worldline.direct.acceleratorfacades.flow.impl;

import com.worldline.direct.order.data.WorldlinePaymentInfoData;
import de.hybris.platform.acceleratorfacades.flow.impl.DefaultCheckoutFlowFacade;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commerceservices.order.CommercePaymentProviderStrategy;
import org.apache.commons.lang.StringUtils;

import java.util.function.Predicate;

public class WorldlineCheckoutFlowFacadeImpl extends DefaultCheckoutFlowFacade {
    private static final String WORLDLINE_PROVIDER = "WORLDLINE";
    private CommercePaymentProviderStrategy commercePaymentProviderStrategy;

    @Override
    public boolean hasNoPaymentInfo() {
        if (WORLDLINE_PROVIDER.equals(commercePaymentProviderStrategy.getPaymentProvider())) {
            final CartData cartData = getCheckoutCart();
            return cartData == null ||
                    cartData.getWorldlinePaymentInfo() == null ||
                    validatePaymentInfo().negate().test(cartData.getWorldlinePaymentInfo());
        }
        return super.hasNoPaymentInfo();
    }

    private Predicate<WorldlinePaymentInfoData> validatePaymentInfo() {
        return worldlinePaymentInfoData -> {
            switch (worldlinePaymentInfoData.getWorldlineCheckoutType()) {
                case HOSTED_TOKENIZATION:
                    return StringUtils.isNotBlank(worldlinePaymentInfoData.getHostedTokenizationId());
                case HOSTED_CHECKOUT:
                default:
                    return true;
            }
        };
    }

    public void setCommercePaymentProviderStrategy(CommercePaymentProviderStrategy commercePaymentProviderStrategy) {
        this.commercePaymentProviderStrategy = commercePaymentProviderStrategy;
    }
}
