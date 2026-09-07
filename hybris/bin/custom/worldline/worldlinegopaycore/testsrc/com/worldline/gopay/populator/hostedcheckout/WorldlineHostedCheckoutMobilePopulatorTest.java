package com.worldline.gopay.populator.hostedcheckout;

import com.onlinepayments.domain.CreateHostedCheckoutRequest;
import com.worldline.gopay.constants.WorldlinegopaycoreConstants;
import com.worldline.gopay.enums.OperationCodesEnum;
import com.worldline.gopay.model.WorldlineConfigurationModel;
import com.worldline.gopay.service.WorldlinePaymentModeService;
import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.store.BaseStoreModel;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@UnitTest
public class WorldlineHostedCheckoutMobilePopulatorTest {

    private final WorldlineConfigurationModel worldlineConfiguration = new WorldlineConfigurationModel();

    private boolean saleOnly;

    private WorldlineHostedCheckoutMobilePopulator populator;

    @Before
    public void setUp() {
        populator = new WorldlineHostedCheckoutMobilePopulator();
        populator.setWorldlinePaymentModeService(worldlinePaymentModeService());

        saleOnly = false;
        worldlineConfiguration.setDefaultOperationCode(OperationCodesEnum.FINAL_AUTHORIZATION);
    }

    @Test
    public void populateSendsApplePayPaymentProductId() {
        CreateHostedCheckoutRequest request = populateApplePayRequest();

        assertEquals(Integer.valueOf(WorldlinegopaycoreConstants.PAYMENT_METHOD_APPLEPAY),
              request.getMobilePaymentMethodSpecificInput().getPaymentProductId());
    }

    @Test
    public void populateSendsConfiguredDefaultOperationCodeAsAuthorizationMode() {
        CreateHostedCheckoutRequest request = populateApplePayRequest();

        assertEquals(OperationCodesEnum.FINAL_AUTHORIZATION.getCode(),
              request.getMobilePaymentMethodSpecificInput().getAuthorizationMode());
    }

    @Test
    public void populateForcesSaleForSaleOnlyPaymentModes() {
        saleOnly = true;

        CreateHostedCheckoutRequest request = populateApplePayRequest();

        assertEquals(OperationCodesEnum.SALE.getCode(),
              request.getMobilePaymentMethodSpecificInput().getAuthorizationMode());
    }

    @Test
    public void populateOmitsAuthorizationModeWhenNoDefaultOperationCodeIsConfigured() {
        worldlineConfiguration.setDefaultOperationCode(null);

        CreateHostedCheckoutRequest request = populateApplePayRequest();

        assertNull(request.getMobilePaymentMethodSpecificInput().getAuthorizationMode());
    }

    @Test
    public void populateIgnoresNonMobilePaymentMethods() {
        CreateHostedCheckoutRequest request = populate(
              WorldlinegopaycoreConstants.PAYMENT_METHOD_TYPE.REDIRECT.getValue(),
              WorldlinegopaycoreConstants.PAYMENT_METHOD_PAYPAL);

        assertNull(request.getMobilePaymentMethodSpecificInput());
    }

    private CreateHostedCheckoutRequest populateApplePayRequest() {
        return populate(WorldlinegopaycoreConstants.PAYMENT_METHOD_TYPE.MOBILE.getValue(),
              WorldlinegopaycoreConstants.PAYMENT_METHOD_APPLEPAY);
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

        BaseStoreModel store = new BaseStoreModel();
        store.setWorldlineConfiguration(worldlineConfiguration);

        CartModel order = new CartModel();
        order.setPaymentInfo(paymentInfo);
        order.setStore(store);
        return order;
    }

    private WorldlinePaymentModeService worldlinePaymentModeService() {
        return (WorldlinePaymentModeService) Proxy.newProxyInstance(
              WorldlinePaymentModeService.class.getClassLoader(),
              new Class[]{WorldlinePaymentModeService.class},
              (proxy, method, args) -> "isSaleOnly".equals(method.getName())
                    ? Boolean.valueOf(saleOnly)
                    : defaultValue(method.getReturnType()));
    }

    private Object defaultValue(Class<?> returnType) {
        if (Boolean.TYPE.equals(returnType)) {
            return Boolean.FALSE;
        }
        if (Integer.TYPE.equals(returnType)) {
            return 0;
        }
        return null;
    }
}
