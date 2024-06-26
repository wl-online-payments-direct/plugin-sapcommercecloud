package com.worldline.direct.populator.hostedcheckout;

import com.google.common.base.Preconditions;
import com.onlinepayments.domain.CardPaymentMethodSpecificInputBase;
import com.onlinepayments.domain.CardRecurrenceDetails;
import com.onlinepayments.domain.CreateHostedCheckoutRequest;
import com.onlinepayments.domain.ThreeDSecureBase;
import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import com.worldline.direct.enums.OperationCodesEnum;
import com.worldline.direct.model.WorldlineConfigurationModel;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.AbstractOrderModel;
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
        if (salePaymentProduct.contains(paymentInfo.getId().toString())) {
            cardPaymentMethodSpecificInput.setAuthorizationMode(OperationCodesEnum.SALE.getCode());
        } else if (currentWorldlineConfiguration.getDefaultOperationCode() != null) {
            cardPaymentMethodSpecificInput.setAuthorizationMode(currentWorldlineConfiguration.getDefaultOperationCode().getCode());
        }

        if (paymentInfo.isRecurringToken()) {
            cardPaymentMethodSpecificInput.setTokenize(true);
            CardRecurrenceDetails cardRecurrenceDetails = new CardRecurrenceDetails();
            cardRecurrenceDetails.setRecurringPaymentSequenceIndicator(FIRST_RECCURANCE);
            cardPaymentMethodSpecificInput.setRecurring(cardRecurrenceDetails);
//
//            cardPaymentMethodSpecificInput.setUnscheduledCardOnFileRequestor(CARD_HOLDER_INITIATED);
//            cardPaymentMethodSpecificInput.setUnscheduledCardOnFileSequenceIndicator(FIRST_RECCURANCE);
        } else {
            cardPaymentMethodSpecificInput.setTokenize(false);
        }

        return cardPaymentMethodSpecificInput;
    }

    public void setSalePaymentProduct(List<String> salePaymentProduct) {
        this.salePaymentProduct = salePaymentProduct;
    }
}
