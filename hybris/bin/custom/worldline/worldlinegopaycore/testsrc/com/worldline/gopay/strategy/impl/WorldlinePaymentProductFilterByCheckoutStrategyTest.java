package com.worldline.gopay.strategy.impl;

import com.onlinepayments.domain.PaymentProduct;
import com.worldline.gopay.constants.WorldlinegopaycoreConstants;
import com.worldline.gopay.enums.WorldlineCheckoutTypesEnum;
import com.worldline.gopay.evaluate.WorldlinePaymentProductEvaluator;
import com.worldline.gopay.evaluate.impl.DefaultWorldlinePaymentProductEvaluator;
import com.worldline.gopay.evaluate.impl.WorldlineHostedCheckoutPaymentProductsEvaluator;
import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.store.BaseStoreModel;
import de.hybris.platform.store.services.BaseStoreService;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@UnitTest
public class WorldlinePaymentProductFilterByCheckoutStrategyTest {

    @Test
    public void hostedCheckoutKeepsGooglePayWhenReturnedByWorldline() {
        WorldlinePaymentProductFilterByCheckoutStrategy strategy = new WorldlinePaymentProductFilterByCheckoutStrategy();
        strategy.setBaseStoreService(baseStoreService(WorldlineCheckoutTypesEnum.HOSTED_CHECKOUT));
        strategy.setWorldlineHostedCheckoutPaymentProductsEvaluatorList(Arrays.asList(
              new WorldlineHostedCheckoutPaymentProductsEvaluator(),
              evaluatorFor(WorldlinegopaycoreConstants.PAYMENT_METHOD_APPLEPAY),
              evaluatorFor(WorldlinegopaycoreConstants.PAYMENT_METHOD_GOOGLEPAY)));
        strategy.setWorldlineHostedTokenizationPaymentProductsEvaluatorList(Arrays.asList());

        List<PaymentProduct> filteredProducts = strategy.filter(Arrays.asList(
              paymentProduct(WorldlinegopaycoreConstants.PAYMENT_METHOD_GOOGLEPAY, WorldlinegopaycoreConstants.PAYMENT_METHOD_TYPE.MOBILE.getValue()),
              paymentProduct(3203, WorldlinegopaycoreConstants.PAYMENT_METHOD_TYPE.REDIRECT.getValue())));

        List<Integer> filteredProductIds = filteredProducts.stream().map(PaymentProduct::getId).collect(Collectors.toList());
        assertTrue(filteredProductIds.contains(WorldlinegopaycoreConstants.PAYMENT_METHOD_GOOGLEPAY));
    }

    @Test
    public void payByLinkStoreConfigurationDoesNotActAsHostedCheckoutMode() {
        WorldlinePaymentProductFilterByCheckoutStrategy strategy = new WorldlinePaymentProductFilterByCheckoutStrategy();
        strategy.setBaseStoreService(baseStoreService(WorldlineCheckoutTypesEnum.PAY_BY_LINK));
        strategy.setWorldlineHostedCheckoutPaymentProductsEvaluatorList(Arrays.asList(
              new WorldlineHostedCheckoutPaymentProductsEvaluator(),
              evaluatorFor(WorldlinegopaycoreConstants.PAYMENT_METHOD_GOOGLEPAY)));
        strategy.setWorldlineHostedTokenizationPaymentProductsEvaluatorList(Arrays.asList());

        List<PaymentProduct> filteredProducts = strategy.filter(Arrays.asList(
              paymentProduct(WorldlinegopaycoreConstants.PAYMENT_METHOD_GOOGLEPAY, WorldlinegopaycoreConstants.PAYMENT_METHOD_TYPE.MOBILE.getValue()),
              paymentProduct(9999, "unsupported")));

        List<Integer> filteredProductIds = filteredProducts.stream().map(PaymentProduct::getId).collect(Collectors.toList());
        assertFalse(filteredProductIds.contains(WorldlinegopaycoreConstants.PAYMENT_METHOD_HCP));
        assertTrue(filteredProductIds.contains(WorldlinegopaycoreConstants.PAYMENT_METHOD_GOOGLEPAY));
        assertTrue(filteredProductIds.contains(9999));
    }

    private WorldlinePaymentProductEvaluator evaluatorFor(Integer paymentProductId) {
        DefaultWorldlinePaymentProductEvaluator evaluator = new DefaultWorldlinePaymentProductEvaluator();
        evaluator.setPaymentProductId(paymentProductId);
        return evaluator;
    }

    private PaymentProduct paymentProduct(Integer id, String paymentMethod) {
        PaymentProduct paymentProduct = new PaymentProduct();
        paymentProduct.setId(id);
        paymentProduct.setPaymentMethod(paymentMethod);
        return paymentProduct;
    }

    private BaseStoreService baseStoreService(WorldlineCheckoutTypesEnum checkoutType) {
        BaseStoreModel baseStore = new BaseStoreModel();
        baseStore.setWorldlineCheckoutType(checkoutType);
        return (BaseStoreService) Proxy.newProxyInstance(
              BaseStoreService.class.getClassLoader(),
              new Class[]{BaseStoreService.class},
              (proxy, method, args) -> "getCurrentBaseStore".equals(method.getName()) ? baseStore : null);
    }
}
