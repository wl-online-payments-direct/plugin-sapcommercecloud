package com.ingenico.ogone.direct.populator.hostedtokenization;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.servicelayer.session.SessionService;

import com.ingenico.direct.domain.CardPaymentMethodSpecificInput;
import com.ingenico.direct.domain.CreatePaymentRequest;
import com.ingenico.direct.domain.Order;
import com.ingenico.direct.domain.RedirectionData;
import com.ingenico.direct.domain.ThreeDSecure;
import com.ingenico.ogone.direct.model.IngenicoConfigurationModel;
import com.ingenico.ogone.direct.service.IngenicoConfigurationService;

public class IngenicoHostedTokenizationPaymentPopulator implements Populator<AbstractOrderModel, CreatePaymentRequest> {

    private static final String ECOMMERCE = "ECOMMERCE";
    private SessionService sessionService;
    private IngenicoConfigurationService ingenicoConfigurationService;

    private Converter<AbstractOrderModel, Order> ingenicoOrderParamConverter;

    @Override
    public void populate(AbstractOrderModel abstractOrderModel, CreatePaymentRequest createPaymentRequest) throws ConversionException {
        validateParameterNotNull(abstractOrderModel, "order cannot be null!");
        createPaymentRequest.setOrder(ingenicoOrderParamConverter.convert(abstractOrderModel));
        createPaymentRequest.setCardPaymentMethodSpecificInput(getCardPaymentMethodSpecificInput());

    }

    private CardPaymentMethodSpecificInput getCardPaymentMethodSpecificInput() {
        final IngenicoConfigurationModel currentIngenicoConfiguration = ingenicoConfigurationService.getCurrentIngenicoConfiguration();

        CardPaymentMethodSpecificInput cardPaymentMethodSpecificInput = new CardPaymentMethodSpecificInput();
        if (currentIngenicoConfiguration.getDefaultOperationCode() != null) {
            cardPaymentMethodSpecificInput.setAuthorizationMode(currentIngenicoConfiguration.getDefaultOperationCode().getCode());
        }
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

    public void setIngenicoOrderParamConverter(Converter<AbstractOrderModel, Order> ingenicoOrderParamConverter) {
        this.ingenicoOrderParamConverter = ingenicoOrderParamConverter;
    }


    public void setIngenicoConfigurationService(IngenicoConfigurationService ingenicoConfigurationService) {
        this.ingenicoConfigurationService = ingenicoConfigurationService;
    }
}
