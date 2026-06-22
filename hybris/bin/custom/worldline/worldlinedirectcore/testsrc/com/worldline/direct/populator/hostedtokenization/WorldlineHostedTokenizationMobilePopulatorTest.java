package com.worldline.direct.populator.hostedtokenization;

import com.onlinepayments.domain.CreatePaymentRequest;
import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import com.worldline.direct.model.WorldlineConfigurationModel;
import com.worldline.direct.service.WorldlineConfigurationService;
import com.worldline.direct.service.WorldlinePaymentModeService;
import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.servicelayer.session.SessionService;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@UnitTest
public class WorldlineHostedTokenizationMobilePopulatorTest {

    private static final String ENCRYPTED_PAYMENT_DATA = "{\"signature\":\"abc\"}";

    private final Map<String, Object> sessionAttributes = new HashMap<>();
    private final WorldlineConfigurationModel worldlineConfiguration = new WorldlineConfigurationModel();

    private WorldlineHostedTokenizationMobilePopulator populator;

    @Before
    public void setUp() {
        populator = new WorldlineHostedTokenizationMobilePopulator();
        populator.setSessionService(sessionService());
        populator.setWorldlineConfigurationService(worldlineConfigurationService());
        populator.setWorldlinePaymentModeService(worldlinePaymentModeService());

        sessionAttributes.clear();
        worldlineConfiguration.setEnable3DS(Boolean.TRUE);
        worldlineConfiguration.setEnableMandatory3DS(Boolean.FALSE);
    }

    @Test
    public void populateUsesGooglePayEncryptedPaymentDataFromSession() {
        sessionAttributes.put(WorldlinedirectcoreConstants.GOOGLE_PAY_ENCRYPTED_PAYMENT_DATA_SESSION_KEY, ENCRYPTED_PAYMENT_DATA);

        CreatePaymentRequest request = populateGooglePayRequest(Boolean.TRUE);

        assertEquals(ENCRYPTED_PAYMENT_DATA, request.getMobilePaymentMethodSpecificInput().getEncryptedPaymentData());
    }

    @Test
    public void populateSkipsThreeDSecureWhenServerDetectedMobileDevice() {
        sessionAttributes.put(WorldlinedirectcoreConstants.GOOGLE_PAY_MOBILE_DEVICE_SESSION_KEY, Boolean.TRUE);

        CreatePaymentRequest request = populateGooglePayRequest(Boolean.TRUE);

        assertNull(request.getMobilePaymentMethodSpecificInput()
              .getPaymentProduct320SpecificInput()
              .getThreeDSecure());
    }

    @Test
    public void populateAddsThreeDSecureWhenServerDetectedDesktopDevice() {
        sessionAttributes.put(WorldlinedirectcoreConstants.GOOGLE_PAY_MOBILE_DEVICE_SESSION_KEY, Boolean.FALSE);

        CreatePaymentRequest request = populateGooglePayRequest(Boolean.FALSE);

        assertNotNull(request.getMobilePaymentMethodSpecificInput()
              .getPaymentProduct320SpecificInput()
              .getThreeDSecure());
    }

    private CreatePaymentRequest populateGooglePayRequest(Boolean recurringToken) {
        CreatePaymentRequest request = new CreatePaymentRequest();
        populator.populate(order(recurringToken), request);
        return request;
    }

    private AbstractOrderModel order(Boolean recurringToken) {
        WorldlinePaymentInfoModel paymentInfo = new WorldlinePaymentInfoModel();
        paymentInfo.setPaymentMethod(WorldlinedirectcoreConstants.PAYMENT_METHOD_TYPE.MOBILE.getValue());
        paymentInfo.setId(WorldlinedirectcoreConstants.PAYMENT_METHOD_GOOGLEPAY);
        paymentInfo.setRecurringToken(recurringToken);

        CurrencyModel currency = new CurrencyModel();
        currency.setIsocode("EUR");

        CartModel order = new CartModel();
        order.setPaymentInfo(paymentInfo);
        order.setCurrency(currency);
        order.setTotalPrice(25d);
        return order;
    }

    private SessionService sessionService() {
        return (SessionService) Proxy.newProxyInstance(
              SessionService.class.getClassLoader(),
              new Class[]{SessionService.class},
              (proxy, method, args) -> {
                  if ("getAttribute".equals(method.getName())) {
                      return sessionAttributes.get(args[0]);
                  }
                  if ("setAttribute".equals(method.getName())) {
                      sessionAttributes.put((String) args[0], args[1]);
                      return null;
                  }
                  if ("removeAttribute".equals(method.getName())) {
                      sessionAttributes.remove(args[0]);
                      return null;
                  }
                  return defaultValue(method.getReturnType());
              });
    }

    private WorldlineConfigurationService worldlineConfigurationService() {
        return (WorldlineConfigurationService) Proxy.newProxyInstance(
              WorldlineConfigurationService.class.getClassLoader(),
              new Class[]{WorldlineConfigurationService.class},
              (proxy, method, args) -> "getCurrentWorldlineConfiguration".equals(method.getName())
                    ? worldlineConfiguration
                    : defaultValue(method.getReturnType()));
    }

    private WorldlinePaymentModeService worldlinePaymentModeService() {
        return (WorldlinePaymentModeService) Proxy.newProxyInstance(
              WorldlinePaymentModeService.class.getClassLoader(),
              new Class[]{WorldlinePaymentModeService.class},
              (proxy, method, args) -> "isSaleOnly".equals(method.getName())
                    ? Boolean.TRUE
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
