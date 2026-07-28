package com.worldline.direct.facade.impl;

import com.onlinepayments.domain.CardPaymentMethodSpecificOutput;
import com.onlinepayments.domain.AmountOfMoney;
import com.onlinepayments.domain.CardEssentials;
import com.onlinepayments.domain.CardFraudResults;
import com.onlinepayments.domain.MobilePaymentData;
import com.onlinepayments.domain.MobilePaymentMethodSpecificOutput;
import com.onlinepayments.domain.PaymentOutput;
import com.onlinepayments.domain.PaymentProduct;
import com.onlinepayments.domain.PaymentReferences;
import com.onlinepayments.domain.PaymentResponse;
import com.onlinepayments.domain.PaymentStatusOutput;
import com.onlinepayments.domain.RedirectPaymentMethodSpecificOutput;
import com.onlinepayments.domain.ThreeDSecureResults;
import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import com.worldline.direct.enums.WorldlineCheckoutTypesEnum;
import com.worldline.direct.exception.WorldlineNonValidPaymentProductException;
import com.worldline.direct.enums.WorldlineRecurringPaymentStatus;
import com.worldline.direct.model.WorldlineRecurringTokenModel;
import com.worldline.direct.order.data.WorldlinePaymentInfoData;
import com.worldline.direct.service.WorldlinePaymentService;
import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.commerceservices.strategies.CheckoutCustomerStrategy;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.store.BaseStoreModel;
import de.hybris.platform.store.services.BaseStoreService;
import org.junit.Test;

import java.lang.reflect.Proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

@UnitTest
public class WorldlineCheckoutFacadeImplPaymentOutputTest {

    private final TestableWorldlineCheckoutFacadeImpl facade = new TestableWorldlineCheckoutFacadeImpl();

    @Test
    public void getPaymentProductIdUsesMobileOutputWhenCardOutputDoesNotIdentifyProduct() {
        PaymentOutput paymentOutput = new PaymentOutput();
        paymentOutput.setCardPaymentMethodSpecificOutput(new CardPaymentMethodSpecificOutput());
        paymentOutput.setMobilePaymentMethodSpecificOutput(mobilePaymentMethodSpecificOutput(WorldlinedirectcoreConstants.PAYMENT_METHOD_GOOGLEPAY));

        Integer paymentProductId = facade.getPaymentProductId(paymentOutput);

        assertEquals(Integer.valueOf(WorldlinedirectcoreConstants.PAYMENT_METHOD_GOOGLEPAY), paymentProductId);
    }

    @Test
    public void getPaymentProductIdUsesCardOutputWhenOnlyCardOutputIdentifiesProduct() {
        PaymentOutput paymentOutput = new PaymentOutput();
        paymentOutput.setCardPaymentMethodSpecificOutput(cardPaymentMethodSpecificOutput(WorldlinedirectcoreConstants.PAYMENT_METHOD_VISA));

        Integer paymentProductId = facade.getPaymentProductId(paymentOutput);

        assertEquals(Integer.valueOf(WorldlinedirectcoreConstants.PAYMENT_METHOD_VISA), paymentProductId);
    }

    @Test
    public void getPaymentProductIdUsesRedirectOutputWhenRedirectOutputIdentifiesProduct() {
        PaymentOutput paymentOutput = new PaymentOutput();
        RedirectPaymentMethodSpecificOutput redirectOutput = new RedirectPaymentMethodSpecificOutput();
        redirectOutput.setPaymentProductId(840);
        paymentOutput.setRedirectPaymentMethodSpecificOutput(redirectOutput);

        Integer paymentProductId = facade.getPaymentProductId(paymentOutput);

        assertEquals(Integer.valueOf(840), paymentProductId);
    }

    @Test
    public void savePaymentTokenAssignsCheckoutCustomerToGooglePayRecurringToken() {
        CustomerModel customer = new CustomerModel();
        CartModel order = new CartModel();
        WorldlinePaymentInfoModel paymentInfo = new WorldlinePaymentInfoModel();
        WorldlineRecurringTokenModel recurringToken = new WorldlineRecurringTokenModel();
        BaseStoreModel baseStore = new BaseStoreModel();
        baseStore.setUid("store-id");
        TestModelService modelService = new TestModelService(recurringToken);
        order.setPaymentInfo(paymentInfo);

        facade.setModelService(modelService.proxy());
        facade.setBaseStoreService(baseStoreService(baseStore));
        facade.setCheckoutCustomerStrategy(checkoutCustomerStrategy(customer));

        facade.savePaymentToken(order, googlePayPaymentResponse(), Boolean.TRUE, "cronjob-id");

        assertEquals("payment-id", recurringToken.getInitialPaymentId());
        assertEquals("cronjob-id", recurringToken.getSubscriptionID());
        assertEquals(WorldlineRecurringPaymentStatus.ACTIVE, recurringToken.getStatus());
        assertEquals("store-id", recurringToken.getStoreId());
        assertEquals("dpan", recurringToken.getAlias());
        assertEquals("1228", recurringToken.getExpiryDate());
        assertSame(customer, recurringToken.getCustomer());
        assertSame(recurringToken, paymentInfo.getWorldlineRecurringToken());
        assertSame(paymentInfo, modelService.savedModels[0]);
        assertSame(recurringToken, modelService.savedModels[1]);
        assertSame(order, modelService.refreshedModel);
    }

    @Test
    public void fillWorldlinePaymentInfoDataAllowsPayByLinkDuringAssistedService() throws WorldlineNonValidPaymentProductException {
        facade.setSessionService(sessionService(Boolean.TRUE));

        WorldlinePaymentInfoData paymentInfoData = new WorldlinePaymentInfoData();
        facade.fillWorldlinePaymentInfoData(paymentInfoData, null, WorldlinedirectcoreConstants.PAYMENT_METHOD_PAY_BY_LINK, null);

        assertEquals(Integer.valueOf(WorldlinedirectcoreConstants.PAYMENT_METHOD_PAY_BY_LINK), paymentInfoData.getId());
        assertEquals(WorldlineCheckoutTypesEnum.PAY_BY_LINK, paymentInfoData.getWorldlineCheckoutType());
    }

    @Test(expected = WorldlineNonValidPaymentProductException.class)
    public void fillWorldlinePaymentInfoDataRejectsPayByLinkOutsideAssistedService() throws WorldlineNonValidPaymentProductException {
        facade.setSessionService(sessionService(Boolean.FALSE));

        facade.fillWorldlinePaymentInfoData(new WorldlinePaymentInfoData(), null, WorldlinedirectcoreConstants.PAYMENT_METHOD_PAY_BY_LINK, null);
    }

    @Test
    public void updatePaymentInfoStoresCardPaymentDetailsReturnedByWorldline() {
        CartModel order = new CartModel();
        WorldlinePaymentInfoModel paymentInfo = new WorldlinePaymentInfoModel();
        paymentInfo.setId(WorldlinedirectcoreConstants.PAYMENT_METHOD_VISA);
        TestModelService modelService = new TestModelService(new WorldlineRecurringTokenModel());
        order.setPaymentInfo(paymentInfo);
        facade.setModelService(modelService.proxy());

        facade.updatePaymentInfo(order, cardPaymentResponse());

        assertEquals("3265460443", paymentInfo.getWorldlinePaymentId());
        assertEquals("PENDING_CAPTURE", paymentInfo.getWorldlineStatus());
        assertEquals(Integer.valueOf(5), paymentInfo.getWorldlineStatusCode());
        assertEquals("merchant-ref", paymentInfo.getWorldlineMerchantReference());
        assertEquals("card", paymentInfo.getWorldlinePaymentMethod());
        assertEquals(Integer.valueOf(WorldlinedirectcoreConstants.PAYMENT_METHOD_VISA), paymentInfo.getWorldlinePaymentProductId());
        assertEquals(Long.valueOf(12345L), paymentInfo.getWorldlineAmount());
        assertEquals("EUR", paymentInfo.getWorldlineCurrency());
        assertEquals(Long.valueOf(12000L), paymentInfo.getWorldlineAcquiredAmount());
        assertEquals("EUR", paymentInfo.getWorldlineAcquiredCurrency());
        assertEquals("411111", paymentInfo.getCardBin());
        assertEquals("1111", paymentInfo.getCardLastFour());
        assertEquals("accepted", paymentInfo.getFraudResult());
        assertEquals("issuer", paymentInfo.getLiability());
        assertEquals("low-value", paymentInfo.getAppliedExemption());
        assertEquals("Y", paymentInfo.getAuthenticationStatus());
    }

    @Test
    public void savePaymentTokenUsesGooglePayRecurringReferenceWhen3dsResponseAlsoContainsCardOutputWithoutToken() {
        CustomerModel customer = new CustomerModel();
        CartModel order = new CartModel();
        WorldlinePaymentInfoModel paymentInfo = new WorldlinePaymentInfoModel();
        WorldlineRecurringTokenModel recurringToken = new WorldlineRecurringTokenModel();
        BaseStoreModel baseStore = new BaseStoreModel();
        baseStore.setUid("store-id");
        TestModelService modelService = new TestModelService(recurringToken);
        order.setPaymentInfo(paymentInfo);

        facade.setModelService(modelService.proxy());
        facade.setBaseStoreService(baseStoreService(baseStore));
        facade.setCheckoutCustomerStrategy(checkoutCustomerStrategy(customer));
        facade.setWorldlinePaymentService(paymentServiceThatFailsOnGetToken());

        facade.savePaymentToken(order, googlePayPaymentResponseWithCardOutputWithoutToken(), Boolean.TRUE, "cronjob-id");

        assertEquals("payment-id", recurringToken.getInitialPaymentId());
        assertSame(recurringToken, paymentInfo.getWorldlineRecurringToken());
        assertSame(order, modelService.refreshedModel);
    }

    private CardPaymentMethodSpecificOutput cardPaymentMethodSpecificOutput(Integer paymentProductId) {
        CardPaymentMethodSpecificOutput output = new CardPaymentMethodSpecificOutput();
        output.setPaymentProductId(paymentProductId);
        return output;
    }

    private MobilePaymentMethodSpecificOutput mobilePaymentMethodSpecificOutput(Integer paymentProductId) {
        MobilePaymentMethodSpecificOutput output = new MobilePaymentMethodSpecificOutput();
        output.setPaymentProductId(paymentProductId);
        return output;
    }

    private PaymentResponse googlePayPaymentResponse() {
        MobilePaymentData paymentData = new MobilePaymentData();
        paymentData.setDpan("dpan");
        paymentData.setExpiryDate("1228");

        MobilePaymentMethodSpecificOutput mobileOutput = mobilePaymentMethodSpecificOutput(WorldlinedirectcoreConstants.PAYMENT_METHOD_GOOGLEPAY);
        mobileOutput.setPaymentData(paymentData);

        PaymentOutput paymentOutput = new PaymentOutput();
        paymentOutput.setMobilePaymentMethodSpecificOutput(mobileOutput);

        PaymentResponse paymentResponse = new PaymentResponse();
        paymentResponse.setId("payment-id");
        paymentResponse.setPaymentOutput(paymentOutput);
        return paymentResponse;
    }

    private PaymentResponse cardPaymentResponse() {
        AmountOfMoney amount = new AmountOfMoney();
        amount.setAmount(12345L);
        amount.setCurrencyCode("EUR");

        AmountOfMoney acquiredAmount = new AmountOfMoney();
        acquiredAmount.setAmount(12000L);
        acquiredAmount.setCurrencyCode("EUR");

        PaymentReferences references = new PaymentReferences();
        references.setMerchantReference("merchant-ref");

        CardEssentials card = new CardEssentials();
        card.setBin("411111");
        card.setCardNumber("4111111111111111");

        CardFraudResults fraudResults = new CardFraudResults();
        fraudResults.setFraudServiceResult("accepted");

        ThreeDSecureResults threeDSecureResults = new ThreeDSecureResults();
        threeDSecureResults.setLiability("issuer");
        threeDSecureResults.setAppliedExemption("low-value");
        threeDSecureResults.setAuthenticationStatus("Y");

        CardPaymentMethodSpecificOutput cardOutput = cardPaymentMethodSpecificOutput(WorldlinedirectcoreConstants.PAYMENT_METHOD_VISA);
        cardOutput.setCard(card);
        cardOutput.setFraudResults(fraudResults);
        cardOutput.setThreeDSecureResults(threeDSecureResults);

        PaymentOutput paymentOutput = new PaymentOutput();
        paymentOutput.setAmountOfMoney(amount);
        paymentOutput.setAcquiredAmount(acquiredAmount);
        paymentOutput.setReferences(references);
        paymentOutput.setPaymentMethod("card");
        paymentOutput.setCardPaymentMethodSpecificOutput(cardOutput);

        PaymentStatusOutput statusOutput = new PaymentStatusOutput();
        statusOutput.setStatusCode(5);

        PaymentResponse paymentResponse = new PaymentResponse();
        paymentResponse.setId("3265460443");
        paymentResponse.setStatus("PENDING_CAPTURE");
        paymentResponse.setStatusOutput(statusOutput);
        paymentResponse.setPaymentOutput(paymentOutput);
        return paymentResponse;
    }

    private PaymentResponse googlePayPaymentResponseWithCardOutputWithoutToken() {
        PaymentResponse paymentResponse = googlePayPaymentResponse();
        paymentResponse.getPaymentOutput().setCardPaymentMethodSpecificOutput(
              cardPaymentMethodSpecificOutput(WorldlinedirectcoreConstants.PAYMENT_METHOD_VISA));
        return paymentResponse;
    }

    private WorldlinePaymentService paymentServiceThatFailsOnGetToken() {
        return (WorldlinePaymentService) Proxy.newProxyInstance(
              WorldlinePaymentService.class.getClassLoader(),
              new Class[]{WorldlinePaymentService.class},
              (proxy, method, args) -> {
                  if ("getToken".equals(method.getName())) {
                      fail("Google Pay recurring token saving must not call getToken for card output without token");
                  }
                  return defaultValue(method.getReturnType());
              });
    }

    private static class TestableWorldlineCheckoutFacadeImpl extends WorldlineCheckoutFacadeImpl {
        Integer getPaymentProductId(PaymentOutput paymentOutput) {
            return getPaymentProductIdFromPaymentOutput(paymentOutput);
        }

        @Override
        public PaymentProduct getPaymentMethodById(int paymentId) {
            if (paymentId == WorldlinedirectcoreConstants.PAYMENT_METHOD_PAY_BY_LINK) {
                return super.getPaymentMethodById(paymentId);
            }
            PaymentProduct paymentProduct = new PaymentProduct();
            paymentProduct.setId(paymentId);
            paymentProduct.setPaymentMethod(WorldlinedirectcoreConstants.PAYMENT_METHOD_TYPE.CARD.getValue());
            return paymentProduct;
        }

        void updatePaymentInfo(AbstractOrderModel orderModel, PaymentResponse paymentResponse) {
            updatePaymentInfoIfNeeded(orderModel, paymentResponse);
        }
    }

    private BaseStoreService baseStoreService(BaseStoreModel baseStore) {
        return (BaseStoreService) Proxy.newProxyInstance(
              BaseStoreService.class.getClassLoader(),
              new Class[]{BaseStoreService.class},
              (proxy, method, args) -> "getCurrentBaseStore".equals(method.getName()) ? baseStore : defaultValue(method.getReturnType()));
    }

    private CheckoutCustomerStrategy checkoutCustomerStrategy(CustomerModel customer) {
        return (CheckoutCustomerStrategy) Proxy.newProxyInstance(
              CheckoutCustomerStrategy.class.getClassLoader(),
              new Class[]{CheckoutCustomerStrategy.class},
                  (proxy, method, args) -> "getCurrentUserForCheckout".equals(method.getName()) ? customer : defaultValue(method.getReturnType()));
    }

    private SessionService sessionService(Boolean assistedServiceSession) {
        return (SessionService) Proxy.newProxyInstance(
              SessionService.class.getClassLoader(),
              new Class[]{SessionService.class},
              (proxy, method, args) -> {
                  if ("getAttribute".equals(method.getName()) && "ASM".equals(args[0])) {
                      return assistedServiceSession ? new Object() : null;
                  }
                  return defaultValue(method.getReturnType());
              });
    }

    private static class TestModelService {
        private final WorldlineRecurringTokenModel recurringToken;
        private Object[] savedModels;
        private Object refreshedModel;

        TestModelService(WorldlineRecurringTokenModel recurringToken) {
            this.recurringToken = recurringToken;
        }

        ModelService proxy() {
            return (ModelService) Proxy.newProxyInstance(
                  ModelService.class.getClassLoader(),
                  new Class[]{ModelService.class},
                  (proxy, method, args) -> {
                      if ("create".equals(method.getName())) {
                          return recurringToken;
                      }
                      if ("saveAll".equals(method.getName())) {
                          savedModels = (Object[]) args[0];
                          return null;
                      }
                      if ("refresh".equals(method.getName())) {
                          refreshedModel = args[0];
                          return null;
                      }
                      return defaultValue(method.getReturnType());
                  });
        }
    }

    private static Object defaultValue(Class<?> returnType) {
        if (Boolean.TYPE.equals(returnType)) {
            return Boolean.FALSE;
        }
        if (Integer.TYPE.equals(returnType)) {
            return 0;
        }
        return null;
    }
}
