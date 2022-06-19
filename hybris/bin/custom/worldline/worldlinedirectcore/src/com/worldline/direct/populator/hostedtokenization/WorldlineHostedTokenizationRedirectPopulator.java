package com.worldline.direct.populator.hostedtokenization;

import com.google.common.base.Preconditions;
import com.onlinepayments.domain.CreatePaymentRequest;
import com.onlinepayments.domain.RedirectPaymentMethodSpecificInput;
import com.onlinepayments.domain.RedirectPaymentProduct809SpecificInput;
import com.onlinepayments.domain.RedirectionData;
import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import de.hybris.platform.servicelayer.session.SessionService;
import org.springframework.beans.factory.annotation.Required;

import static com.worldline.direct.constants.WorldlinedirectcoreConstants.PAYMENT_METHOD_IDEAL;
import static com.worldline.direct.populator.hostedtokenization.WorldlineHostedTokenizationBasicPopulator.HOSTED_TOKENIZATION_RETURN_URL;
import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

public class WorldlineHostedTokenizationRedirectPopulator implements Populator<AbstractOrderModel, CreatePaymentRequest> {

    private SessionService sessionService;

    @Override
    public void populate(AbstractOrderModel abstractOrderModel, CreatePaymentRequest createPaymentRequest) throws ConversionException {
        validateParameterNotNull(abstractOrderModel, "abstractOrderModel cannot be null!");
        validateParameterNotNull(abstractOrderModel.getPaymentInfo(), "PaymentInfo cannot be null!");
        Preconditions.checkArgument(abstractOrderModel.getPaymentInfo() instanceof WorldlinePaymentInfoModel, "Payment has to be WorldlinePaymentInfo");

        final WorldlinePaymentInfoModel paymentInfo = (WorldlinePaymentInfoModel) abstractOrderModel.getPaymentInfo();

        if (WorldlinedirectcoreConstants.PAYMENT_METHOD_TYPE.REDIRECT.getValue().equals(paymentInfo.getPaymentMethod())) {
            createPaymentRequest.setRedirectPaymentMethodSpecificInput(getRedirectPaymentMethodSpecificInput(abstractOrderModel));
        }

    }

    private RedirectPaymentMethodSpecificInput getRedirectPaymentMethodSpecificInput(AbstractOrderModel orderModel) {
        RedirectPaymentMethodSpecificInput input = new RedirectPaymentMethodSpecificInput();
        input.setPaymentProductId(PAYMENT_METHOD_IDEAL);
        input.setPaymentProduct809SpecificInput(new RedirectPaymentProduct809SpecificInput());
        input.getPaymentProduct809SpecificInput().setIssuerId(((WorldlinePaymentInfoModel) orderModel.getPaymentInfo()).getPaymentProductDirectoryId());
        input.setRedirectionData(new RedirectionData());
        input.getRedirectionData().setReturnUrl(getHostedTokenizationReturnUrl());
        return input;
    }

    private String getHostedTokenizationReturnUrl() {
        return sessionService.getAttribute(HOSTED_TOKENIZATION_RETURN_URL);
    }

    @Required
    public void setSessionService(SessionService sessionService) {
        this.sessionService = sessionService;
    }
}
