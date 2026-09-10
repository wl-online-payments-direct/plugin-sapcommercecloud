package com.worldline.gopay.populator.paymentlink;

import com.onlinepayments.domain.CreateHostedCheckoutRequest;
import com.onlinepayments.domain.CreatePaymentLinkRequest;
import com.onlinepayments.domain.HostedCheckoutSpecificInput;
import com.onlinepayments.domain.Order;
import com.onlinepayments.domain.PaymentLinkOrderInput;
import com.onlinepayments.domain.PaymentLinkSpecificInput;
import com.onlinepayments.domain.RedirectPaymentMethodSpecificInput;
import de.hybris.platform.acceleratorservices.urlresolver.SiteBaseUrlResolutionService;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import java.time.ZonedDateTime;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

public class WorldlinePaymentLinkPopulator implements Populator<AbstractOrderModel, CreatePaymentLinkRequest> {

    private final static Logger LOGGER = LoggerFactory.getLogger(WorldlinePaymentLinkPopulator.class);

    private static final int DEFAULT_EXPIRATION_HOURS = 168;
    private static final int MIN_EXPIRATION_HOURS = 24;
    private static final int MAX_EXPIRATION_HOURS = 24 * 31 * 6;
    private static final String HTTP_SCHEME = "http://";
    private static final String HTTPS_SCHEME = "https://";
    private static final String SLASH = "/";

    private Converter<AbstractOrderModel, CreateHostedCheckoutRequest> worldlineHostedCheckoutParamConverter;
    private SiteBaseUrlResolutionService siteBaseUrlResolutionService;
    private boolean displayQRCode;

    @Override
    public void populate(AbstractOrderModel abstractOrderModel, CreatePaymentLinkRequest createPaymentLinkRequest) throws ConversionException {
        validateParameterNotNull(abstractOrderModel, "abstractOrderModel cannot be null!");

        final CreateHostedCheckoutRequest hostedCheckoutRequest = worldlineHostedCheckoutParamConverter.convert(abstractOrderModel);
        final Order order = hostedCheckoutRequest.getOrder();

        createPaymentLinkRequest.setOrder(order);
        createPaymentLinkRequest.setPaymentLinkOrder(getPaymentLinkOrder(order));
        final String returnUrl = resolveReturnUrl(abstractOrderModel);
        createPaymentLinkRequest.setHostedCheckoutSpecificInput(getHostedCheckoutSpecificInput(hostedCheckoutRequest, returnUrl));
        createPaymentLinkRequest.setCardPaymentMethodSpecificInput(hostedCheckoutRequest.getCardPaymentMethodSpecificInput());
        createPaymentLinkRequest.setRedirectPaymentMethodSpecificInput(getRedirectPaymentMethodSpecificInput(hostedCheckoutRequest, returnUrl));
        createPaymentLinkRequest.setMobilePaymentMethodSpecificInput(hostedCheckoutRequest.getMobilePaymentMethodSpecificInput());
        createPaymentLinkRequest.setSepaDirectDebitPaymentMethodSpecificInput(hostedCheckoutRequest.getSepaDirectDebitPaymentMethodSpecificInput());
        createPaymentLinkRequest.setFraudFields(hostedCheckoutRequest.getFraudFields());
        createPaymentLinkRequest.setFeedbacks(hostedCheckoutRequest.getFeedbacks());
        createPaymentLinkRequest.setIsReusableLink(Boolean.FALSE);
        createPaymentLinkRequest.setDisplayQRCode(BooleanUtils.toBooleanObject(displayQRCode));
        createPaymentLinkRequest.setPaymentLinkSpecificInput(getPaymentLinkSpecificInput(abstractOrderModel));
    }

    protected HostedCheckoutSpecificInput getHostedCheckoutSpecificInput(CreateHostedCheckoutRequest hostedCheckoutRequest, String returnUrl) {
        final HostedCheckoutSpecificInput hostedCheckoutSpecificInput = hostedCheckoutRequest.getHostedCheckoutSpecificInput();
        if (hostedCheckoutSpecificInput != null) {
            hostedCheckoutSpecificInput.setTokens(null);
            // The returnUrl the customer would have been sent to belongs to the agent's checkout session, so it is
            // replaced by the merchant's configured Pay by Link return page, or dropped entirely if none is
            // configured - in which case the order is unlocked purely via webhooks.
            hostedCheckoutSpecificInput.setReturnUrl(returnUrl);
        }
        return hostedCheckoutSpecificInput;
    }

    protected RedirectPaymentMethodSpecificInput getRedirectPaymentMethodSpecificInput(CreateHostedCheckoutRequest hostedCheckoutRequest, String returnUrl) {
        final RedirectPaymentMethodSpecificInput redirectPaymentMethodSpecificInput = hostedCheckoutRequest.getRedirectPaymentMethodSpecificInput();
        if (redirectPaymentMethodSpecificInput != null && redirectPaymentMethodSpecificInput.getRedirectionData() != null) {
            // See getHostedCheckoutSpecificInput.
            redirectPaymentMethodSpecificInput.getRedirectionData().setReturnUrl(returnUrl);
        }
        return redirectPaymentMethodSpecificInput;
    }

    /**
     * Resolves the merchant's configured Pay by Link return page into an absolute URL. Returns {@code null} when no
     * return page is configured, or when a site-relative path is configured but the order has no base site to resolve
     * it against - in both cases no returnUrl is sent to Worldline GoPay.
     */
    protected String resolveReturnUrl(AbstractOrderModel abstractOrderModel) {
        final String configuredReturnUrl = getConfiguredReturnUrl(abstractOrderModel);
        if (StringUtils.isBlank(configuredReturnUrl)) {
            return null;
        }
        final String trimmedReturnUrl = configuredReturnUrl.trim();
        if (StringUtils.startsWithIgnoreCase(trimmedReturnUrl, HTTP_SCHEME) || StringUtils.startsWithIgnoreCase(trimmedReturnUrl, HTTPS_SCHEME)) {
            return trimmedReturnUrl;
        }
        if (abstractOrderModel.getSite() == null) {
            LOGGER.warn("[ WORLDLINE ] Pay by Link return page [{}] cannot be resolved because order [{}] has no base site, no returnUrl will be sent.",
                    trimmedReturnUrl, abstractOrderModel.getCode());
            return null;
        }
        return siteBaseUrlResolutionService.getWebsiteUrlForSite(abstractOrderModel.getSite(), true, asPath(trimmedReturnUrl));
    }

    protected String getConfiguredReturnUrl(AbstractOrderModel abstractOrderModel) {
        if (abstractOrderModel.getStore() == null || abstractOrderModel.getStore().getWorldlineConfiguration() == null) {
            return null;
        }
        return abstractOrderModel.getStore().getWorldlineConfiguration().getPaymentLinkReturnUrl();
    }

    private String asPath(String configuredReturnUrl) {
        return configuredReturnUrl.startsWith(SLASH) ? configuredReturnUrl : SLASH + configuredReturnUrl;
    }

    protected PaymentLinkOrderInput getPaymentLinkOrder(Order order) {
        if (order == null) {
            return null;
        }
        final PaymentLinkOrderInput paymentLinkOrder = new PaymentLinkOrderInput();
        paymentLinkOrder.setAmount(order.getAmountOfMoney());
        if (order.getReferences() != null) {
            paymentLinkOrder.setMerchantReference(order.getReferences().getMerchantReference());
        }
        return paymentLinkOrder;
    }

    protected PaymentLinkSpecificInput getPaymentLinkSpecificInput(AbstractOrderModel abstractOrderModel) {
        final PaymentLinkSpecificInput paymentLinkSpecificInput = new PaymentLinkSpecificInput();
        paymentLinkSpecificInput.setExpirationDate(ZonedDateTime.now().plusHours(getExpirationHours(abstractOrderModel)));
        return paymentLinkSpecificInput;
    }

    protected int getExpirationHours(AbstractOrderModel abstractOrderModel) {
        Integer expirationHours = null;
        if (abstractOrderModel.getStore() != null && abstractOrderModel.getStore().getWorldlineConfiguration() != null) {
            expirationHours = abstractOrderModel.getStore().getWorldlineConfiguration().getPaymentLinkExpirationHours();
        }
        if (expirationHours == null) {
            return DEFAULT_EXPIRATION_HOURS;
        }
        return Math.max(MIN_EXPIRATION_HOURS, Math.min(MAX_EXPIRATION_HOURS, expirationHours));
    }

    @Required
    public void setWorldlineHostedCheckoutParamConverter(Converter<AbstractOrderModel, CreateHostedCheckoutRequest> worldlineHostedCheckoutParamConverter) {
        this.worldlineHostedCheckoutParamConverter = worldlineHostedCheckoutParamConverter;
    }

    @Required
    public void setSiteBaseUrlResolutionService(SiteBaseUrlResolutionService siteBaseUrlResolutionService) {
        this.siteBaseUrlResolutionService = siteBaseUrlResolutionService;
    }

    public void setDisplayQRCode(boolean displayQRCode) {
        this.displayQRCode = displayQRCode;
    }
}
