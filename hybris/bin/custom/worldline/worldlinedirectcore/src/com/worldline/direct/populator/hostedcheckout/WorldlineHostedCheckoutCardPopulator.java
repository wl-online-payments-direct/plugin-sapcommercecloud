package com.worldline.direct.populator.hostedcheckout;

import com.google.common.base.Preconditions;
import com.onlinepayments.domain.*;
import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import com.worldline.direct.enums.OperationCodesEnum;
import com.worldline.direct.model.WorldlineConfigurationModel;
import com.worldline.direct.service.WorldlinePaymentModeService;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.payment.PaymentInfoModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import org.apache.commons.lang.BooleanUtils;

import java.util.List;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

public class WorldlineHostedCheckoutCardPopulator implements Populator<AbstractOrderModel, CreateHostedCheckoutRequest> {

    private static final String ECOMMERCE = "ECOMMERCE";

    private static final String FIRST_RECCURANCE = "first";
    private List<String> salePaymentProduct;

    public static final String CHALLENGE_REQUIRED = "challenge-required";
    public static final String CARD_HOLDER_INITIATED = "cardholderInitiated";
    public static final String LOW_VALUE = "low-value";

    public static final String CARTES_BANCAIRES_SINGLE_SALE = "single-amount";
    public static final String CARTES_BANCAIRES_SINGLE_AUTH = "payment-upon-shipment";
    public static final String CARTES_BANCAIRES_RECURRING = "other-recurring-payments";

    private WorldlinePaymentModeService worldlinePaymentModeService;

    @Override
    public void populate(AbstractOrderModel abstractOrderModel, CreateHostedCheckoutRequest createHostedCheckoutRequest) throws ConversionException {
        validateParameterNotNull(abstractOrderModel, "abstractOrderModel cannot be null!");
        validateParameterNotNull(abstractOrderModel.getPaymentInfo(), "PaymentInfo cannot be null!");
        Preconditions.checkArgument(abstractOrderModel.getPaymentInfo() instanceof WorldlinePaymentInfoModel, "Payment has to be WorldlinePaymentInfo");

        final WorldlinePaymentInfoModel paymentInfo = (WorldlinePaymentInfoModel) abstractOrderModel.getPaymentInfo();

        if (WorldlinedirectcoreConstants.PAYMENT_METHOD_TYPE.CARD.getValue().equals(paymentInfo.getPaymentMethod())) {
            createHostedCheckoutRequest.setCardPaymentMethodSpecificInput(getCardPaymentMethodSpecificInput(abstractOrderModel, paymentInfo));
        }

    }

    private CardPaymentMethodSpecificInputBase getCardPaymentMethodSpecificInput(AbstractOrderModel abstractOrderModel, WorldlinePaymentInfoModel paymentInfo) {
        final WorldlineConfigurationModel currentWorldlineConfiguration = abstractOrderModel.getStore().getWorldlineConfiguration();
        final CardPaymentMethodSpecificInputBase cardPaymentMethodSpecificInput = new CardPaymentMethodSpecificInputBase();
        cardPaymentMethodSpecificInput.setTransactionChannel(ECOMMERCE);

        cardPaymentMethodSpecificInput.setToken(paymentInfo.getToken());
        if (WorldlinedirectcoreConstants.PAYMENT_METHOD_GROUP_CARDS != paymentInfo.getId()) {
            cardPaymentMethodSpecificInput.setPaymentProductId(paymentInfo.getId());
        }
        boolean isExemptionRequestLowValue = BooleanUtils.isTrue(currentWorldlineConfiguration.isExemptionRequest()) && abstractOrderModel.getCurrency().getIsocode().equals("EUR") && abstractOrderModel.getTotalPrice() < 30;
        boolean isChallengeRequired = BooleanUtils.isTrue(currentWorldlineConfiguration.isChallengeRequired());
        if (isExemptionRequestLowValue || isChallengeRequired) {
            ThreeDSecureBase threeDSecureBase = new ThreeDSecureBase();
            if (isChallengeRequired) {
                threeDSecureBase.setChallengeIndicator(CHALLENGE_REQUIRED);
            } else if (isExemptionRequestLowValue) {
                threeDSecureBase.setExemptionRequest(LOW_VALUE);
            }
            cardPaymentMethodSpecificInput.setThreeDSecure(threeDSecureBase);
        }
        boolean isSale = false;
        if (worldlinePaymentModeService.isIntersolve(String.valueOf(paymentInfo.getId()))) {
            cardPaymentMethodSpecificInput.setAuthorizationMode(OperationCodesEnum.SALE.getCode());
            isSale = true;
        } else if (currentWorldlineConfiguration.getDefaultOperationCode() != null) {
            cardPaymentMethodSpecificInput.setAuthorizationMode(currentWorldlineConfiguration.getDefaultOperationCode().getCode());
            isSale = OperationCodesEnum.SALE.equals(currentWorldlineConfiguration.getDefaultOperationCode());
        }

        if (paymentInfo.isRecurringToken()) {
            cardPaymentMethodSpecificInput.setTokenize(true);
            CardRecurrenceDetails cardRecurrenceDetails = new CardRecurrenceDetails();
            cardRecurrenceDetails.setRecurringPaymentSequenceIndicator(FIRST_RECCURANCE);
            cardPaymentMethodSpecificInput.setRecurring(cardRecurrenceDetails);
        } else {
            cardPaymentMethodSpecificInput.setTokenize(false);
        }
        if (WorldlinedirectcoreConstants.PAYMENT_METHOD_CARTES_BANCAIRES_FRICTIONLESS == paymentInfo.getId()) {
            PaymentProduct130SpecificInput paymentProduct130SpecificInput = getPaymentProduct130SpecificInput(abstractOrderModel, paymentInfo, isSale);
            cardPaymentMethodSpecificInput.setPaymentProduct130SpecificInput(paymentProduct130SpecificInput);
        }

        return cardPaymentMethodSpecificInput;
    }

    /**
     * Generates a PaymentProduct130SpecificInput for Cartes Bancaires. Provides numberOfItems which will represtent the
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

    public void setWorldlinePaymentModeService(WorldlinePaymentModeService worldlinePaymentModeService) {
        this.worldlinePaymentModeService = worldlinePaymentModeService;
    }
}
