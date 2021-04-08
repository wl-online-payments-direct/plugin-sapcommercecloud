package com.ingenico.ogone.direct.acceleratorfacades.flow.impl;

import de.hybris.platform.acceleratorfacades.flow.impl.DefaultCheckoutFlowFacade;
import de.hybris.platform.commercefacades.order.data.CartData;

import com.ingenico.ogone.direct.acceleratorfacades.flow.IngenicoCheckoutFlowFacade;
import com.ingenico.ogone.direct.enums.IngenicoCheckoutTypesEnum;

public class IngenicoCheckoutFlowFacadeImpl extends DefaultCheckoutFlowFacade implements IngenicoCheckoutFlowFacade {

    @Override
    public boolean hasNoPaymentInfo() {
        final CartData cartData = getCheckoutCart();
        return cartData == null || cartData.getIngenicoPaymentInfo() == null;
    }


    @Override
    public boolean isNotCheckoutWithTokenization() {
        final CartData cartData = getCheckoutCart();
        return cartData == null ||
                cartData.getIngenicoPaymentInfo() == null ||
                !IngenicoCheckoutTypesEnum.HOSTED_TOKENIZATION.equals(cartData.getIngenicoPaymentInfo().getIngenicoCheckoutType());
    }
}
