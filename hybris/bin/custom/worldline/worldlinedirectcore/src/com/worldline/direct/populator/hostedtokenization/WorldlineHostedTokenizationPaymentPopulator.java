package com.worldline.direct.populator.hostedtokenization;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

import com.worldline.direct.service.WorldlineConfigurationService;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.servicelayer.session.SessionService;

import com.ingenico.direct.domain.CardPaymentMethodSpecificInput;
import com.ingenico.direct.domain.CreatePaymentRequest;
import com.ingenico.direct.domain.Order;
import com.ingenico.direct.domain.RedirectionData;
import com.ingenico.direct.domain.ThreeDSecure;
import com.worldline.direct.model.WorldlineConfigurationModel;

public class WorldlineHostedTokenizationPaymentPopulator implements Populator<AbstractOrderModel, CreatePaymentRequest> {

    private static final String ECOMMERCE = "ECOMMERCE";
    private SessionService sessionService;
    private WorldlineConfigurationService worldlineConfigurationService;

    private Converter<AbstractOrderModel, Order> worldlineOrderParamConverter;

    @Override
    public void populate(AbstractOrderModel abstractOrderModel, CreatePaymentRequest createPaymentRequest) throws ConversionException {
        validateParameterNotNull(abstractOrderModel, "order cannot be null!");
        createPaymentRequest.setOrder(worldlineOrderParamConverter.convert(abstractOrderModel));
        createPaymentRequest.setCardPaymentMethodSpecificInput(getCardPaymentMethodSpecificInput());

    }

    private CardPaymentMethodSpecificInput getCardPaymentMethodSpecificInput() {
        final WorldlineConfigurationModel currentWorldlineConfiguration = worldlineConfigurationService.getCurrentWorldlineConfiguration();

        CardPaymentMethodSpecificInput cardPaymentMethodSpecificInput = new CardPaymentMethodSpecificInput();
        if (currentWorldlineConfiguration.getDefaultOperationCode() != null) {
            cardPaymentMethodSpecificInput.setAuthorizationMode(currentWorldlineConfiguration.getDefaultOperationCode().getCode());
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

    public void setWorldlineOrderParamConverter(Converter<AbstractOrderModel, Order> worldlineOrderParamConverter) {
        this.worldlineOrderParamConverter = worldlineOrderParamConverter;
    }

    public void setWorldlineConfigurationService(WorldlineConfigurationService worldlineConfigurationService) {
        this.worldlineConfigurationService = worldlineConfigurationService;
    }
}
