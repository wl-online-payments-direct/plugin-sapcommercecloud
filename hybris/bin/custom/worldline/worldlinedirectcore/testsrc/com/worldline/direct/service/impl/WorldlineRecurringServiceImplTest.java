package com.worldline.direct.service.impl;

import com.onlinepayments.domain.CreatePaymentResponse;
import com.onlinepayments.domain.GetMandateResponse;
import com.onlinepayments.domain.MandateResponse;
import com.worldline.direct.enums.WorldlineRecurringPaymentStatus;
import com.worldline.direct.enums.WorldlineRecurringType;
import com.worldline.direct.model.WorldlineMandateModel;
import com.worldline.direct.model.WorldlineRecurringTokenModel;
import com.worldline.direct.service.WorldlinePaymentService;
import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.servicelayer.model.ModelService;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.Optional;

import static com.worldline.direct.constants.WorldlinedirectcoreConstants.PAYMENT_METHOD_SEPA;
import static com.worldline.direct.constants.WorldlinedirectcoreConstants.PAYMENT_METHOD_VISA;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@UnitTest
public class WorldlineRecurringServiceImplTest {
    private WorldlineRecurringServiceImpl recurringService;
    private FakeWorldlinePaymentService worldlinePaymentService;

    @Before
    public void setUp() {
        recurringService = new WorldlineRecurringServiceImpl();
        worldlinePaymentService = new FakeWorldlinePaymentService();
        recurringService.setWorldlinePaymentService(worldlinePaymentService.proxy());
        recurringService.setModelService(modelService());
    }

    @Test
    public void createRecurringPaymentUsesSubsequentPaymentForActiveCardToken() throws Exception {
        WorldlineRecurringTokenModel recurringToken = new WorldlineRecurringTokenModel();
        recurringToken.setStatus(WorldlineRecurringPaymentStatus.ACTIVE);

        WorldlinePaymentInfoModel paymentInfo = new WorldlinePaymentInfoModel();
        paymentInfo.setId(PAYMENT_METHOD_VISA);
        paymentInfo.setWorldlineRecurringToken(recurringToken);

        AbstractOrderModel order = order(paymentInfo);
        CreatePaymentResponse expectedResponse = new CreatePaymentResponse();
        worldlinePaymentService.createSubsequentPaymentResponse = expectedResponse;

        Optional<CreatePaymentResponse> response = recurringService.createRecurringPayment(order);

        assertTrue(response.isPresent());
        assertSame(expectedResponse, response.get());
        assertSame(order, worldlinePaymentService.createSubsequentPaymentOrder);
        assertSame(null, worldlinePaymentService.createPaymentOrder);
    }

    @Test
    public void createRecurringPaymentKeepsCreatePaymentForActiveSepaMandate() throws Exception {
        WorldlineMandateModel mandate = mandate(WorldlineRecurringPaymentStatus.ACTIVE, WorldlineRecurringType.RECURRING);
        WorldlinePaymentInfoModel paymentInfo = sepaPaymentInfo(mandate);
        AbstractOrderModel order = order(paymentInfo);
        CreatePaymentResponse expectedResponse = new CreatePaymentResponse();
        worldlinePaymentService.createPaymentResponse = expectedResponse;

        Optional<CreatePaymentResponse> response = recurringService.createRecurringPayment(order);

        assertTrue(response.isPresent());
        assertSame(expectedResponse, response.get());
        assertSame(order, worldlinePaymentService.createPaymentOrder);
        assertSame(null, worldlinePaymentService.createSubsequentPaymentOrder);
    }

    @Test
    public void createRecurringPaymentDoesNotChargeUniqueSepaMandate() throws Exception {
        WorldlineMandateModel mandate = mandate(WorldlineRecurringPaymentStatus.ACTIVE, WorldlineRecurringType.UNIQUE);
        AbstractOrderModel order = order(sepaPaymentInfo(mandate));

        Optional<CreatePaymentResponse> response = recurringService.createRecurringPayment(order);

        assertTrue(response.isEmpty());
        assertSame(null, worldlinePaymentService.createPaymentOrder);
        assertSame(null, worldlinePaymentService.createSubsequentPaymentOrder);
    }

    @Test
    public void updateMandateRefreshesRecurrenceTypeFromWorldline() {
        WorldlineMandateModel mandate = new WorldlineMandateModel();
        GetMandateResponse response = new GetMandateResponse();
        MandateResponse mandateResponse = new MandateResponse();
        mandateResponse.setStatus("ACTIVE");
        mandateResponse.setRecurrenceType("RECURRING");
        response.setMandate(mandateResponse);
        worldlinePaymentService.getMandateResponse = response;

        recurringService.updateMandate(mandate);

        assertSame(WorldlineRecurringPaymentStatus.ACTIVE, mandate.getStatus());
        assertSame(WorldlineRecurringType.RECURRING, mandate.getRecurrenceType());
    }

    private AbstractOrderModel order(WorldlinePaymentInfoModel paymentInfo) {
        CartModel order = new CartModel();
        order.setPaymentInfo(paymentInfo);
        return order;
    }

    private WorldlinePaymentInfoModel sepaPaymentInfo(WorldlineMandateModel mandate) {
        WorldlinePaymentInfoModel paymentInfo = new WorldlinePaymentInfoModel();
        paymentInfo.setId(PAYMENT_METHOD_SEPA);
        paymentInfo.setMandateDetail(mandate);
        return paymentInfo;
    }

    private WorldlineMandateModel mandate(WorldlineRecurringPaymentStatus status, WorldlineRecurringType recurrenceType) {
        WorldlineMandateModel mandate = new WorldlineMandateModel();
        mandate.setStatus(status);
        mandate.setRecurrenceType(recurrenceType);
        return mandate;
    }

    private ModelService modelService() {
        return (ModelService) Proxy.newProxyInstance(
              ModelService.class.getClassLoader(),
              new Class[]{ModelService.class},
              (proxy, method, args) -> null);
    }

    private static class FakeWorldlinePaymentService {
        private CreatePaymentResponse createPaymentResponse;
        private CreatePaymentResponse createSubsequentPaymentResponse;
        private GetMandateResponse getMandateResponse;
        private AbstractOrderModel createPaymentOrder;
        private AbstractOrderModel createSubsequentPaymentOrder;

        private WorldlinePaymentService proxy() {
            return (WorldlinePaymentService) Proxy.newProxyInstance(
                  WorldlinePaymentService.class.getClassLoader(),
                  new Class[]{WorldlinePaymentService.class},
                  (proxy, method, args) -> {
                      switch (method.getName()) {
                          case "createPayment":
                              createPaymentOrder = (AbstractOrderModel) args[0];
                              return createPaymentResponse;
                          case "createSubsequentPayment":
                              createSubsequentPaymentOrder = (AbstractOrderModel) args[0];
                              return createSubsequentPaymentResponse;
                          case "getMandate":
                              return getMandateResponse;
                          default:
                              return null;
                      }
                  });
        }
    }
}
