package com.worldline.direct.populator.hostedcheckout;

import com.google.common.base.Preconditions;
import com.onlinepayments.domain.*;
import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import com.worldline.direct.enums.OperationCodesEnum;
import com.worldline.direct.enums.WeroCaptureTrigger;
import com.worldline.direct.model.OneyPaymentModeModel;
import com.worldline.direct.model.WorldlineConfigurationModel;
import com.worldline.direct.service.WorldlineConfigurationService;
import com.worldline.direct.service.WorldlinePaymentModeService;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.payment.PaymentModeModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import de.hybris.platform.servicelayer.session.SessionService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.worldline.direct.populator.hostedcheckout.WorldlineHostedCheckoutBasicPopulator.HOSTED_CHECKOUT_RETURN_URL;
import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

public class WorldlineHostedCheckoutRedirectPopulator implements Populator<AbstractOrderModel, CreateHostedCheckoutRequest> {

    private SessionService sessionService;
    private WorldlineConfigurationService worldlineConfigurationService;
    private WorldlinePaymentModeService worldlinePaymentModeService;

    private final static Logger LOGGER = LoggerFactory.getLogger(WorldlineHostedCheckoutRedirectPopulator.class);

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
        redirectPaymentMethodSpecificInput.setRequiresApproval(requiresApproval(String.valueOf(paymentInfo.getId())));
        redirectPaymentMethodSpecificInput.setTokenize(Boolean.FALSE);
        RedirectionData redirectionData = new RedirectionData();
        redirectionData.setReturnUrl(getHostedCheckoutReturnUrl());
        redirectPaymentMethodSpecificInput.setRedirectionData(redirectionData);
        switch (paymentInfo.getId()) {
            case WorldlinedirectcoreConstants.PAYMENT_METHOD_PAYPAL:
                RedirectPaymentProduct840SpecificInput redirectPaymentProduct840SpecificInput = new RedirectPaymentProduct840SpecificInput();
                redirectPaymentProduct840SpecificInput.setAddressSelectionAtPayPal(Boolean.FALSE);
                redirectPaymentMethodSpecificInput.setPaymentProduct840SpecificInput(redirectPaymentProduct840SpecificInput);
                break;
            case WorldlinedirectcoreConstants.PAYMENT_METHOD_BANK_TRANSFER:
                Boolean instantPaymentOnly = worldlineConfigurationService.getCurrentWorldlineConfiguration().isInstantBankTransfers();
                RedirectPaymentProduct5408SpecificInput redirectPaymentProduct5408SpecificInput = new RedirectPaymentProduct5408SpecificInput();
                redirectPaymentProduct5408SpecificInput.setInstantPaymentOnly(instantPaymentOnly);
                redirectPaymentMethodSpecificInput.setPaymentProduct5408SpecificInput(redirectPaymentProduct5408SpecificInput);
                break;
            case WorldlinedirectcoreConstants.PAYMENT_METHOD_CHEQUES_VACANCE_CONNECT:
                RedirectPaymentProduct5403SpecificInput redirectPaymentProduct5403SpecificInput = new RedirectPaymentProduct5403SpecificInput();
                redirectPaymentProduct5403SpecificInput.setCompleteRemainingPaymentAmount(true);
                redirectPaymentMethodSpecificInput.setPaymentProduct5403SpecificInput(redirectPaymentProduct5403SpecificInput);
                break;
            case WorldlinedirectcoreConstants.PAYMENT_METHOD_WERO:
                String weroCaptureTrigger = getWeroCaptureTrigger();
                if (StringUtils.isNotBlank(weroCaptureTrigger)) {
                    RedirectPaymentProduct900SpecificInput redirectPaymentProduct900SpecificInput = new RedirectPaymentProduct900SpecificInput();
                    redirectPaymentProduct900SpecificInput.setCaptureTrigger(weroCaptureTrigger);
                    redirectPaymentMethodSpecificInput.setPaymentProduct900SpecificInput(redirectPaymentProduct900SpecificInput);
                }
                break;
            case WorldlinedirectcoreConstants.PAYMENT_METHOD_MEALVOUCHER:
                RedirectPaymentProduct5402SpecificInput redirectPaymentProduct5402SpecificInput = new RedirectPaymentProduct5402SpecificInput();
                redirectPaymentProduct5402SpecificInput.setCompleteRemainingPaymentAmount(true);
                redirectPaymentMethodSpecificInput.setPaymentProduct5402SpecificInput(redirectPaymentProduct5402SpecificInput);
                break;
            default:
                // No Specific parameter needed for this paymentMethod
                break;
        }
        PaymentModeModel paymentMode = worldlinePaymentModeService.getPaymentModeForCode(String.valueOf(paymentInfo.getId()));
        if (paymentMode instanceof OneyPaymentModeModel oneyPaymentModeModel) {
            if(oneyPaymentModeModel.getPaymentOption() != null) {
                redirectPaymentMethodSpecificInput.setPaymentOption(String.valueOf(oneyPaymentModeModel.getPaymentOption()));
            }
        }

        return redirectPaymentMethodSpecificInput;
    }

    private String getWeroCaptureTrigger() {
        WorldlineConfigurationModel worldlineConfiguration = worldlineConfigurationService.getCurrentWorldlineConfiguration();
        if (worldlineConfiguration == null) {
            return StringUtils.EMPTY;
        }
        WeroCaptureTrigger weroCaptureTrigger = worldlineConfiguration.getWeroCaptureTrigger();
        if (weroCaptureTrigger == null) {
            LOGGER.warn("No Wero capture trigger set, but Wero is being used. Please set this against your " +
                    "WorldlineConfiguration! The field will be omitted for this transaction, which Worldline " +
                    "rejects when the payment requires approval (authorisation mode).");
            return StringUtils.EMPTY;
        }
        return weroCaptureTrigger.getCode();
    }
    private Boolean requiresApproval(String paymentModeId) {
        // Find out if this payment mode is 'sale only' (e.g. does not support auth)
        if (worldlinePaymentModeService.isSaleOnly(paymentModeId)) {
            return Boolean.FALSE;
        }
        // It's not, so return the current configured value.
        final WorldlineConfigurationModel currentWorldlineConfiguration = worldlineConfigurationService.getCurrentWorldlineConfiguration();
        OperationCodesEnum defaultOperationCode = currentWorldlineConfiguration.getDefaultOperationCode();
        return !OperationCodesEnum.SALE.equals(defaultOperationCode);
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

    public void setWorldlinePaymentModeService(WorldlinePaymentModeService worldlinePaymentModeService) {
        this.worldlinePaymentModeService = worldlinePaymentModeService;
    }
}
