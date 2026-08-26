package com.worldline.gopay.populator;

import com.onlinepayments.domain.Order;
import com.worldline.gopay.model.WorldlineConfigurationModel;
import com.worldline.gopay.util.WorldlineAmountUtils;
import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.model.c2l.CountryModel;
import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.store.BaseStoreModel;
import de.hybris.platform.util.TaxValue;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@UnitTest
public class WorldlineOrderRequestParamPopulatorTest {

    @Test
    public void populateSetsNetShippingCostAndTaxFromOrderTaxMinusEntryTax() {
        WorldlineOrderRequestParamPopulator populator = new WorldlineOrderRequestParamPopulator();
        populator.setWorldlineAmountUtils(new FixedScaleAmountUtils());
        AbstractOrderModel source = orderWithShippingAndTax(false, 7.99d, 124.84d);
        Order target = new Order();

        populator.populate(source, target);

        assertEquals(Long.valueOf(12484L), target.getAmountOfMoney().getAmount());
        assertEquals(Long.valueOf(666L), target.getShipping().getShippingCost());
        assertEquals(Long.valueOf(133L), target.getShipping().getShippingCostTax());
    }

    @Test
    public void populateLeavesNetShippingCostUntouchedAndAddsTaxToOrderAmountForNetOrders() {
        WorldlineOrderRequestParamPopulator populator = new WorldlineOrderRequestParamPopulator();
        populator.setWorldlineAmountUtils(new FixedScaleAmountUtils());
        AbstractOrderModel source = orderWithShippingAndTax(true, 6.66d, 104.03d);
        Order target = new Order();

        populator.populate(source, target);

        assertEquals(Long.valueOf(12484L), target.getAmountOfMoney().getAmount());
        assertEquals(Long.valueOf(666L), target.getShipping().getShippingCost());
        assertEquals(Long.valueOf(133L), target.getShipping().getShippingCostTax());
    }

    private AbstractOrderModel orderWithShippingAndTax(boolean net, double deliveryCost, double totalPrice) {
        CurrencyModel currency = mock(CurrencyModel.class);
        when(currency.getIsocode()).thenReturn("EUR");

        CountryModel country = mock(CountryModel.class);
        when(country.getIsocode()).thenReturn("NL");

        AddressModel address = mock(AddressModel.class);
        when(address.getCountry()).thenReturn(country);
        when(address.getShippingAddress()).thenReturn(Boolean.TRUE);
        when(address.getPostalcode()).thenReturn("SW1 1AA");
        when(address.getTown()).thenReturn("London");
        when(address.getLine1()).thenReturn("123 Fake Street");
        when(address.getLine2()).thenReturn("");
        when(address.getFirstname()).thenReturn("Fred");
        when(address.getLastname()).thenReturn("Flintstone");

        WorldlineConfigurationModel configuration = mock(WorldlineConfigurationModel.class);
        when(configuration.getMerchantID()).thenReturn("greenlightcomm");
        when(configuration.isApplySurcharge()).thenReturn(Boolean.FALSE);
        BaseStoreModel store = mock(BaseStoreModel.class);
        when(store.getWorldlineConfiguration()).thenReturn(configuration);

        AbstractOrderEntryModel entry = mock(AbstractOrderEntryModel.class);
        when(entry.getTaxValues()).thenReturn(Collections.singletonList(new TaxValue("VAT", 0d, true, 19.48d, "EUR")));

        AbstractOrderModel order = mock(AbstractOrderModel.class);
        when(order.getNet()).thenReturn(net);
        when(order.getCurrency()).thenReturn(currency);
        when(order.getDeliveryAddress()).thenReturn(address);
        when(order.getDeliveryCost()).thenReturn(deliveryCost);
        when(order.getEntries()).thenReturn(Collections.singletonList(entry));
        when(order.getTotalTax()).thenReturn(20.81d);
        when(order.getTotalDiscounts()).thenReturn(0d);
        when(order.getTotalPrice()).thenReturn(totalPrice);
        when(order.getPaymentCost()).thenReturn(0d);
        when(order.getCode()).thenReturn("JJ100040002");
        when(order.getStore()).thenReturn(store);
        return order;
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
