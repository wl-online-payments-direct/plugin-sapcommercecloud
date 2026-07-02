package com.worldline.direct.populator.paymentlink;

import com.onlinepayments.domain.AmountOfMoney;
import com.onlinepayments.domain.CreateHostedCheckoutRequest;
import com.onlinepayments.domain.CreatePaymentLinkRequest;
import com.onlinepayments.domain.HostedCheckoutSpecificInput;
import com.onlinepayments.domain.Order;
import com.onlinepayments.domain.OrderReferences;
import com.worldline.direct.model.WorldlineConfigurationModel;
import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.store.BaseStoreModel;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
@UnitTest
public class WorldlinePaymentLinkPopulatorTest {

    @Test
    public void populateBuildsOneTimePaymentLinkFromHostedCheckoutRequest() {
        WorldlinePaymentLinkPopulator populator = new WorldlinePaymentLinkPopulator();
        CreateHostedCheckoutRequest hostedCheckoutRequest = hostedCheckoutRequest();
        populator.setWorldlineHostedCheckoutParamConverter(converter(hostedCheckoutRequest));
        populator.setDisplayQRCode(true);

        CreatePaymentLinkRequest paymentLinkRequest = new CreatePaymentLinkRequest();
        populator.populate(order(72), paymentLinkRequest);

        assertSame(hostedCheckoutRequest.getOrder(), paymentLinkRequest.getOrder());
        assertSame(hostedCheckoutRequest.getHostedCheckoutSpecificInput(), paymentLinkRequest.getHostedCheckoutSpecificInput());
        assertFalse(paymentLinkRequest.getIsReusableLink());
        assertEquals(Boolean.TRUE, paymentLinkRequest.getDisplayQRCode());
        ZonedDateTime expirationDate = paymentLinkRequest.getPaymentLinkSpecificInput().getExpirationDate();
        assertNotNull(expirationDate);
        long hoursUntilExpiration = ChronoUnit.HOURS.between(ZonedDateTime.now(), expirationDate);
        assertTrue(hoursUntilExpiration >= 71L && hoursUntilExpiration <= 72L);
    }

    @Test
    public void populateRemovesSavedTokensBecausePaymentLinksAreBearerLinks() {
        WorldlinePaymentLinkPopulator populator = new WorldlinePaymentLinkPopulator();
        CreateHostedCheckoutRequest hostedCheckoutRequest = hostedCheckoutRequest();
        hostedCheckoutRequest.getHostedCheckoutSpecificInput().setTokens("token-1,token-2");
        populator.setWorldlineHostedCheckoutParamConverter(converter(hostedCheckoutRequest));

        CreatePaymentLinkRequest paymentLinkRequest = new CreatePaymentLinkRequest();
        populator.populate(order(168), paymentLinkRequest);

        assertNull(paymentLinkRequest.getHostedCheckoutSpecificInput().getTokens());
    }

    private AbstractOrderModel order(Integer expirationHours) {
        WorldlineConfigurationModel configuration = new WorldlineConfigurationModel();
        configuration.setPaymentLinkExpirationHours(expirationHours);

        BaseStoreModel store = new BaseStoreModel();
        store.setWorldlineConfiguration(configuration);

        AbstractOrderModel order = new AbstractOrderModel();
        order.setStore(store);
        return order;
    }

    private Converter<AbstractOrderModel, CreateHostedCheckoutRequest> converter(CreateHostedCheckoutRequest request) {
        return new Converter<AbstractOrderModel, CreateHostedCheckoutRequest>() {
            @Override
            public CreateHostedCheckoutRequest convert(AbstractOrderModel source) throws ConversionException {
                return request;
            }

            @Override
            public CreateHostedCheckoutRequest convert(AbstractOrderModel source, CreateHostedCheckoutRequest prototype) throws ConversionException {
                return request;
            }
        };
    }

    private CreateHostedCheckoutRequest hostedCheckoutRequest() {
        AmountOfMoney amountOfMoney = new AmountOfMoney();
        amountOfMoney.setAmount(1234L);
        amountOfMoney.setCurrencyCode("EUR");

        OrderReferences references = new OrderReferences();
        references.setMerchantReference("ORDER-100");

        Order order = new Order();
        order.setAmountOfMoney(amountOfMoney);
        order.setReferences(references);

        HostedCheckoutSpecificInput hostedCheckoutSpecificInput = new HostedCheckoutSpecificInput();
        hostedCheckoutSpecificInput.setReturnUrl("https://shop.example/worldline/payment-link/return/ORDER-100");

        CreateHostedCheckoutRequest request = new CreateHostedCheckoutRequest();
        request.setOrder(order);
        request.setHostedCheckoutSpecificInput(hostedCheckoutSpecificInput);
        return request;
    }
}
