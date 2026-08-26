package com.worldline.gopay.populator.hostedtokenization;

import com.google.common.base.Preconditions;
import com.onlinepayments.domain.CreatePaymentRequest;
import com.onlinepayments.domain.GPayThreeDSecure;
import com.onlinepayments.domain.MobilePaymentMethodSpecificInput;
import com.onlinepayments.domain.MobilePaymentProduct320SpecificInput;
import com.onlinepayments.domain.Product320Recurring;
import com.onlinepayments.domain.RedirectionData;
import com.worldline.gopay.constants.WorldlinegopaycoreConstants;
import com.worldline.gopay.enums.OperationCodesEnum;
import com.worldline.gopay.factory.impl.WorldlineThreeDSecureFactory;
import com.worldline.gopay.model.WorldlineConfigurationModel;
import com.worldline.gopay.service.WorldlineConfigurationService;
import com.worldline.gopay.service.WorldlinePaymentModeService;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import de.hybris.platform.servicelayer.session.SessionService;
import org.apache.commons.lang.BooleanUtils;

import static com.worldline.gopay.constants.WorldlinegopaycoreConstants.GOOGLE_PAY_ENCRYPTED_PAYMENT_DATA_SESSION_KEY;
import static com.worldline.gopay.constants.WorldlinegopaycoreConstants.GOOGLE_PAY_MOBILE_DEVICE_SESSION_KEY;
import static com.worldline.gopay.populator.hostedtokenization.WorldlineHostedTokenizationBasicPopulator.HOSTED_TOKENIZATION_RETURN_URL;
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
        if (!WorldlinegopaycoreConstants.PAYMENT_METHOD_TYPE.MOBILE.getValue().equals(paymentInfo.getPaymentMethod())
              || !Integer.valueOf(WorldlinegopaycoreConstants.PAYMENT_METHOD_GOOGLEPAY).equals(paymentInfo.getId())) {
            return;
        }

        MobilePaymentMethodSpecificInput mobileInput = new MobilePaymentMethodSpecificInput();
        mobileInput.setPaymentProductId(WorldlinegopaycoreConstants.PAYMENT_METHOD_GOOGLEPAY);
        mobileInput.setEncryptedPaymentData(sessionService.getAttribute(GOOGLE_PAY_ENCRYPTED_PAYMENT_DATA_SESSION_KEY));
        MobilePaymentProduct320SpecificInput product320SpecificInput = createProduct320SpecificInput(abstractOrderModel, paymentInfo);
        mobileInput.setPaymentProduct320SpecificInput(product320SpecificInput);

        Boolean requiresApproval = getRequiresApproval(paymentInfo, product320SpecificInput);
        if (requiresApproval != null) {
            mobileInput.setRequiresApproval(requiresApproval);
        }

        createPaymentRequest.setMobilePaymentMethodSpecificInput(mobileInput);
    }

    private MobilePaymentProduct320SpecificInput createProduct320SpecificInput(AbstractOrderModel order, WorldlinePaymentInfoModel paymentInfo) {
        MobilePaymentProduct320SpecificInput productInput = new MobilePaymentProduct320SpecificInput();

        if (paymentInfo.isRecurringToken()) {
            productInput.setIsRecurring(Boolean.TRUE);
            productInput.setTokenize(Boolean.TRUE);
            Product320Recurring recurring = new Product320Recurring();
            recurring.setRecurringPaymentSequenceIndicator(RECURRING_FIRST);
            productInput.setRecurring(recurring);
        } else if (order.getTotalPrice() == 0d) {
            productInput.setTokenize(Boolean.TRUE);
        }

        if (!BooleanUtils.isTrue(sessionService.getAttribute(GOOGLE_PAY_MOBILE_DEVICE_SESSION_KEY))) {
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

    private Boolean getRequiresApproval(WorldlinePaymentInfoModel paymentInfo, MobilePaymentProduct320SpecificInput product320SpecificInput) {
        if (product320SpecificInput.getThreeDSecure() != null) {
            return Boolean.TRUE;
        }
        WorldlineConfigurationModel configuration = worldlineConfigurationService.getCurrentWorldlineConfiguration();
        if (worldlinePaymentModeService.isSaleOnly(String.valueOf(paymentInfo.getId()))) {
            return null;
        }
        if (configuration.getDefaultOperationCode() != null
              && OperationCodesEnum.FINAL_AUTHORIZATION.getCode().equals(configuration.getDefaultOperationCode().getCode())) {
            return Boolean.TRUE;
        }
        return null;
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
