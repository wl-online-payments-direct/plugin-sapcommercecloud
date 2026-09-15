package com.worldline.gopay.populator.paymentlink;

import com.onlinepayments.domain.AmountOfMoney;
import com.onlinepayments.domain.CreateHostedCheckoutRequest;
import com.onlinepayments.domain.CreatePaymentLinkRequest;
import com.onlinepayments.domain.HostedCheckoutSpecificInput;
import com.onlinepayments.domain.Order;
import com.onlinepayments.domain.OrderReferences;
import com.onlinepayments.domain.RedirectPaymentMethodSpecificInput;
import com.onlinepayments.domain.RedirectionData;
import com.worldline.gopay.model.WorldlineConfigurationModel;
import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.acceleratorservices.urlresolver.SiteBaseUrlResolutionService;
import de.hybris.platform.basecommerce.model.site.BaseSiteModel;
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
    public void populateSendsNoReturnUrlWhenNoReturnPageIsConfigured() {
        WorldlinePaymentLinkPopulator populator = new WorldlinePaymentLinkPopulator();
        CreateHostedCheckoutRequest hostedCheckoutRequest = hostedCheckoutRequest();
        hostedCheckoutRequest.getHostedCheckoutSpecificInput().setReturnUrl("https://shop.example/worldline/payment-link/return/ORDER-100");
        RedirectPaymentMethodSpecificInput redirectInput = new RedirectPaymentMethodSpecificInput();
        RedirectionData redirectionData = new RedirectionData();
        redirectionData.setReturnUrl("https://shop.example/worldline/payment-link/return/ORDER-100");
        redirectInput.setRedirectionData(redirectionData);
        hostedCheckoutRequest.setRedirectPaymentMethodSpecificInput(redirectInput);
        populator.setWorldlineHostedCheckoutParamConverter(converter(hostedCheckoutRequest));

        CreatePaymentLinkRequest paymentLinkRequest = new CreatePaymentLinkRequest();
        populator.populate(order(168), paymentLinkRequest);

        assertNull(paymentLinkRequest.getHostedCheckoutSpecificInput().getReturnUrl());
        assertNull(paymentLinkRequest.getRedirectPaymentMethodSpecificInput().getRedirectionData().getReturnUrl());
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

    @Test
    public void populateResolvesConfiguredSiteRelativePathAgainstTheOrdersBaseSite() {
        WorldlinePaymentLinkPopulator populator = new WorldlinePaymentLinkPopulator();
        CreateHostedCheckoutRequest hostedCheckoutRequest = hostedCheckoutRequestWithRedirectionData();
        populator.setWorldlineHostedCheckoutParamConverter(converter(hostedCheckoutRequest));
        RecordingSiteBaseUrlResolutionService urlResolver = new RecordingSiteBaseUrlResolutionService("https://shop.example/payment-complete");
        populator.setSiteBaseUrlResolutionService(urlResolver);

        AbstractOrderModel order = order(168);
        BaseSiteModel site = new BaseSiteModel();
        order.setSite(site);
        order.getStore().getWorldlineConfiguration().setPaymentLinkReturnUrl("/payment-complete");

        CreatePaymentLinkRequest paymentLinkRequest = new CreatePaymentLinkRequest();
        populator.populate(order, paymentLinkRequest);

        assertEquals("https://shop.example/payment-complete", paymentLinkRequest.getHostedCheckoutSpecificInput().getReturnUrl());
        assertEquals("https://shop.example/payment-complete", paymentLinkRequest.getRedirectPaymentMethodSpecificInput().getRedirectionData().getReturnUrl());
        assertSame(site, urlResolver.site);
        assertTrue(urlResolver.secure);
        assertEquals("/payment-complete", urlResolver.path);
    }

    @Test
    public void populatePrependsSlashToAConfiguredPathThatOmitsIt() {
        WorldlinePaymentLinkPopulator populator = new WorldlinePaymentLinkPopulator();
        populator.setWorldlineHostedCheckoutParamConverter(converter(hostedCheckoutRequestWithRedirectionData()));
        RecordingSiteBaseUrlResolutionService urlResolver = new RecordingSiteBaseUrlResolutionService("https://shop.example/payment-complete");
        populator.setSiteBaseUrlResolutionService(urlResolver);

        AbstractOrderModel order = order(168);
        order.setSite(new BaseSiteModel());
        order.getStore().getWorldlineConfiguration().setPaymentLinkReturnUrl("payment-complete");

        populator.populate(order, new CreatePaymentLinkRequest());

        assertEquals("/payment-complete", urlResolver.path);
    }

    @Test
    public void populateSendsAConfiguredAbsoluteUrlWithoutResolvingIt() {
        WorldlinePaymentLinkPopulator populator = new WorldlinePaymentLinkPopulator();
        CreateHostedCheckoutRequest hostedCheckoutRequest = hostedCheckoutRequestWithRedirectionData();
        populator.setWorldlineHostedCheckoutParamConverter(converter(hostedCheckoutRequest));
        RecordingSiteBaseUrlResolutionService urlResolver = new RecordingSiteBaseUrlResolutionService("https://shop.example/resolved");
        populator.setSiteBaseUrlResolutionService(urlResolver);

        AbstractOrderModel order = order(168);
        order.setSite(new BaseSiteModel());
        order.getStore().getWorldlineConfiguration().setPaymentLinkReturnUrl("https://thanks.example/payment-complete");

        CreatePaymentLinkRequest paymentLinkRequest = new CreatePaymentLinkRequest();
        populator.populate(order, paymentLinkRequest);

        assertEquals("https://thanks.example/payment-complete", paymentLinkRequest.getHostedCheckoutSpecificInput().getReturnUrl());
        assertEquals("https://thanks.example/payment-complete", paymentLinkRequest.getRedirectPaymentMethodSpecificInput().getRedirectionData().getReturnUrl());
        assertNull(urlResolver.site);
    }

    @Test
    public void populateSendsNoReturnUrlWhenARelativePathIsConfiguredButTheOrderHasNoSite() {
        WorldlinePaymentLinkPopulator populator = new WorldlinePaymentLinkPopulator();
        CreateHostedCheckoutRequest hostedCheckoutRequest = hostedCheckoutRequestWithRedirectionData();
        populator.setWorldlineHostedCheckoutParamConverter(converter(hostedCheckoutRequest));
        populator.setSiteBaseUrlResolutionService(new RecordingSiteBaseUrlResolutionService("https://shop.example/payment-complete"));

        AbstractOrderModel order = order(168);
        order.getStore().getWorldlineConfiguration().setPaymentLinkReturnUrl("/payment-complete");

        CreatePaymentLinkRequest paymentLinkRequest = new CreatePaymentLinkRequest();
        populator.populate(order, paymentLinkRequest);

        assertNull(paymentLinkRequest.getHostedCheckoutSpecificInput().getReturnUrl());
        assertNull(paymentLinkRequest.getRedirectPaymentMethodSpecificInput().getRedirectionData().getReturnUrl());
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

    private CreateHostedCheckoutRequest hostedCheckoutRequestWithRedirectionData() {
        CreateHostedCheckoutRequest request = hostedCheckoutRequest();
        RedirectionData redirectionData = new RedirectionData();
        redirectionData.setReturnUrl("https://shop.example/worldline/payment-link/return/ORDER-100");
        RedirectPaymentMethodSpecificInput redirectInput = new RedirectPaymentMethodSpecificInput();
        redirectInput.setRedirectionData(redirectionData);
        request.setRedirectPaymentMethodSpecificInput(redirectInput);
        return request;
    }

    private static class RecordingSiteBaseUrlResolutionService implements SiteBaseUrlResolutionService {

        private final String websiteUrl;
        private BaseSiteModel site;
        private boolean secure;
        private String path;

        private RecordingSiteBaseUrlResolutionService(String websiteUrl) {
            this.websiteUrl = websiteUrl;
        }

        @Override
        public String getWebsiteUrlForSite(BaseSiteModel site, boolean secure, String path) {
            this.site = site;
            this.secure = secure;
            this.path = path;
            return websiteUrl;
        }

        @Override
        public String getMediaUrlForSite(BaseSiteModel site, boolean secure) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getMediaUrlForSite(BaseSiteModel site, boolean secure, String path) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getWebsiteUrlForSite(BaseSiteModel site, boolean secure, String path, String queryParams) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getWebsiteUrlForSite(BaseSiteModel site, String encodingAttributes, boolean secure, String path) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getWebsiteUrlForSite(BaseSiteModel site, String encodingAttributes, boolean secure, String path, String queryParams) {
            throw new UnsupportedOperationException();
        }
    }
}
