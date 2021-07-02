package com.ingenico.ogone.direct.populator.hostedcheckout;

import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;

import com.ingenico.direct.domain.CreateHostedCheckoutRequest;

public class IngenicoHostedCheckoutMobilePopulator implements Populator<AbstractOrderModel, CreateHostedCheckoutRequest> {

    @Override
    public void populate(AbstractOrderModel abstractOrderModel, CreateHostedCheckoutRequest createHostedCheckoutRequest) throws ConversionException {

    }
}
