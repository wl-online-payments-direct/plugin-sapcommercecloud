package com.worldline.direct.populator.hostedcheckout;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import com.worldline.direct.service.WorldlineConfigurationService;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;

import com.google.common.base.Preconditions;

import com.ingenico.direct.domain.CardPaymentMethodSpecificInputBase;
import com.ingenico.direct.domain.CreateHostedCheckoutRequest;
import com.worldline.direct.model.WorldlineConfigurationModel;

public class WorldlineHostedCheckoutCardPopulator implements Populator<AbstractOrderModel, CreateHostedCheckoutRequest> {

    private static final String ECOMMERCE = "ECOMMERCE";
    private WorldlineConfigurationService worldlineConfigurationService;

    @Override
    public void populate(AbstractOrderModel abstractOrderModel, CreateHostedCheckoutRequest createHostedCheckoutRequest) throws ConversionException {
        validateParameterNotNull(abstractOrderModel, "abstractOrderModel cannot be null!");
        validateParameterNotNull(abstractOrderModel.getPaymentInfo(), "PaymentInfo cannot be null!");
        Preconditions.checkArgument(abstractOrderModel.getPaymentInfo() instanceof WorldlinePaymentInfoModel, "Payment has to be WorldlinePaymentInfo");

        final WorldlinePaymentInfoModel paymentInfo = (WorldlinePaymentInfoModel) abstractOrderModel.getPaymentInfo();

        if (WorldlinedirectcoreConstants.PAYMENT_METHOD_TYPE.CARD.getValue().equals(paymentInfo.getPaymentMethod())) {
            createHostedCheckoutRequest.setCardPaymentMethodSpecificInput(getCardPaymentMethodSpecificInput(paymentInfo));
        }

    }

    private CardPaymentMethodSpecificInputBase getCardPaymentMethodSpecificInput(WorldlinePaymentInfoModel paymentInfo) {
        final WorldlineConfigurationModel currentWorldlineConfiguration = worldlineConfigurationService.getCurrentWorldlineConfiguration();

        final CardPaymentMethodSpecificInputBase cardPaymentMethodSpecificInput = new CardPaymentMethodSpecificInputBase();
        if (currentWorldlineConfiguration.getDefaultOperationCode() != null) {
            cardPaymentMethodSpecificInput.setAuthorizationMode(currentWorldlineConfiguration.getDefaultOperationCode().getCode());
        }
        cardPaymentMethodSpecificInput.setTransactionChannel(ECOMMERCE);
        cardPaymentMethodSpecificInput.setTokenize(false);
        cardPaymentMethodSpecificInput.setPaymentProductId(paymentInfo.getId());

        return cardPaymentMethodSpecificInput;
    }

    public void setWorldlineConfigurationService(WorldlineConfigurationService worldlineConfigurationService) {
        this.worldlineConfigurationService = worldlineConfigurationService;
    }
}