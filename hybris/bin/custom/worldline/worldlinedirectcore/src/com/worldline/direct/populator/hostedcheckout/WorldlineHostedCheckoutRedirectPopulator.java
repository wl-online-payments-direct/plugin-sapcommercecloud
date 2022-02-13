package com.worldline.direct.populator.hostedcheckout;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import com.worldline.direct.service.WorldlineConfigurationService;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import de.hybris.platform.servicelayer.session.SessionService;

import com.google.common.base.Preconditions;

import com.ingenico.direct.domain.CreateHostedCheckoutRequest;
import com.ingenico.direct.domain.RedirectPaymentMethodSpecificInput;
import com.ingenico.direct.domain.RedirectPaymentProduct809SpecificInput;
import com.ingenico.direct.domain.RedirectPaymentProduct840SpecificInput;
import com.ingenico.direct.domain.RedirectionData;
import com.worldline.direct.enums.OperationCodesEnum;
import com.worldline.direct.model.WorldlineConfigurationModel;

public class WorldlineHostedCheckoutRedirectPopulator implements Populator<AbstractOrderModel, CreateHostedCheckoutRequest> {

    private SessionService sessionService;
    private WorldlineConfigurationService worldlineConfigurationService;

    @Override
    public void populate(AbstractOrderModel abstractOrderModel, CreateHostedCheckoutRequest createHostedCheckoutRequest) throws ConversionException {
        validateParameterNotNull(abstractOrderModel, "order cannot be null!");
        validateParameterNotNull(abstractOrderModel.getPaymentInfo(), "PaymentInfo cannot be null!");
        Preconditions.checkArgument(abstractOrderModel.getPaymentInfo() instanceof WorldlinePaymentInfoModel, "Payment has to be WorldlinePaymentInfo");

        final WorldlinePaymentInfoModel paymentInfo = (WorldlinePaymentInfoModel) abstractOrderModel.getPaymentInfo();

        if (WorldlinedirectcoreConstants.PAYMENT_METHOD_TYPE.REDIRECT.getValue().equals(paymentInfo.getPaymentMethod())) {
            createHostedCheckoutRequest.setRedirectPaymentMethodSpecificInput(getRedirectPaymentMethodSpecificInput(paymentInfo));
        }

    }

    private RedirectPaymentMethodSpecificInput getRedirectPaymentMethodSpecificInput(WorldlinePaymentInfoModel paymentInfo) {
        final RedirectPaymentMethodSpecificInput redirectPaymentMethodSpecificInput = new RedirectPaymentMethodSpecificInput();
        redirectPaymentMethodSpecificInput.setPaymentProductId(paymentInfo.getId());
        redirectPaymentMethodSpecificInput.setRequiresApproval(requiresApproval());
        redirectPaymentMethodSpecificInput.setTokenize(Boolean.FALSE);
        RedirectionData redirectionData = new RedirectionData();
        redirectionData.setReturnUrl(getHostedCheckoutReturnUrl());
        redirectPaymentMethodSpecificInput.setRedirectionData(redirectionData);
        switch (paymentInfo.getId()) {
            case WorldlinedirectcoreConstants.PAYMENT_METHOD_IDEAL:
                RedirectPaymentProduct809SpecificInput iDealSpecificInfo = new RedirectPaymentProduct809SpecificInput();
                iDealSpecificInfo.setIssuerId(paymentInfo.getPaymentProductDirectoryId());
                redirectPaymentMethodSpecificInput.setPaymentProduct809SpecificInput(iDealSpecificInfo);
                break;
            case WorldlinedirectcoreConstants.PAYMENT_METHOD_PAYPAL:
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

    private Boolean requiresApproval() {
        final WorldlineConfigurationModel currentWorldlineConfiguration = worldlineConfigurationService.getCurrentWorldlineConfiguration();
        OperationCodesEnum defaultOperationCode = currentWorldlineConfiguration.getDefaultOperationCode();
        if (OperationCodesEnum.SALE.equals(defaultOperationCode)) {
            return false;
        }
        return true;
    }

    private String getHostedCheckoutReturnUrl() {
        return sessionService.getAttribute("hostedCheckoutReturnUrl");
    }

    public void setSessionService(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    public void setWorldlineConfigurationService(WorldlineConfigurationService worldlineConfigurationService) {
        this.worldlineConfigurationService = worldlineConfigurationService;
    }
}
