package com.worldline.direct.populator.hostedtokenization;

import com.google.common.base.Preconditions;
import com.onlinepayments.domain.*;
import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import com.worldline.direct.enums.OperationCodesEnum;
import com.worldline.direct.factory.impl.WorldlinePaymentProduct130SpecificInputFactory;
import com.worldline.direct.factory.impl.WorldlineThreeDSecureFactory;
import com.worldline.direct.model.WorldlineConfigurationModel;
import com.worldline.direct.service.WorldlineConfigurationService;
import com.worldline.direct.service.WorldlinePaymentModeService;
import com.worldline.direct.service.WorldlinePaymentService;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.enumeration.EnumerationService;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import de.hybris.platform.servicelayer.session.SessionService;
import org.apache.commons.lang.StringUtils;

import static com.worldline.direct.populator.hostedtokenization.WorldlineHostedTokenizationBasicPopulator.HOSTED_TOKENIZATION_RETURN_URL;
import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

public class WorldlineHostedTokenizationCardPopulator implements Populator<AbstractOrderModel, CreatePaymentRequest> {

    private static final String ECOMMERCE = "ECOMMERCE";
    private static final String RECCURANCE = "recurring";
    private static final String RECCURANCE_SUBSEQUENT = "subsequent";
    private static final String RECCURANCE_FIRST = "first";
    private static final String CARDHOLDER_INITIAITED = "cardholderInitiated";
    private static final String MERCHANT_INITIAITED = "merchantInitiated";

    private SessionService sessionService;
    private WorldlineConfigurationService worldlineConfigurationService;
    private WorldlinePaymentService worldlinePaymentService;
    private WorldlinePaymentModeService worldlinePaymentModeService;
    private EnumerationService enumerationService;

    @Override
    public void populate(AbstractOrderModel abstractOrderModel, CreatePaymentRequest createPaymentRequest) throws ConversionException {
        validateParameterNotNull(abstractOrderModel, "abstractOrderModel cannot be null!");
        validateParameterNotNull(abstractOrderModel.getPaymentInfo(), "PaymentInfo cannot be null!");
        Preconditions.checkArgument(abstractOrderModel.getPaymentInfo() instanceof WorldlinePaymentInfoModel, "Payment has to be WorldlinePaymentInfo");

        final WorldlinePaymentInfoModel paymentInfo = (WorldlinePaymentInfoModel) abstractOrderModel.getPaymentInfo();

        if (WorldlinedirectcoreConstants.PAYMENT_METHOD_TYPE.CARD.getValue().equals(paymentInfo.getPaymentMethod())) {
            if (paymentInfo.getWorldlineRecurringToken() != null) {
                createPaymentRequest.setCardPaymentMethodSpecificInput(getCardPaymentMethodSpecificInput(Boolean.TRUE, paymentInfo, RECCURANCE, abstractOrderModel));
                createPaymentRequest.getCardPaymentMethodSpecificInput().setToken(paymentInfo.getWorldlineRecurringToken().getToken());

                createPaymentRequest.getCardPaymentMethodSpecificInput().setPaymentProductId(paymentInfo.getId());
            } else {
                final GetHostedTokenizationResponse hostedTokenization = worldlinePaymentService.getHostedTokenization(paymentInfo.getHostedTokenizationId());
                validateParameterNotNull(hostedTokenization, "tokenizationResponse cannot be null");
                validateParameterNotNull(hostedTokenization.getToken(), "Token cannot be null");
                createPaymentRequest.setCardPaymentMethodSpecificInput(getCardPaymentMethodSpecificInput(paymentInfo.isRecurringToken(), paymentInfo, paymentInfo.isRecurringToken() ? RECCURANCE_FIRST : StringUtils.EMPTY, abstractOrderModel, hostedTokenization));
                createPaymentRequest.getCardPaymentMethodSpecificInput()
                      .setToken(hostedTokenization.getToken().getId());
                createPaymentRequest.getCardPaymentMethodSpecificInput()
                      .setPaymentProductId(hostedTokenization.getToken().getPaymentProductId());
            }
        }


    }

    private CardPaymentMethodSpecificInput getCardPaymentMethodSpecificInput(Boolean isRecurring, WorldlinePaymentInfoModel paymentInfo, String recurrance, AbstractOrderModel abstractOrderModel, GetHostedTokenizationResponse hostedTokenization) {
        final WorldlineConfigurationModel currentWorldlineConfiguration = worldlineConfigurationService.getCurrentWorldlineConfiguration();
        CardPaymentMethodSpecificInput cardPaymentMethodSpecificInput = getCardPaymentMethodSpecificInput(isRecurring, paymentInfo, recurrance, abstractOrderModel);
        boolean isSale = false;
        if (worldlinePaymentModeService.isSaleOnly(String.valueOf(paymentInfo.getId()))) {
            cardPaymentMethodSpecificInput.setAuthorizationMode(OperationCodesEnum.SALE.getCode());
            isSale = true;
        } else if (currentWorldlineConfiguration.getDefaultOperationCode() != null) {
            cardPaymentMethodSpecificInput.setAuthorizationMode(currentWorldlineConfiguration.getDefaultOperationCode().getCode());
            isSale = OperationCodesEnum.SALE.equals(currentWorldlineConfiguration.getDefaultOperationCode());
        }

        if (WorldlinedirectcoreConstants.PAYMENT_METHOD_CARTES_BANCAIRES_FRICTIONLESS == hostedTokenization.getToken().getPaymentProductId()) {
            PaymentProduct130SpecificInput paymentProduct130SpecificInput = WorldlinePaymentProduct130SpecificInputFactory.getPaymentProduct130SpecificInput(currentWorldlineConfiguration, abstractOrderModel, paymentInfo, isSale);
            if (paymentProduct130SpecificInput != null) {
                cardPaymentMethodSpecificInput.setPaymentProduct130SpecificInput(paymentProduct130SpecificInput);
            }
        }
        return cardPaymentMethodSpecificInput;
    }

    private CardPaymentMethodSpecificInput getCardPaymentMethodSpecificInput(Boolean isRecurring, WorldlinePaymentInfoModel paymentInfo, String recurrance, AbstractOrderModel abstractOrderModel) {
        final WorldlineConfigurationModel currentWorldlineConfiguration = worldlineConfigurationService.getCurrentWorldlineConfiguration();
        CardPaymentMethodSpecificInput cardPaymentMethodSpecificInput = new CardPaymentMethodSpecificInput();
        cardPaymentMethodSpecificInput.setTokenize(false);
        cardPaymentMethodSpecificInput.setTransactionChannel(ECOMMERCE);
        cardPaymentMethodSpecificInput.setIsRecurring(isRecurring);


        if (StringUtils.equals(RECCURANCE, recurrance)) {
            CardRecurrenceDetails cardRecurrenceDetails = new CardRecurrenceDetails();
            cardRecurrenceDetails.setRecurringPaymentSequenceIndicator(recurrance);
            cardPaymentMethodSpecificInput.setRecurring(cardRecurrenceDetails);

        } else {
            ThreeDSecure threeDSecure = WorldlineThreeDSecureFactory.createThreeDSecure(currentWorldlineConfiguration, abstractOrderModel, enumerationService);
            if(threeDSecure != null) {
                cardPaymentMethodSpecificInput.setThreeDSecure(threeDSecure);
                threeDSecure.setRedirectionData(new RedirectionData());
                threeDSecure.getRedirectionData().setReturnUrl(getHostedTokenizationReturnUrl());
            }

            if (isRecurring) {
                CardRecurrenceDetails cardRecurrenceDetails = new CardRecurrenceDetails();
                cardRecurrenceDetails.setRecurringPaymentSequenceIndicator(recurrance);
                cardPaymentMethodSpecificInput.setRecurring(cardRecurrenceDetails);
            }
        }

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

    public void setWorldlinePaymentModeService(WorldlinePaymentModeService worldlinePaymentModeService) {
        this.worldlinePaymentModeService = worldlinePaymentModeService;
    }

    public void setEnumerationService(EnumerationService enumerationService) {
        this.enumerationService = enumerationService;
    }
}
