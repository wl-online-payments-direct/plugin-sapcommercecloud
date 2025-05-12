package com.worldline.direct.populator.hostedtokenization;

import com.google.common.base.Preconditions;
import com.onlinepayments.domain.*;
import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import com.worldline.direct.enums.OperationCodesEnum;
import com.worldline.direct.model.WorldlineConfigurationModel;
import com.worldline.direct.service.WorldlineConfigurationService;
import com.worldline.direct.service.WorldlinePaymentService;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import de.hybris.platform.servicelayer.session.SessionService;
import org.apache.commons.lang.BooleanUtils;
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
    public static final String CHALLENGE_REQUIRED = "challenge-required";
    public static final String LOW_VALUE = "low-value";
    private SessionService sessionService;
    private WorldlineConfigurationService worldlineConfigurationService;
    private WorldlinePaymentService worldlinePaymentService;

    public static final String CARTES_BANCAIRES_SINGLE_SALE = "single-amount";
    public static final String CARTES_BANCAIRES_SINGLE_AUTH = "payment-upon-shipment";
    public static final String CARTES_BANCAIRES_RECURRING = "other-recurring-payments";

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
        if (currentWorldlineConfiguration.getDefaultOperationCode() != null) {
            cardPaymentMethodSpecificInput.setAuthorizationMode(currentWorldlineConfiguration.getDefaultOperationCode().getCode());
            isSale = OperationCodesEnum.SALE.equals(currentWorldlineConfiguration.getDefaultOperationCode());
        }

        if (WorldlinedirectcoreConstants.PAYMENT_METHOD_CARTES_BANCAIRES_FRICTIONLESS == hostedTokenization.getToken().getPaymentProductId()) {
            PaymentProduct130SpecificInput paymentProduct130SpecificInput = getPaymentProduct130SpecificInput(abstractOrderModel, paymentInfo, isSale);
            cardPaymentMethodSpecificInput.setPaymentProduct130SpecificInput(paymentProduct130SpecificInput);
        }
        return cardPaymentMethodSpecificInput;
    }

    private CardPaymentMethodSpecificInput getCardPaymentMethodSpecificInput(Boolean isRecurring, WorldlinePaymentInfoModel paymentInfo, String recurrance, AbstractOrderModel abstractOrderModel) {
        final WorldlineConfigurationModel currentWorldlineConfiguration = worldlineConfigurationService.getCurrentWorldlineConfiguration();
        CardPaymentMethodSpecificInput cardPaymentMethodSpecificInput = new CardPaymentMethodSpecificInput();
        cardPaymentMethodSpecificInput.setTokenize(false);
        cardPaymentMethodSpecificInput.setSkipAuthentication(false);
        cardPaymentMethodSpecificInput.setTransactionChannel(ECOMMERCE);
        cardPaymentMethodSpecificInput.setIsRecurring(isRecurring);


        if (StringUtils.equals(RECCURANCE, recurrance)) {
            CardRecurrenceDetails cardRecurrenceDetails = new CardRecurrenceDetails();
            cardRecurrenceDetails.setRecurringPaymentSequenceIndicator(recurrance);
            cardPaymentMethodSpecificInput.setRecurring(cardRecurrenceDetails);

        } else {
            cardPaymentMethodSpecificInput.setThreeDSecure(new ThreeDSecure());
            cardPaymentMethodSpecificInput.getThreeDSecure().setRedirectionData(new RedirectionData());
            cardPaymentMethodSpecificInput.getThreeDSecure().getRedirectionData().setReturnUrl(getHostedTokenizationReturnUrl());

//            if (BooleanUtils.isTrue(currentWorldlineConfiguration.isChallengeRequired())) {
//                cardPaymentMethodSpecificInput.getThreeDSecure().setChallengeIndicator(CHALLENGE_REQUIRED);
//            }
            boolean isExemptionRequestLowValue = BooleanUtils.isTrue(currentWorldlineConfiguration.isExemptionRequest()) && abstractOrderModel.getCurrency().getIsocode().equals("EUR") && abstractOrderModel.getTotalPrice() < 30;
            boolean isChallengeRequired = BooleanUtils.isTrue(currentWorldlineConfiguration.isChallengeRequired());

            if (isChallengeRequired) {
                cardPaymentMethodSpecificInput.getThreeDSecure().setChallengeIndicator(CHALLENGE_REQUIRED);
            } else if (isExemptionRequestLowValue) {
                cardPaymentMethodSpecificInput.getThreeDSecure().setExemptionRequest(LOW_VALUE);
            }

            if (isRecurring) {
                CardRecurrenceDetails cardRecurrenceDetails = new CardRecurrenceDetails();
                cardRecurrenceDetails.setRecurringPaymentSequenceIndicator(recurrance);
                cardPaymentMethodSpecificInput.setRecurring(cardRecurrenceDetails);
            }
        }

        return cardPaymentMethodSpecificInput;
    }

    /**
     * Generates a PaymentProduct130SpecificInput for Cartes Bancaires. Provides numberOfItems which will represent the
     * number of items in the AbstractOrder but is capped to 99, and populates the 3DSecure use case.
     * @param abstractOrderModel Used to find the total items being ordered.
     * @param paymentInfo Used to find whether this is a recurring payment.
     * @param isSale Used to populate 3DSecure use case if not a recurring payment.
     *
     * @return A PaymentProduct130SpecificInput for the provided AbstractOrderModel based on whether the order is recurring
     * and whether the current payment mode is SALE or AUTH.
     */
    private static PaymentProduct130SpecificInput getPaymentProduct130SpecificInput(AbstractOrderModel abstractOrderModel, WorldlinePaymentInfoModel paymentInfo, boolean isSale) {
        PaymentProduct130SpecificInput paymentProduct130SpecificInput = new PaymentProduct130SpecificInput();
        PaymentProduct130SpecificThreeDSecure threeDSecure = new PaymentProduct130SpecificThreeDSecure();
        int numberOfItems = 0;
        for (AbstractOrderEntryModel entry : abstractOrderModel.getEntries()) {
            numberOfItems += entry.getQuantity();
        }
        threeDSecure.setNumberOfItems(Math.min(numberOfItems, 99));

        if (paymentInfo.isRecurringToken()) {
            threeDSecure.setUsecase(CARTES_BANCAIRES_RECURRING);
        } else if (isSale) {
            threeDSecure.setUsecase(CARTES_BANCAIRES_SINGLE_SALE);
        } else {
            threeDSecure.setUsecase(CARTES_BANCAIRES_SINGLE_AUTH);
        }
        paymentProduct130SpecificInput.setThreeDSecure(threeDSecure);

        return paymentProduct130SpecificInput;
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
