package com.ingenico.ogone.direct.populator.hostedcheckout;

import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_METHOD_TYPE.CARD;
import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.payment.IngenicoPaymentInfoModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;

import com.google.common.base.Preconditions;
import org.apache.commons.lang.StringUtils;

import com.ingenico.direct.domain.CardPaymentMethodSpecificInputBase;
import com.ingenico.direct.domain.CreateHostedCheckoutRequest;
import com.ingenico.ogone.direct.model.IngenicoConfigurationModel;
import com.ingenico.ogone.direct.service.IngenicoConfigurationService;

public class IngenicoHostedCheckoutCardPopulator implements Populator<CartModel, CreateHostedCheckoutRequest> {

    private static final String ECOMMERCE = "ECOMMERCE";
    private IngenicoConfigurationService ingenicoConfigurationService;

    @Override
    public void populate(CartModel cartModel, CreateHostedCheckoutRequest createHostedCheckoutRequest) throws ConversionException {
        validateParameterNotNull(cartModel, "cart cannot be null!");
        validateParameterNotNull(cartModel.getPaymentInfo(), "PaymentInfo cannot be null!");
        Preconditions.checkArgument(cartModel.getPaymentInfo() instanceof IngenicoPaymentInfoModel, "Payment has to be IngenicoPaymentInfo");

        final IngenicoPaymentInfoModel paymentInfo = (IngenicoPaymentInfoModel) cartModel.getPaymentInfo();

        if (CARD.getValue().equals(paymentInfo.getPaymentMethod())) {
            createHostedCheckoutRequest.setCardPaymentMethodSpecificInput(getCardPaymentMethodSpecificInput(paymentInfo));
        }

    }

    private CardPaymentMethodSpecificInputBase getCardPaymentMethodSpecificInput(IngenicoPaymentInfoModel paymentInfo) {
        final IngenicoConfigurationModel currentIngenicoConfiguration = ingenicoConfigurationService.getCurrentIngenicoConfiguration();

        final CardPaymentMethodSpecificInputBase cardPaymentMethodSpecificInput = new CardPaymentMethodSpecificInputBase();
        if (currentIngenicoConfiguration.getDefaultOperationCode() != null) {
            cardPaymentMethodSpecificInput.setAuthorizationMode(currentIngenicoConfiguration.getDefaultOperationCode().getCode());
        }
        cardPaymentMethodSpecificInput.setTransactionChannel(ECOMMERCE);
        cardPaymentMethodSpecificInput.setTokenize(false);
        cardPaymentMethodSpecificInput.setPaymentProductId(paymentInfo.getId());

        return cardPaymentMethodSpecificInput;
    }

    public void setIngenicoConfigurationService(IngenicoConfigurationService ingenicoConfigurationService) {
        this.ingenicoConfigurationService = ingenicoConfigurationService;
    }
}
