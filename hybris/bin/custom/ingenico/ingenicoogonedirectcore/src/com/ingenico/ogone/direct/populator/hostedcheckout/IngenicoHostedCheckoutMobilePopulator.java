package com.ingenico.ogone.direct.populator.hostedcheckout;

import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;

import com.ingenico.direct.domain.CreateHostedCheckoutRequest;

public class IngenicoHostedCheckoutMobilePopulator implements Populator<CartModel, CreateHostedCheckoutRequest> {

    @Override
    public void populate(CartModel cartModel, CreateHostedCheckoutRequest createHostedCheckoutRequest) throws ConversionException {
        //TODO for applePay
    }
}
