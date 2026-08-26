package com.worldline.gopay.service.impl;

import com.onlinepayments.domain.AmountOfMoney;
import com.onlinepayments.domain.PaymentLinkOrderOutput;
import com.onlinepayments.domain.PaymentLinkResponse;
import com.onlinepayments.domain.PaymentOutput;
import com.onlinepayments.domain.PaymentReferences;
import com.onlinepayments.domain.PaymentResponse;
import com.onlinepayments.domain.WebhooksEvent;
import com.worldline.gopay.constants.WorldlinegopaycoreConstants;
import com.worldline.gopay.dao.WorldlineOrderDao;
import com.worldline.gopay.dao.WorldlineTransactionDao;
import com.worldline.gopay.service.WorldlineBusinessProcessService;
import com.worldline.gopay.util.WorldlineAmountUtils;
import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.enums.PaymentStatus;
import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.order.exceptions.CalculationException;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.payment.model.PaymentTransactionModel;
import de.hybris.platform.servicelayer.exceptions.ModelNotFoundException;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;
import de.hybris.platform.servicelayer.model.ModelService;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

@UnitTest
public class WorldlineTransactionServiceImplTest {

    @Test
    public void getPaymentIdKeepsShortWorldlinePaymentIds() throws Exception {
        assertEquals("12345678901", getPaymentId("12345678901"));
    }

    @Test
    public void getPaymentIdKeepsSuffixBeforeCaptureSeparator() throws Exception {
        assertEquals("1234567890", getPaymentId("1234567890_1"));
    }

    @Test
    public void getPaymentIdPreservesLegacyLongIdStripping() throws Exception {
        assertEquals("1234567890", getPaymentId("0000001234567890"));
    }

    @Test
    public void processPaymentLinkEventCreatesAuthorizationAndResumesOrderWhenPaymentIsPresent() {
        AbstractOrderModel order = worldlineOrder();
        PaymentTransactionModel transaction = new PaymentTransactionModel();
        transaction.setEntries(new ArrayList<>());
        TestModelService modelService = new TestModelService(transaction);
        TestWorldlineBusinessProcessService businessProcessService = new TestWorldlineBusinessProcessService();

        WorldlineTransactionServiceImpl service = service(order, modelService, businessProcessService);

        service.processPaymentLinkEvent(paymentLinkEvent(
                WorldlinegopaycoreConstants.WEBHOOK_TYPE_ENUM.PAYMENT_LINK_PAID.getValue(),
                payment("0000001234567890", "CAPTURED")));

        assertEquals(PaymentStatus.WORLDLINE_AUTHORIZED, order.getPaymentStatus());
        assertEquals("pl-1", ((WorldlinePaymentInfoModel) order.getPaymentInfo()).getPaymentLinkId());
        assertEquals("0000001234567890", ((WorldlinePaymentInfoModel) order.getPaymentInfo()).getPaymentLinkPaymentId());
        assertSame(order, businessProcessService.order);
        assertEquals(WorldlinegopaycoreConstants.WORLDLINE_EVENT_PAYMENT, businessProcessService.event);
    }

    @Test
    public void processPaymentLinkEventCancelsAndResumesWaitingOrderWhenUnpaidLinkExpires() {
        AbstractOrderModel order = worldlineOrder();
        TestModelService modelService = new TestModelService(new PaymentTransactionModel());
        TestWorldlineBusinessProcessService businessProcessService = new TestWorldlineBusinessProcessService();

        WorldlineTransactionServiceImpl service = service(order, modelService, businessProcessService);

        service.processPaymentLinkEvent(paymentLinkEvent(
                WorldlinegopaycoreConstants.WEBHOOK_TYPE_ENUM.PAYMENT_LINK_EXPIRED.getValue(),
                null));

        assertEquals(PaymentStatus.WORLDLINE_CANCELED, order.getPaymentStatus());
        assertSame(order, businessProcessService.order);
        assertEquals(WorldlinegopaycoreConstants.WORLDLINE_EVENT_PAYMENT, businessProcessService.event);
    }

    private String getPaymentId(String rawPaymentTransactionId) throws Exception {
        Method method = WorldlineTransactionServiceImpl.class.getDeclaredMethod("getPaymentId", String.class);
        method.setAccessible(true);
        return (String) method.invoke(new WorldlineTransactionServiceImpl(), rawPaymentTransactionId);
    }

    private WorldlineTransactionServiceImpl service(AbstractOrderModel order, TestModelService modelService,
                                                    TestWorldlineBusinessProcessService businessProcessService) {
        WorldlineTransactionServiceImpl service = new WorldlineTransactionServiceImpl();
        service.setWorldlineOrderDao(worldlineOrderDao(order));
        service.setWorldlineTransactionDao(worldlineTransactionDao());
        service.setModelService(modelService.proxy());
        service.setWorldlineAmountUtils(worldlineAmountUtils());
        service.setWorldlineBusinessProcessService(businessProcessService);
        return service;
    }

    private AbstractOrderModel worldlineOrder() {
        CurrencyModel currency = new CurrencyModel();
        currency.setIsocode("EUR");
        currency.setDigits(2);

        AbstractOrderModel order = new OrderModel();
        order.setCode("ORDER-100");
        order.setCurrency(currency);
        order.setTotalPrice(12.34D);
        order.setPaymentInfo(new WorldlinePaymentInfoModel());
        order.setPaymentStatus(PaymentStatus.WORLDLINE_WAITING_AUTH);
        return order;
    }

    private WebhooksEvent paymentLinkEvent(String type, PaymentResponse payment) {
        PaymentLinkOrderOutput paymentLinkOrder = new PaymentLinkOrderOutput();
        paymentLinkOrder.setMerchantReference("ORDER-100");

        PaymentLinkResponse paymentLink = new PaymentLinkResponse();
        paymentLink.setPaymentLinkId("pl-1");
        paymentLink.setPaymentLinkOrder(paymentLinkOrder);
        paymentLink.setPaymentId(payment != null ? payment.getId() : null);
        paymentLink.setStatus("PAYMENT_ATTEMPTED");

        WebhooksEvent webhooksEvent = new WebhooksEvent();
        webhooksEvent.setId("event-1");
        webhooksEvent.setType(type);
        webhooksEvent.setPaymentLink(paymentLink);
        webhooksEvent.setPayment(payment);
        return webhooksEvent;
    }

    private PaymentResponse payment(String id, String status) {
        AmountOfMoney amountOfMoney = new AmountOfMoney();
        amountOfMoney.setAmount(1234L);
        amountOfMoney.setCurrencyCode("EUR");

        PaymentReferences references = new PaymentReferences();
        references.setMerchantReference("ORDER-100");

        PaymentOutput paymentOutput = new PaymentOutput();
        paymentOutput.setAmountOfMoney(amountOfMoney);
        paymentOutput.setReferences(references);

        PaymentResponse payment = new PaymentResponse();
        payment.setId(id);
        payment.setStatus(status);
        payment.setPaymentOutput(paymentOutput);
        return payment;
    }

    private WorldlineOrderDao worldlineOrderDao(AbstractOrderModel order) {
        return (WorldlineOrderDao) Proxy.newProxyInstance(
                WorldlineOrderDao.class.getClassLoader(),
                new Class[]{WorldlineOrderDao.class},
                (proxy, method, args) -> "findWorldlineOrder".equals(method.getName()) ? order : defaultValue(method.getReturnType()));
    }

    private WorldlineTransactionDao worldlineTransactionDao() {
        return (WorldlineTransactionDao) Proxy.newProxyInstance(
                WorldlineTransactionDao.class.getClassLoader(),
                new Class[]{WorldlineTransactionDao.class},
                (proxy, method, args) -> {
                    if ("findPaymentTransaction".equals(method.getName())) {
                        throw new ModelNotFoundException("not found");
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private WorldlineAmountUtils worldlineAmountUtils() {
        WorldlineAmountUtils amountUtils = new WorldlineAmountUtils();
        amountUtils.setCommonI18NService((CommonI18NService) Proxy.newProxyInstance(
                CommonI18NService.class.getClassLoader(),
                new Class[]{CommonI18NService.class},
                (proxy, method, args) -> {
                    if ("getCurrency".equals(method.getName())) {
                        CurrencyModel currency = new CurrencyModel();
                        currency.setIsocode((String) args[0]);
                        currency.setDigits(2);
                        return currency;
                    }
                    return defaultValue(method.getReturnType());
                }));
        return amountUtils;
    }

    private static class TestWorldlineBusinessProcessService implements WorldlineBusinessProcessService {
        private AbstractOrderModel order;
        private String event;

        @Override
        public void triggerOrderProcessEvent(AbstractOrderModel orderModel, String event) {
            this.order = orderModel;
            this.event = event;
        }

        @Override
        public void triggerReturnProcessEvent(de.hybris.platform.core.model.order.OrderModel orderModel, String event) {
        }
    }

    private static class TestModelService {
        private final PaymentTransactionModel transaction;

        TestModelService(PaymentTransactionModel transaction) {
            this.transaction = transaction;
        }

        ModelService proxy() {
            return (ModelService) Proxy.newProxyInstance(
                    ModelService.class.getClassLoader(),
                    new Class[]{ModelService.class},
                    (proxy, method, args) -> {
                        if ("create".equals(method.getName()) && PaymentTransactionModel.class.equals(args[0])) {
                            return transaction;
                        }
                        if ("create".equals(method.getName()) && PaymentTransactionEntryModel.class.equals(args[0])) {
                            return new PaymentTransactionEntryModel();
                        }
                        return defaultValue(method.getReturnType());
                    });
        }
    }

    private static Object defaultValue(Class<?> returnType) throws CalculationException {
        if (Boolean.TYPE.equals(returnType)) {
            return Boolean.FALSE;
        }
        if (Integer.TYPE.equals(returnType)) {
            return 0;
        }
        if (BigDecimal.class.equals(returnType)) {
            return BigDecimal.ZERO;
        }
        return null;
    }
}
