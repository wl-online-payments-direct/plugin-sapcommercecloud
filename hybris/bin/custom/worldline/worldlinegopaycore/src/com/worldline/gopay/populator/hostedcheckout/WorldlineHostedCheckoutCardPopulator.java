package com.worldline.gopay.populator.hostedcheckout;

import com.google.common.base.Preconditions;
import com.onlinepayments.domain.*;
import com.worldline.gopay.constants.WorldlinegopaycoreConstants;
import com.worldline.gopay.enums.OperationCodesEnum;
import com.worldline.gopay.factory.impl.WorldlinePaymentProduct130SpecificInputFactory;
import com.worldline.gopay.factory.impl.WorldlineThreeDSecureFactory;
import com.worldline.gopay.model.WorldlineConfigurationModel;
import com.worldline.gopay.service.WorldlinePaymentModeService;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;

import de.hybris.platform.servicelayer.dto.converter.ConversionException;

import java.util.List;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

public class WorldlineHostedCheckoutCardPopulator implements Populator<AbstractOrderModel, CreateHostedCheckoutRequest> {

    private static final String ECOMMERCE = "ECOMMERCE";

    private static final String FIRST_RECCURANCE = "first";
    private List<String> salePaymentProduct;

    public static final String CHALLENGE_REQUIRED = "challenge-required";
    public static final String CARD_HOLDER_INITIATED = "cardholderInitiated";
    public static final String LOW_VALUE = "low-value";

    private WorldlinePaymentModeService worldlinePaymentModeService;
    @Override
    public void populate(AbstractOrderModel abstractOrderModel, CreateHostedCheckoutRequest createHostedCheckoutRequest) throws ConversionException {
        validateParameterNotNull(abstractOrderModel, "abstractOrderModel cannot be null!");
        validateParameterNotNull(abstractOrderModel.getPaymentInfo(), "PaymentInfo cannot be null!");
        Preconditions.checkArgument(abstractOrderModel.getPaymentInfo() instanceof WorldlinePaymentInfoModel, "Payment has to be WorldlinePaymentInfo");

        final WorldlinePaymentInfoModel paymentInfo = (WorldlinePaymentInfoModel) abstractOrderModel.getPaymentInfo();

        if (WorldlinegopaycoreConstants.PAYMENT_METHOD_TYPE.CARD.getValue().equals(paymentInfo.getPaymentMethod())) {
            createHostedCheckoutRequest.setCardPaymentMethodSpecificInput(getCardPaymentMethodSpecificInput(abstractOrderModel, paymentInfo));
        }

    }

    private CardPaymentMethodSpecificInputBase getCardPaymentMethodSpecificInput(AbstractOrderModel abstractOrderModel, WorldlinePaymentInfoModel paymentInfo) {
        final WorldlineConfigurationModel currentWorldlineConfiguration = abstractOrderModel.getStore().getWorldlineConfiguration();
        final CardPaymentMethodSpecificInputBase cardPaymentMethodSpecificInput = new CardPaymentMethodSpecificInputBase();
        cardPaymentMethodSpecificInput.setTransactionChannel(ECOMMERCE);

        cardPaymentMethodSpecificInput.setToken(paymentInfo.getToken());
        if (WorldlinegopaycoreConstants.PAYMENT_METHOD_GROUP_CARDS != paymentInfo.getId()) {
            cardPaymentMethodSpecificInput.setPaymentProductId(paymentInfo.getId());
        }
        ThreeDSecureBase threeDSecure = WorldlineThreeDSecureFactory.createThreeDSecureBase(currentWorldlineConfiguration, abstractOrderModel);
        cardPaymentMethodSpecificInput.setThreeDSecure(threeDSecure);

        boolean isSale = false;
        if (worldlinePaymentModeService.isSaleOnly(String.valueOf(paymentInfo.getId()))) {
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
        }
        if (WorldlinegopaycoreConstants.PAYMENT_METHOD_CARTES_BANCAIRES_FRICTIONLESS == paymentInfo.getId()) {
            PaymentProduct130SpecificInput paymentProduct130SpecificInput = WorldlinePaymentProduct130SpecificInputFactory.getPaymentProduct130SpecificInput(currentWorldlineConfiguration, abstractOrderModel, paymentInfo, isSale);
            if (paymentProduct130SpecificInput != null) {
                cardPaymentMethodSpecificInput.setPaymentProduct130SpecificInput(paymentProduct130SpecificInput);
            }
        }

        return cardPaymentMethodSpecificInput;
    }

    public void setWorldlinePaymentModeService(WorldlinePaymentModeService worldlinePaymentModeService) {
        this.worldlinePaymentModeService = worldlinePaymentModeService;
    }

}
