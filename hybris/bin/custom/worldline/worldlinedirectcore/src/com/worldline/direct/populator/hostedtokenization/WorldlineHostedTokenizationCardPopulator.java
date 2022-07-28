package com.worldline.direct.populator.hostedtokenization;

import com.google.common.base.Preconditions;
import com.onlinepayments.domain.*;
import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import com.worldline.direct.model.WorldlineConfigurationModel;
import com.worldline.direct.service.WorldlineConfigurationService;
import com.worldline.direct.service.WorldlinePaymentService;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import de.hybris.platform.servicelayer.session.SessionService;

import static com.worldline.direct.populator.hostedtokenization.WorldlineHostedTokenizationBasicPopulator.HOSTED_TOKENIZATION_RETURN_URL;
import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

public class WorldlineHostedTokenizationCardPopulator implements Populator<AbstractOrderModel, CreatePaymentRequest> {

    private static final String ECOMMERCE = "ECOMMERCE";
    private SessionService sessionService;
    private WorldlineConfigurationService worldlineConfigurationService;
    private WorldlinePaymentService worldlinePaymentService;

    @Override
    public void populate(AbstractOrderModel abstractOrderModel, CreatePaymentRequest createPaymentRequest) throws ConversionException {
        validateParameterNotNull(abstractOrderModel, "abstractOrderModel cannot be null!");
        validateParameterNotNull(abstractOrderModel.getPaymentInfo(), "PaymentInfo cannot be null!");
        Preconditions.checkArgument(abstractOrderModel.getPaymentInfo() instanceof WorldlinePaymentInfoModel, "Payment has to be WorldlinePaymentInfo");

        final WorldlinePaymentInfoModel paymentInfo = (WorldlinePaymentInfoModel) abstractOrderModel.getPaymentInfo();

        if (WorldlinedirectcoreConstants.PAYMENT_METHOD_TYPE.CARD.getValue().equals(paymentInfo.getPaymentMethod())) {
            final GetHostedTokenizationResponse hostedTokenization = worldlinePaymentService.getHostedTokenization(paymentInfo.getHostedTokenizationId());
            validateParameterNotNull(hostedTokenization, "tokenizationResponse cannot be null");

            createPaymentRequest.setCardPaymentMethodSpecificInput(getCardPaymentMethodSpecificInput());
            createPaymentRequest.getCardPaymentMethodSpecificInput()
                    .setToken(hostedTokenization.getToken().getId());
            createPaymentRequest.getCardPaymentMethodSpecificInput()
                    .setPaymentProductId(hostedTokenization.getToken().getPaymentProductId());

        }

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
        return sessionService.getAttribute(HOSTED_TOKENIZATION_RETURN_URL);
    }

    public void setSessionService(SessionService sessionService) {
        this.sessionService = sessionService;
    }


    public void setWorldlineConfigurationService(WorldlineConfigurationService worldlineConfigurationService) {
        this.worldlineConfigurationService = worldlineConfigurationService;
    }

    public void setWorldlinePaymentService(WorldlinePaymentService worldlinePaymentService) {
        this.worldlinePaymentService = worldlinePaymentService;
    }
}
