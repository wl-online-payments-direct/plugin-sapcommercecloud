package com.worldline.gopay.populator.hostedcheckout;

import com.google.common.base.Preconditions;
import com.onlinepayments.domain.CreateHostedCheckoutRequest;
import com.onlinepayments.domain.MobilePaymentMethodHostedCheckoutSpecificInput;
import com.worldline.gopay.constants.WorldlinegopaycoreConstants;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

public class WorldlineHostedCheckoutMobilePopulator implements Populator<AbstractOrderModel, CreateHostedCheckoutRequest> {

    @Override
    public void populate(AbstractOrderModel abstractOrderModel, CreateHostedCheckoutRequest createHostedCheckoutRequest) throws ConversionException {
        validateParameterNotNull(abstractOrderModel, "abstractOrderModel cannot be null!");
        validateParameterNotNull(abstractOrderModel.getPaymentInfo(), "PaymentInfo cannot be null!");
        Preconditions.checkArgument(abstractOrderModel.getPaymentInfo() instanceof WorldlinePaymentInfoModel, "Payment has to be WorldlinePaymentInfo");

        final WorldlinePaymentInfoModel paymentInfo = (WorldlinePaymentInfoModel) abstractOrderModel.getPaymentInfo();

        if (WorldlinegopaycoreConstants.PAYMENT_METHOD_TYPE.MOBILE.getValue().equals(paymentInfo.getPaymentMethod())) {
            createHostedCheckoutRequest.setMobilePaymentMethodSpecificInput(getMobilePaymentMethodSpecificInput(paymentInfo));
        }
    }

    /**
     * Passing the payment product id through means the Hosted Checkout page opens straight into the wallet the
     * shopper already picked in the storefront (Apple Pay), rather than asking them to select it a second time.
     */
    private MobilePaymentMethodHostedCheckoutSpecificInput getMobilePaymentMethodSpecificInput(WorldlinePaymentInfoModel paymentInfo) {
        final MobilePaymentMethodHostedCheckoutSpecificInput mobilePaymentMethodSpecificInput = new MobilePaymentMethodHostedCheckoutSpecificInput();
        mobilePaymentMethodSpecificInput.setPaymentProductId(paymentInfo.getId());

        return mobilePaymentMethodSpecificInput;
    }
}
