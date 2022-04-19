package com.worldline.direct.populator.hostedcheckout;

import com.google.common.base.Preconditions;
import com.ingenico.direct.domain.*;
import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import com.worldline.direct.enums.OperationCodesEnum;
import com.worldline.direct.model.WorldlineConfigurationModel;
import com.worldline.direct.service.WorldlineConfigurationService;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import de.hybris.platform.servicelayer.session.SessionService;

import static com.worldline.direct.populator.hostedcheckout.WorldlineHostedCheckoutBasicPopulator.HOSTED_CHECKOUT_RETURN_URL;
import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

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
        return sessionService.getAttribute(HOSTED_CHECKOUT_RETURN_URL);
    }

    public void setSessionService(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    public void setWorldlineConfigurationService(WorldlineConfigurationService worldlineConfigurationService) {
        this.worldlineConfigurationService = worldlineConfigurationService;
    }
}
