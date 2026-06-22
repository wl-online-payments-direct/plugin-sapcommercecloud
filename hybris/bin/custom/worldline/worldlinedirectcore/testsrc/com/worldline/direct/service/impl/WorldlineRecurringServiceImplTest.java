package com.worldline.direct.service.impl;

import com.onlinepayments.domain.CreatePaymentResponse;
import com.worldline.direct.enums.WorldlineRecurringPaymentStatus;
import com.worldline.direct.model.WorldlineMandateModel;
import com.worldline.direct.model.WorldlineRecurringTokenModel;
import com.worldline.direct.service.WorldlinePaymentService;
import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.servicelayer.model.ModelService;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static com.worldline.direct.constants.WorldlinedirectcoreConstants.PAYMENT_METHOD_SEPA;
import static com.worldline.direct.constants.WorldlinedirectcoreConstants.PAYMENT_METHOD_VISA;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@UnitTest
public class WorldlineRecurringServiceImplTest {
    private WorldlineRecurringServiceImpl recurringService;
    private WorldlinePaymentService worldlinePaymentService;

    @Before
    public void setUp() {
        recurringService = new WorldlineRecurringServiceImpl();
        worldlinePaymentService = mock(WorldlinePaymentService.class);
        recurringService.setWorldlinePaymentService(worldlinePaymentService);
        recurringService.setModelService(mock(ModelService.class));
    }

    @Test
    public void createRecurringPaymentUsesSubsequentPaymentForActiveCardToken() throws Exception {
        AbstractOrderModel order = mock(AbstractOrderModel.class);
        WorldlinePaymentInfoModel paymentInfo = mock(WorldlinePaymentInfoModel.class);
        WorldlineRecurringTokenModel recurringToken = mock(WorldlineRecurringTokenModel.class);
        CreatePaymentResponse expectedResponse = new CreatePaymentResponse();

        when(order.getPaymentInfo()).thenReturn(paymentInfo);
        when(paymentInfo.getId()).thenReturn(PAYMENT_METHOD_VISA);
        when(paymentInfo.getWorldlineRecurringToken()).thenReturn(recurringToken);
        when(recurringToken.getStatus()).thenReturn(WorldlineRecurringPaymentStatus.ACTIVE);
        when(worldlinePaymentService.createSubsequentPayment(order)).thenReturn(expectedResponse);

        Optional<CreatePaymentResponse> response = recurringService.createRecurringPayment(order);

        assertTrue(response.isPresent());
        assertSame(expectedResponse, response.get());
        verify(worldlinePaymentService).createSubsequentPayment(order);
        verify(worldlinePaymentService, never()).createPayment(order);
    }

    @Test
    public void createRecurringPaymentKeepsCreatePaymentForActiveSepaMandate() throws Exception {
        AbstractOrderModel order = mock(AbstractOrderModel.class);
        WorldlinePaymentInfoModel paymentInfo = mock(WorldlinePaymentInfoModel.class);
        WorldlineMandateModel mandate = mock(WorldlineMandateModel.class);
        CreatePaymentResponse expectedResponse = new CreatePaymentResponse();

        when(order.getPaymentInfo()).thenReturn(paymentInfo);
        when(paymentInfo.getId()).thenReturn(PAYMENT_METHOD_SEPA);
        when(paymentInfo.getMandateDetail()).thenReturn(mandate);
        when(mandate.getStatus()).thenReturn(WorldlineRecurringPaymentStatus.ACTIVE);
        when(worldlinePaymentService.getMandate(mandate)).thenReturn(null);
        when(worldlinePaymentService.createPayment(order)).thenReturn(expectedResponse);

        Optional<CreatePaymentResponse> response = recurringService.createRecurringPayment(order);

        assertTrue(response.isPresent());
        assertSame(expectedResponse, response.get());
        verify(worldlinePaymentService).createPayment(order);
        verify(worldlinePaymentService, never()).createSubsequentPayment(order);
    }
}
