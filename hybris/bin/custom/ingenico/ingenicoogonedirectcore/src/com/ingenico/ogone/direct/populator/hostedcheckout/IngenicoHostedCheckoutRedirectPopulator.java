package com.ingenico.ogone.direct.populator.hostedcheckout;

import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_METHOD_IDEAL;
import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_METHOD_PAYPAL;
import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_METHOD_TYPE.REDIRECT;
import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.payment.IngenicoPaymentInfoModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import de.hybris.platform.servicelayer.session.SessionService;

import com.google.common.base.Preconditions;

import com.ingenico.direct.domain.CreateHostedCheckoutRequest;
import com.ingenico.direct.domain.RedirectPaymentMethodSpecificInput;
import com.ingenico.direct.domain.RedirectPaymentProduct809SpecificInput;
import com.ingenico.direct.domain.RedirectPaymentProduct840SpecificInput;
import com.ingenico.direct.domain.RedirectionData;

public class IngenicoHostedCheckoutRedirectPopulator implements Populator<CartModel, CreateHostedCheckoutRequest> {

    private SessionService sessionService;

    @Override
    public void populate(CartModel cartModel, CreateHostedCheckoutRequest createHostedCheckoutRequest) throws ConversionException {
        validateParameterNotNull(cartModel, "cart cannot be null!");
        validateParameterNotNull(cartModel.getPaymentInfo(), "PaymentInfo cannot be null!");
        Preconditions.checkArgument(cartModel.getPaymentInfo() instanceof IngenicoPaymentInfoModel, "Payment has to be IngenicoPaymentInfo");

        final IngenicoPaymentInfoModel paymentInfo = (IngenicoPaymentInfoModel) cartModel.getPaymentInfo();

        if (REDIRECT.getValue().equals(paymentInfo.getPaymentMethod())) {
            createHostedCheckoutRequest.setRedirectPaymentMethodSpecificInput(getRedirectPaymentMethodSpecificInput(paymentInfo));
        }

    }

    private RedirectPaymentMethodSpecificInput getRedirectPaymentMethodSpecificInput(IngenicoPaymentInfoModel paymentInfo) {
        final RedirectPaymentMethodSpecificInput redirectPaymentMethodSpecificInput = new RedirectPaymentMethodSpecificInput();
        redirectPaymentMethodSpecificInput.setPaymentProductId(paymentInfo.getId());
        redirectPaymentMethodSpecificInput.setRequiresApproval(Boolean.FALSE);
        redirectPaymentMethodSpecificInput.setTokenize(Boolean.FALSE);
        RedirectionData redirectionData = new RedirectionData();
        redirectionData.setReturnUrl(getHostedCheckoutReturnUrl());
        redirectPaymentMethodSpecificInput.setRedirectionData(redirectionData);
        switch (paymentInfo.getId()) {
            case PAYMENT_METHOD_IDEAL:
                RedirectPaymentProduct809SpecificInput iDealSpecificInfo = new RedirectPaymentProduct809SpecificInput();
                iDealSpecificInfo.setIssuerId(paymentInfo.getPaymentProductDirectoryId());
                redirectPaymentMethodSpecificInput.setPaymentProduct809SpecificInput(iDealSpecificInfo);
                break;
            case PAYMENT_METHOD_PAYPAL:
                RedirectPaymentProduct840SpecificInput redirectPaymentProduct840SpecificInput = new RedirectPaymentProduct840SpecificInput();
                redirectPaymentProduct840SpecificInput.setAddressSelectionAtPayPal(Boolean.FALSE);
                redirectPaymentMethodSpecificInput.setPaymentProduct840SpecificInput(redirectPaymentProduct840SpecificInput);
                break;
            default:
                // No Specific parameter needed for this paymentMethod
                break;
        }


        return redirectPaymentMethodSpecificInput;
    }

    private String getHostedCheckoutReturnUrl() {
        return sessionService.getAttribute("hostedCheckoutReturnUrl");
    }

    public void setSessionService(SessionService sessionService) {
        this.sessionService = sessionService;
    }

}
