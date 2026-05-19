package com.worldline.direct.populator.hostedtokenization;

import com.google.common.base.Preconditions;
import com.onlinepayments.domain.CreatePaymentRequest;
import com.onlinepayments.domain.GPayThreeDSecure;
import com.onlinepayments.domain.MobilePaymentMethodSpecificInput;
import com.onlinepayments.domain.MobilePaymentProduct320SpecificInput;
import com.onlinepayments.domain.Product320Recurring;
import com.onlinepayments.domain.RedirectionData;
import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import com.worldline.direct.enums.OperationCodesEnum;
import com.worldline.direct.factory.impl.WorldlineThreeDSecureFactory;
import com.worldline.direct.model.WorldlineConfigurationModel;
import com.worldline.direct.service.WorldlineConfigurationService;
import com.worldline.direct.service.WorldlinePaymentModeService;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import de.hybris.platform.servicelayer.session.SessionService;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;

import static com.worldline.direct.populator.hostedtokenization.WorldlineHostedTokenizationBasicPopulator.HOSTED_TOKENIZATION_RETURN_URL;
import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

public class WorldlineHostedTokenizationMobilePopulator implements Populator<AbstractOrderModel, CreatePaymentRequest> {

    private static final String RECURRING_FIRST = "first";

    private SessionService sessionService;
    private WorldlineConfigurationService worldlineConfigurationService;
    private WorldlinePaymentModeService worldlinePaymentModeService;

    @Override
    public void populate(AbstractOrderModel abstractOrderModel, CreatePaymentRequest createPaymentRequest) throws ConversionException {
        validateParameterNotNull(abstractOrderModel, "abstractOrderModel cannot be null!");
        validateParameterNotNull(abstractOrderModel.getPaymentInfo(), "PaymentInfo cannot be null!");
        Preconditions.checkArgument(abstractOrderModel.getPaymentInfo() instanceof WorldlinePaymentInfoModel, "Payment has to be WorldlinePaymentInfo");

        WorldlinePaymentInfoModel paymentInfo = (WorldlinePaymentInfoModel) abstractOrderModel.getPaymentInfo();
        if (!WorldlinedirectcoreConstants.PAYMENT_METHOD_TYPE.MOBILE.getValue().equals(paymentInfo.getPaymentMethod())
              || !Integer.valueOf(WorldlinedirectcoreConstants.PAYMENT_METHOD_GOOGLEPAY).equals(paymentInfo.getId())) {
            return;
        }

        MobilePaymentMethodSpecificInput mobileInput = new MobilePaymentMethodSpecificInput();
        mobileInput.setPaymentProductId(WorldlinedirectcoreConstants.PAYMENT_METHOD_GOOGLEPAY);
        mobileInput.setEncryptedPaymentData(paymentInfo.getGooglePayEncryptedPaymentData());
        mobileInput.setPaymentProduct320SpecificInput(createProduct320SpecificInput(abstractOrderModel, paymentInfo));

        String authorizationMode = getAuthorizationMode(paymentInfo);
        if (StringUtils.isNotBlank(authorizationMode)) {
            mobileInput.setAuthorizationMode(authorizationMode);
            mobileInput.setRequiresApproval(!OperationCodesEnum.SALE.getCode().equals(authorizationMode));
        }

        createPaymentRequest.setMobilePaymentMethodSpecificInput(mobileInput);
    }

    private MobilePaymentProduct320SpecificInput createProduct320SpecificInput(AbstractOrderModel order, WorldlinePaymentInfoModel paymentInfo) {
        MobilePaymentProduct320SpecificInput productInput = new MobilePaymentProduct320SpecificInput();
        productInput.setIsRecurring(paymentInfo.isRecurringToken());
        productInput.setTokenize(paymentInfo.isRecurringToken() || order.getTotalPrice() == 0d);

        if (paymentInfo.isRecurringToken()) {
            Product320Recurring recurring = new Product320Recurring();
            recurring.setRecurringPaymentSequenceIndicator(RECURRING_FIRST);
            productInput.setRecurring(recurring);
        }

        if (!BooleanUtils.isTrue(paymentInfo.getGooglePayMobileDevice())) {
            WorldlineConfigurationModel configuration = worldlineConfigurationService.getCurrentWorldlineConfiguration();
            GPayThreeDSecure threeDSecure = WorldlineThreeDSecureFactory.createGPayThreeDSecure(configuration, order);
            if (threeDSecure != null) {
                threeDSecure.setRedirectionData(new RedirectionData());
                threeDSecure.getRedirectionData().setReturnUrl(sessionService.getAttribute(HOSTED_TOKENIZATION_RETURN_URL));
                productInput.setThreeDSecure(threeDSecure);
            }
        }

        return productInput;
    }

    private String getAuthorizationMode(WorldlinePaymentInfoModel paymentInfo) {
        WorldlineConfigurationModel configuration = worldlineConfigurationService.getCurrentWorldlineConfiguration();
        if (worldlinePaymentModeService.isSaleOnly(String.valueOf(paymentInfo.getId()))) {
            return OperationCodesEnum.SALE.getCode();
        }
        if (configuration.getDefaultOperationCode() != null) {
            return configuration.getDefaultOperationCode().getCode();
        }
        return StringUtils.EMPTY;
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
