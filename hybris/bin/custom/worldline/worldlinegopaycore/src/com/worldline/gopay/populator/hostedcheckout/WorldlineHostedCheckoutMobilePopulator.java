package com.worldline.gopay.populator.hostedcheckout;

import com.google.common.base.Preconditions;
import com.onlinepayments.domain.CreateHostedCheckoutRequest;
import com.onlinepayments.domain.MobilePaymentMethodHostedCheckoutSpecificInput;
import com.worldline.gopay.constants.WorldlinegopaycoreConstants;
import com.worldline.gopay.enums.OperationCodesEnum;
import com.worldline.gopay.model.WorldlineConfigurationModel;
import com.worldline.gopay.service.WorldlinePaymentModeService;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

public class WorldlineHostedCheckoutMobilePopulator implements Populator<AbstractOrderModel, CreateHostedCheckoutRequest> {

    private WorldlinePaymentModeService worldlinePaymentModeService;

    @Override
    public void populate(AbstractOrderModel abstractOrderModel, CreateHostedCheckoutRequest createHostedCheckoutRequest) throws ConversionException {
        validateParameterNotNull(abstractOrderModel, "abstractOrderModel cannot be null!");
        validateParameterNotNull(abstractOrderModel.getPaymentInfo(), "PaymentInfo cannot be null!");
        Preconditions.checkArgument(abstractOrderModel.getPaymentInfo() instanceof WorldlinePaymentInfoModel, "Payment has to be WorldlinePaymentInfo");

        final WorldlinePaymentInfoModel paymentInfo = (WorldlinePaymentInfoModel) abstractOrderModel.getPaymentInfo();

        if (WorldlinegopaycoreConstants.PAYMENT_METHOD_TYPE.MOBILE.getValue().equals(paymentInfo.getPaymentMethod())) {
            createHostedCheckoutRequest.setMobilePaymentMethodSpecificInput(getMobilePaymentMethodSpecificInput(abstractOrderModel, paymentInfo));
        }
    }

    /**
     * Passing the payment product id through means the Hosted Checkout page opens straight into the wallet the
     * shopper already picked in the storefront (Apple Pay), rather than asking them to select it a second time.
     */
    private MobilePaymentMethodHostedCheckoutSpecificInput getMobilePaymentMethodSpecificInput(AbstractOrderModel abstractOrderModel, WorldlinePaymentInfoModel paymentInfo) {
        final MobilePaymentMethodHostedCheckoutSpecificInput mobilePaymentMethodSpecificInput = new MobilePaymentMethodHostedCheckoutSpecificInput();
        mobilePaymentMethodSpecificInput.setPaymentProductId(paymentInfo.getId());

        final String authorizationMode = getAuthorizationMode(abstractOrderModel, paymentInfo);
        if (authorizationMode != null) {
            mobilePaymentMethodSpecificInput.setAuthorizationMode(authorizationMode);
        }

        return mobilePaymentMethodSpecificInput;
    }

    /**
     * Without this the wallet falls back to the authorization mode configured on the Worldline account, which is not
     * necessarily the merchant's configured default operation code. Cards and redirects already send it explicitly.
     */
    private String getAuthorizationMode(AbstractOrderModel abstractOrderModel, WorldlinePaymentInfoModel paymentInfo) {
        if (worldlinePaymentModeService.isSaleOnly(String.valueOf(paymentInfo.getId()))) {
            return OperationCodesEnum.SALE.getCode();
        }
        final WorldlineConfigurationModel worldlineConfiguration = abstractOrderModel.getStore().getWorldlineConfiguration();
        if (worldlineConfiguration.getDefaultOperationCode() != null) {
            return worldlineConfiguration.getDefaultOperationCode().getCode();
        }
        return null;
    }

    public void setWorldlinePaymentModeService(WorldlinePaymentModeService worldlinePaymentModeService) {
        this.worldlinePaymentModeService = worldlinePaymentModeService;
    }
}
