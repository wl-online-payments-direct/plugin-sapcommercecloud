package com.worldline.direct.populator.hostedtokenization;

import com.onlinepayments.domain.CreatePaymentRequest;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;

public class WorldlineHostedTokenizationMobilePopulator implements Populator<AbstractOrderModel, CreatePaymentRequest> {

    @Override
    public void populate(AbstractOrderModel abstractOrderModel, CreatePaymentRequest createPaymentRequest) throws ConversionException {
    }

}
