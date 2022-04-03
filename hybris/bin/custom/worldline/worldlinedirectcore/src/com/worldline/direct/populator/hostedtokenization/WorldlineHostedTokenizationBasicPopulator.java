package com.worldline.direct.populator.hostedtokenization;

import com.ingenico.direct.domain.CreatePaymentRequest;
import com.ingenico.direct.domain.Order;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import de.hybris.platform.servicelayer.dto.converter.Converter;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

public class WorldlineHostedTokenizationBasicPopulator implements Populator<AbstractOrderModel, CreatePaymentRequest> {

    public static final String HOSTED_TOKENIZATION_RETURN_URL = "hostedTokenizationReturnUrl";
    private Converter<AbstractOrderModel, Order> worldlineOrderParamConverter;

    @Override
    public void populate(AbstractOrderModel abstractOrderModel, CreatePaymentRequest createPaymentRequest) throws ConversionException {
        validateParameterNotNull(abstractOrderModel, "order cannot be null!");
        createPaymentRequest.setOrder(worldlineOrderParamConverter.convert(abstractOrderModel));
    }

    public void setWorldlineOrderParamConverter(Converter<AbstractOrderModel, Order> worldlineOrderParamConverter) {
        this.worldlineOrderParamConverter = worldlineOrderParamConverter;
    }

}
