package com.worldline.direct.service.impl;

import com.onlinepayments.domain.AmountOfMoney;
import com.onlinepayments.domain.CardPaymentMethodSpecificInputBase;
import com.onlinepayments.domain.CardPaymentMethodSpecificInput;
import com.onlinepayments.domain.CreateHostedCheckoutRequest;
import com.onlinepayments.domain.CreatePaymentRequest;
import com.onlinepayments.domain.MobilePaymentMethodSpecificInput;
import com.onlinepayments.domain.MobilePaymentProduct320SpecificInput;
import com.onlinepayments.domain.Order;
import com.onlinepayments.domain.SepaDirectDebitPaymentMethodSpecificInputBase;
import com.onlinepayments.domain.SepaDirectDebitPaymentMethodSpecificInput;
import com.onlinepayments.domain.ShoppingCart;
import com.worldline.direct.util.WorldlineAmountUtils;
import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.core.model.order.CartModel;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@UnitTest
public class WorldlineB2BPaymentServiceImplTest {
    private WorldlineB2BPaymentServiceImpl paymentService;
    private CartModel order;

    @Before
    public void setUp() {
        paymentService = new WorldlineB2BPaymentServiceImpl();
        paymentService.setWorldlineAmountUtils(new FixedScaleAmountUtils());

        CurrencyModel currency = new CurrencyModel();
        currency.setIsocode("EUR");
        order = new CartModel();
        order.setCurrency(currency);
    }

    @Test
    public void applyZeroAmountForTokenizedRecurringSetupKeepsSepaAmountBecauseSepaCannotBeTokenized() {
        CreatePaymentRequest request = paymentRequest(12484L);
        ShoppingCart shoppingCart = request.getOrder().getShoppingCart();
        request.setSepaDirectDebitPaymentMethodSpecificInput(new SepaDirectDebitPaymentMethodSpecificInput());

        boolean zeroAmountApplied = paymentService.applyZeroAmountForTokenizedRecurringSetup(request, order, true);

        assertFalse(zeroAmountApplied);
        assertEquals(Long.valueOf(12484L), request.getOrder().getAmountOfMoney().getAmount());
        assertSame(shoppingCart, request.getOrder().getShoppingCart());
    }

    @Test
    public void applyZeroAmountForTokenizedRecurringSetupZeroesCardAmountAndEnablesTokenization() {
        CreatePaymentRequest request = paymentRequest(12484L);
        CardPaymentMethodSpecificInput cardInput = new CardPaymentMethodSpecificInput();
        request.setCardPaymentMethodSpecificInput(cardInput);

        boolean zeroAmountApplied = paymentService.applyZeroAmountForTokenizedRecurringSetup(request, order, true);

        assertTrue(zeroAmountApplied);
        assertEquals(Long.valueOf(0L), request.getOrder().getAmountOfMoney().getAmount());
        assertTrue(cardInput.getTokenize());
        assertNull(request.getOrder().getShoppingCart());
    }

    @Test
    public void applyZeroAmountForTokenizedRecurringSetupZeroesGooglePayAmountWhenTokenizationIsEnabled() {
        CreatePaymentRequest request = paymentRequest(12484L);
        MobilePaymentMethodSpecificInput mobileInput = new MobilePaymentMethodSpecificInput();
        MobilePaymentProduct320SpecificInput product320SpecificInput = new MobilePaymentProduct320SpecificInput();
        product320SpecificInput.setTokenize(true);
        mobileInput.setPaymentProduct320SpecificInput(product320SpecificInput);
        request.setMobilePaymentMethodSpecificInput(mobileInput);

        boolean zeroAmountApplied = paymentService.applyZeroAmountForTokenizedRecurringSetup(request, order, true);

        assertTrue(zeroAmountApplied);
        assertEquals(Long.valueOf(0L), request.getOrder().getAmountOfMoney().getAmount());
        assertNull(request.getOrder().getShoppingCart());
    }

    @Test
    public void applyZeroAmountForTokenizedRecurringHostedCheckoutKeepsSepaAmountBecauseSepaCannotBeTokenized() {
        CreateHostedCheckoutRequest request = hostedCheckoutRequest(12484L);
        ShoppingCart shoppingCart = request.getOrder().getShoppingCart();
        request.setSepaDirectDebitPaymentMethodSpecificInput(new SepaDirectDebitPaymentMethodSpecificInputBase());

        boolean zeroAmountApplied = paymentService.applyZeroAmountForTokenizedRecurringHostedCheckout(request, order, true);

        assertFalse(zeroAmountApplied);
        assertEquals(Long.valueOf(12484L), request.getOrder().getAmountOfMoney().getAmount());
        assertSame(shoppingCart, request.getOrder().getShoppingCart());
    }

    @Test
    public void applyZeroAmountForTokenizedRecurringHostedCheckoutZeroesCardAmountAndEnablesTokenization() {
        CreateHostedCheckoutRequest request = hostedCheckoutRequest(12484L);
        CardPaymentMethodSpecificInputBase cardInput = new CardPaymentMethodSpecificInputBase();
        request.setCardPaymentMethodSpecificInput(cardInput);

        boolean zeroAmountApplied = paymentService.applyZeroAmountForTokenizedRecurringHostedCheckout(request, order, true);

        assertTrue(zeroAmountApplied);
        assertEquals(Long.valueOf(0L), request.getOrder().getAmountOfMoney().getAmount());
        assertTrue(cardInput.getTokenize());
        assertNull(request.getOrder().getShoppingCart());
    }

    private CreatePaymentRequest paymentRequest(long amount) {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setOrder(order(amount));
        return request;
    }

    private CreateHostedCheckoutRequest hostedCheckoutRequest(long amount) {
        CreateHostedCheckoutRequest request = new CreateHostedCheckoutRequest();
        request.setOrder(order(amount));
        return request;
    }

    private Order order(long amount) {
        AmountOfMoney amountOfMoney = new AmountOfMoney();
        amountOfMoney.setAmount(amount);
        amountOfMoney.setCurrencyCode("EUR");

        Order paymentOrder = new Order();
        paymentOrder.setAmountOfMoney(amountOfMoney);
        paymentOrder.setShoppingCart(new ShoppingCart());
        return paymentOrder;
    }

    private static final class FixedScaleAmountUtils extends WorldlineAmountUtils {
        @Override
        public long createAmount(BigDecimal amount, String currencyIso) {
            return amount.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValue();
        }

        @Override
        public long createAmount(Double amount, String currencyIso) {
            return createAmount(BigDecimal.valueOf(amount), currencyIso);
        }
    }
}
