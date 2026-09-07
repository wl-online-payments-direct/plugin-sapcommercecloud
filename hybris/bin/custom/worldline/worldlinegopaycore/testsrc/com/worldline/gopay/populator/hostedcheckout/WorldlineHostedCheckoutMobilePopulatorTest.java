package com.worldline.gopay.populator.hostedcheckout;

import com.onlinepayments.domain.CreateHostedCheckoutRequest;
import com.worldline.gopay.constants.WorldlinegopaycoreConstants;
import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@UnitTest
public class WorldlineHostedCheckoutMobilePopulatorTest {

    private WorldlineHostedCheckoutMobilePopulator populator;

    @Before
    public void setUp() {
        populator = new WorldlineHostedCheckoutMobilePopulator();
    }

    @Test
    public void populateSendsApplePayPaymentProductId() {
        CreateHostedCheckoutRequest request = populate(
              WorldlinegopaycoreConstants.PAYMENT_METHOD_TYPE.MOBILE.getValue(),
              WorldlinegopaycoreConstants.PAYMENT_METHOD_APPLEPAY);

        assertEquals(Integer.valueOf(WorldlinegopaycoreConstants.PAYMENT_METHOD_APPLEPAY),
              request.getMobilePaymentMethodSpecificInput().getPaymentProductId());
    }

    @Test
    public void populateIgnoresNonMobilePaymentMethods() {
        CreateHostedCheckoutRequest request = populate(
              WorldlinegopaycoreConstants.PAYMENT_METHOD_TYPE.REDIRECT.getValue(),
              WorldlinegopaycoreConstants.PAYMENT_METHOD_PAYPAL);

        assertNull(request.getMobilePaymentMethodSpecificInput());
    }

    private CreateHostedCheckoutRequest populate(String paymentMethod, int paymentProductId) {
        CreateHostedCheckoutRequest request = new CreateHostedCheckoutRequest();
        populator.populate(order(paymentMethod, paymentProductId), request);
        return request;
    }

    private AbstractOrderModel order(String paymentMethod, int paymentProductId) {
        WorldlinePaymentInfoModel paymentInfo = new WorldlinePaymentInfoModel();
        paymentInfo.setPaymentMethod(paymentMethod);
        paymentInfo.setId(paymentProductId);

        CartModel order = new CartModel();
        order.setPaymentInfo(paymentInfo);
        return order;
    }
}
