package com.ingenico.ogone.direct.populator.hostedtokenization;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.servicelayer.session.SessionService;

import com.ingenico.direct.domain.CardPaymentMethodSpecificInput;
import com.ingenico.direct.domain.CreatePaymentRequest;
import com.ingenico.direct.domain.Order;
import com.ingenico.direct.domain.RedirectionData;
import com.ingenico.direct.domain.ThreeDSecure;

public class IngenicoHostedTokenizationPaymentPopulator implements Populator<CartModel, CreatePaymentRequest> {

    private static final String ECOMMERCE = "ECOMMERCE";
    private SessionService sessionService;

    private Converter<CartModel, Order> ingenicoOrderParamConverter;

    @Override
    public void populate(CartModel cartModel, CreatePaymentRequest createPaymentRequest) throws ConversionException {
        validateParameterNotNull(cartModel, "cart cannot be null!");
        createPaymentRequest.setOrder(ingenicoOrderParamConverter.convert(cartModel));
        createPaymentRequest.setCardPaymentMethodSpecificInput(getCardPaymentMethodSpecificInput());

    }

    private CardPaymentMethodSpecificInput getCardPaymentMethodSpecificInput() {
        CardPaymentMethodSpecificInput cardPaymentMethodSpecificInput = new CardPaymentMethodSpecificInput();
        cardPaymentMethodSpecificInput.setTokenize(false);
        cardPaymentMethodSpecificInput.setIsRecurring(false);
        cardPaymentMethodSpecificInput.setSkipAuthentication(false);
        cardPaymentMethodSpecificInput.setTransactionChannel(ECOMMERCE);
        cardPaymentMethodSpecificInput.setThreeDSecure(new ThreeDSecure());
        cardPaymentMethodSpecificInput.getThreeDSecure().setRedirectionData(new RedirectionData());
        cardPaymentMethodSpecificInput.getThreeDSecure().getRedirectionData().setReturnUrl(getHostedTokenizationReturnUrl());

        return cardPaymentMethodSpecificInput;
    }

    private String getHostedTokenizationReturnUrl() {
        return sessionService.getAttribute("hostedTokenizationReturnUrl");
    }

    public void setSessionService(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    public void setIngenicoOrderParamConverter(Converter<CartModel, Order> ingenicoOrderParamConverter) {
        this.ingenicoOrderParamConverter = ingenicoOrderParamConverter;
    }


}
