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

import static org.junit.Assert.assertEquals;
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
