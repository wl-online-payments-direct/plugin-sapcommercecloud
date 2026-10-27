package com.worldline.gopay.factory.impl;

import com.onlinepayments.domain.LineItem;
import com.onlinepayments.domain.ShoppingCart;
import com.worldline.gopay.enums.WorldlineMealvouchersProductType;
import com.worldline.gopay.util.WorldlineAmountUtils;
import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.core.model.product.UnitModel;
import de.hybris.platform.util.DiscountValue;
import de.hybris.platform.util.TaxValue;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@UnitTest
public class WorldlineShoppingCartTaxTest {

    @Test
    public void defaultShoppingCartUsesGrossLineAmountAndBreaksOutTax() {
        DefaultWorldlineShoppingCartFactory factory = new DefaultWorldlineShoppingCartFactory();
        factory.setWorldlineAmountUtils(new FixedScaleAmountUtils());

        AbstractOrderEntryModel entry = orderEntry("SKU-1", "Default item", 1L, 20.48d, 20.48d,
                Collections.emptyList(), Collections.singletonList(tax(3.41d)), null);
        AbstractOrderModel order = order(false, entry);

        ShoppingCart shoppingCart = factory.create(order);

        LineItem item = shoppingCart.getItems().get(0);
        assertEquals(Long.valueOf(2048L), item.getAmountOfMoney().getAmount());
        assertEquals(Long.valueOf(1707L), item.getOrderLineDetails().getProductPrice());
        assertEquals(Long.valueOf(341L), item.getOrderLineDetails().getTaxAmount());
    }

    @Test
    public void defaultShoppingCartUsesNetLineAmountAndAddsTaxForNetOrders() {
        DefaultWorldlineShoppingCartFactory factory = new DefaultWorldlineShoppingCartFactory();
        factory.setWorldlineAmountUtils(new FixedScaleAmountUtils());

        AbstractOrderEntryModel entry = orderEntry("SKU-1", "Default item", 1L, 17.07d, 17.07d,
                Collections.emptyList(), Collections.singletonList(tax(3.41d)), null);
        AbstractOrderModel order = order(true, entry);

        ShoppingCart shoppingCart = factory.create(order);

        LineItem item = shoppingCart.getItems().get(0);
        assertEquals(Long.valueOf(2048L), item.getAmountOfMoney().getAmount());
        assertEquals(Long.valueOf(1707L), item.getOrderLineDetails().getProductPrice());
        assertEquals(Long.valueOf(341L), item.getOrderLineDetails().getTaxAmount());
    }

    @Test
    public void mealvoucherShoppingCartUsesGrossLineAmountAndBreaksOutTax() {
        MealvouchersWorldlineShoppingCartFactory factory = new MealvouchersWorldlineShoppingCartFactory();
        factory.setWorldlineAmountUtils(new FixedScaleAmountUtils());

        AbstractOrderEntryModel entry = orderEntry("FOOD-1", "Food item", 1L, 204.48d, 204.48d,
                Collections.emptyList(), Collections.singletonList(tax(34.08d)), WorldlineMealvouchersProductType.FOODANDDRINK);
        AbstractOrderModel order = order(false, entry);
        when(order.getTotalTax()).thenReturn(34.08d);

        ShoppingCart shoppingCart = factory.create(order);

        LineItem item = shoppingCart.getItems().get(0);
        assertEquals(Long.valueOf(20448L), item.getAmountOfMoney().getAmount());
        assertEquals(Long.valueOf(17040L), item.getOrderLineDetails().getProductPrice());
        assertEquals(Long.valueOf(3408L), item.getOrderLineDetails().getTaxAmount());
    }

    @Test
    public void mealvoucherShoppingCartUsesNetLineAmountAndAddsTaxForNetOrders() {
        MealvouchersWorldlineShoppingCartFactory factory = new MealvouchersWorldlineShoppingCartFactory();
        factory.setWorldlineAmountUtils(new FixedScaleAmountUtils());

        AbstractOrderEntryModel entry = orderEntry("FOOD-1", "Food item", 1L, 170.40d, 170.40d,
                Collections.emptyList(), Collections.singletonList(tax(34.08d)), WorldlineMealvouchersProductType.FOODANDDRINK);
        AbstractOrderModel order = order(true, entry);
        when(order.getTotalTax()).thenReturn(34.08d);

        ShoppingCart shoppingCart = factory.create(order);

        LineItem item = shoppingCart.getItems().get(0);
        assertEquals(Long.valueOf(20448L), item.getAmountOfMoney().getAmount());
        assertEquals(Long.valueOf(17040L), item.getOrderLineDetails().getProductPrice());
        assertEquals(Long.valueOf(3408L), item.getOrderLineDetails().getTaxAmount());
    }

    @Test
    public void defaultShoppingCartSplitsLineWhenAmountsDoNotDivideByQuantity() {
        DefaultWorldlineShoppingCartFactory factory = new DefaultWorldlineShoppingCartFactory();
        factory.setWorldlineAmountUtils(new FixedScaleAmountUtils());

        AbstractOrderEntryModel entry = orderEntry("SKU-1", "Default item", 5L, 116.85d, 584.25d,
                Collections.emptyList(), Collections.singletonList(tax(97.38d)), null);
        AbstractOrderModel order = order(false, entry);

        ShoppingCart shoppingCart = factory.create(order);

        assertEquals(2, shoppingCart.getItems().size());
        assertLineItem(shoppingCart.getItems().get(0), 4L, 9737L, 0L, 1947L, 46736L);
        assertLineItem(shoppingCart.getItems().get(1), 1L, 9739L, 0L, 1950L, 11689L);
        assertEquals(58425L, totalAmount(shoppingCart));
    }

    @Test
    public void defaultShoppingCartSendsPerUnitTaxWhenQuantityDividesEvenly() {
        DefaultWorldlineShoppingCartFactory factory = new DefaultWorldlineShoppingCartFactory();
        factory.setWorldlineAmountUtils(new FixedScaleAmountUtils());

        AbstractOrderEntryModel entry = orderEntry("SKU-1", "Default item", 4L, 20.00d, 80.00d,
                Collections.emptyList(), Collections.singletonList(tax(13.32d)), null);
        AbstractOrderModel order = order(false, entry);

        ShoppingCart shoppingCart = factory.create(order);

        assertEquals(1, shoppingCart.getItems().size());
        assertLineItem(shoppingCart.getItems().get(0), 4L, 1667L, 0L, 333L, 8000L);
    }

    @Test
    public void defaultShoppingCartSendsPerUnitDiscount() {
        DefaultWorldlineShoppingCartFactory factory = new DefaultWorldlineShoppingCartFactory();
        factory.setWorldlineAmountUtils(new FixedScaleAmountUtils());

        AbstractOrderEntryModel entry = orderEntry("SKU-1", "Default item", 3L, 31.00d, 90.00d,
                Collections.singletonList(discount(3.00d)), Collections.singletonList(tax(15.00d)), null);
        AbstractOrderModel order = order(false, entry);

        ShoppingCart shoppingCart = factory.create(order);

        assertEquals(1, shoppingCart.getItems().size());
        assertLineItem(shoppingCart.getItems().get(0), 3L, 2600L, 100L, 500L, 9000L);
    }

    @Test
    public void mealvoucherShoppingCartSendsWholeLineAsOneUnitWhenAmountsDoNotDivideByQuantity() {
        MealvouchersWorldlineShoppingCartFactory factory = new MealvouchersWorldlineShoppingCartFactory();
        factory.setWorldlineAmountUtils(new FixedScaleAmountUtils());

        AbstractOrderEntryModel entry = orderEntry("FOOD-1", "Food item", 5L, 116.85d, 584.25d,
                Collections.emptyList(), Collections.singletonList(tax(97.38d)), WorldlineMealvouchersProductType.FOODANDDRINK);
        AbstractOrderModel order = order(false, entry);
        when(order.getTotalTax()).thenReturn(97.38d);

        ShoppingCart shoppingCart = factory.create(order);

        assertEquals(1, shoppingCart.getItems().size());
        assertLineItem(shoppingCart.getItems().get(0), 1L, 48687L, 0L, 9738L, 58425L);
    }

    @Test
    public void mealvoucherShoppingCartSendsPerUnitTaxWhenQuantityDividesEvenly() {
        MealvouchersWorldlineShoppingCartFactory factory = new MealvouchersWorldlineShoppingCartFactory();
        factory.setWorldlineAmountUtils(new FixedScaleAmountUtils());

        AbstractOrderEntryModel entry = orderEntry("FOOD-1", "Food item", 2L, 20.00d, 40.00d,
                Collections.emptyList(), Collections.singletonList(tax(6.66d)), WorldlineMealvouchersProductType.FOODANDDRINK);
        AbstractOrderModel order = order(false, entry);
        when(order.getTotalTax()).thenReturn(6.66d);

        ShoppingCart shoppingCart = factory.create(order);

        assertEquals(1, shoppingCart.getItems().size());
        assertLineItem(shoppingCart.getItems().get(0), 2L, 1667L, 0L, 333L, 4000L);
    }

    @Test
    public void lineItemPortionsAlwaysAddUpToLineTotals() {
        for (long quantity = 1; quantity <= 12; quantity++) {
            for (long net = 0; net <= 2000; net += 37) {
                for (long discount : new long[]{0L, 1L, 250L, 999L}) {
                    for (long tax : new long[]{0L, 1L, 333L, 1947L}) {
                        List<WorldlineLineItemPortions.Portion> portions = WorldlineLineItemPortions.split(quantity, net, discount, tax);
                        long units = 0;
                        long amount = 0;
                        for (WorldlineLineItemPortions.Portion portion : portions) {
                            assertTrue(portion.quantity() > 0);
                            assertEquals(portion.quantity() * (portion.productPrice() - portion.discount() + portion.tax()), portion.amount());
                            units += portion.quantity();
                            amount += portion.amount();
                        }
                        assertEquals(quantity, units);
                        assertEquals(net + tax, amount);
                    }
                }
            }
        }
    }

    @Test
    public void lineItemPortionsTreatMissingQuantityAsOneUnit() {
        List<WorldlineLineItemPortions.Portion> portions = WorldlineLineItemPortions.split(null, 1707L, 0L, 341L);

        assertEquals(1, portions.size());
        assertEquals(1L, portions.get(0).quantity());
        assertEquals(2048L, portions.get(0).amount());
    }

    private void assertLineItem(LineItem item, long quantity, long productPrice, long discountAmount, long taxAmount, long amount) {
        assertEquals(Long.valueOf(quantity), item.getOrderLineDetails().getQuantity());
        assertEquals(Long.valueOf(productPrice), item.getOrderLineDetails().getProductPrice());
        assertEquals(Long.valueOf(discountAmount), item.getOrderLineDetails().getDiscountAmount());
        assertEquals(Long.valueOf(taxAmount), item.getOrderLineDetails().getTaxAmount());
        assertEquals(Long.valueOf(amount), item.getAmountOfMoney().getAmount());
        // Worldline rejects a line item unless its per unit amounts add up to the line amount
        assertEquals(amount, quantity * (productPrice - discountAmount + taxAmount));
    }

    private long totalAmount(ShoppingCart shoppingCart) {
        return shoppingCart.getItems().stream().mapToLong(item -> item.getAmountOfMoney().getAmount()).sum();
    }

    private DiscountValue discount(double appliedValue) {
        return new DiscountValue("PROMO", appliedValue, true, appliedValue, "EUR");
    }

    private AbstractOrderModel order(boolean net, AbstractOrderEntryModel... entries) {
        AbstractOrderModel order = mock(AbstractOrderModel.class);
        CurrencyModel currency = mock(CurrencyModel.class);
        when(currency.getIsocode()).thenReturn("EUR");
        when(order.getNet()).thenReturn(net);
        when(order.getCurrency()).thenReturn(currency);
        when(order.getEntries()).thenReturn(java.util.Arrays.asList(entries));
        return order;
    }

    private AbstractOrderEntryModel orderEntry(String productCode, String productName, Long quantity, Double basePrice,
                                               Double totalPrice, java.util.List<DiscountValue> discounts,
                                               java.util.Collection<TaxValue> taxes,
                                               WorldlineMealvouchersProductType mealvouchersProductType) {
        ProductModel product = mock(ProductModel.class);
        when(product.getCode()).thenReturn(productCode);
        when(product.getName()).thenReturn(productName);
        when(product.getWorldlineMealvoucherProductType()).thenReturn(mealvouchersProductType);

        UnitModel unit = mock(UnitModel.class);
        when(unit.getCode()).thenReturn("pieces");
        when(product.getUnit()).thenReturn(unit);

        AbstractOrderEntryModel entry = mock(AbstractOrderEntryModel.class);
        when(entry.getProduct()).thenReturn(product);
        when(entry.getQuantity()).thenReturn(quantity);
        when(entry.getBasePrice()).thenReturn(basePrice);
        when(entry.getTotalPrice()).thenReturn(totalPrice);
        when(entry.getDiscountValues()).thenReturn(discounts);
        when(entry.getTaxValues()).thenReturn(taxes);
        return entry;
    }

    private TaxValue tax(double appliedValue) {
        return new TaxValue("VAT", 0d, true, appliedValue, "EUR");
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
