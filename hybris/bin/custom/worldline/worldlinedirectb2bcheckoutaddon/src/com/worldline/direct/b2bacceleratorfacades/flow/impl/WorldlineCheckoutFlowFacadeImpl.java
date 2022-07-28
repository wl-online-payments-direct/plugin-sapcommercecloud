package com.worldline.direct.b2bacceleratorfacades.flow.impl;

import com.worldline.direct.order.data.WorldlinePaymentInfoData;
import de.hybris.platform.b2bacceleratorfacades.order.impl.B2BMultiStepCheckoutFlowFacade;
import de.hybris.platform.b2bacceleratorservices.enums.CheckoutPaymentType;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commerceservices.order.CommercePaymentProviderStrategy;
import org.apache.commons.lang.StringUtils;

import java.util.function.Predicate;

import static com.worldline.direct.constants.WorldlinedirectcoreConstants.PAYMENT_METHOD_IDEAL;

public class WorldlineCheckoutFlowFacadeImpl extends B2BMultiStepCheckoutFlowFacade {
    private static final String WORLDLINE_PROVIDER = "WORLDLINE";
    private CommercePaymentProviderStrategy commercePaymentProviderStrategy;

    @Override
    public boolean hasNoPaymentInfo() {
        final CartData cartData = getCheckoutCart();
        if (WORLDLINE_PROVIDER.equals(commercePaymentProviderStrategy.getPaymentProvider()) && CheckoutPaymentType.CARD.getCode().equals(cartData.getPaymentType().getCode())) {
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
                    return (PAYMENT_METHOD_IDEAL == worldlinePaymentInfoData.getId() && StringUtils.isNotEmpty(worldlinePaymentInfoData.getPaymentProductDirectoryId()))
                            || StringUtils.isNotBlank(worldlinePaymentInfoData.getHostedTokenizationId());

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
