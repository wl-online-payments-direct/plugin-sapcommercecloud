package com.worldline.gopay.populator.paymentlink;

import com.onlinepayments.domain.CreateHostedCheckoutRequest;
import com.onlinepayments.domain.CreatePaymentLinkRequest;
import com.onlinepayments.domain.HostedCheckoutSpecificInput;
import com.onlinepayments.domain.Order;
import com.onlinepayments.domain.PaymentLinkOrderInput;
import com.onlinepayments.domain.PaymentLinkSpecificInput;
import com.onlinepayments.domain.RedirectPaymentMethodSpecificInput;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import org.apache.commons.lang.BooleanUtils;
import org.springframework.beans.factory.annotation.Required;

import java.time.ZonedDateTime;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

public class WorldlinePaymentLinkPopulator implements Populator<AbstractOrderModel, CreatePaymentLinkRequest> {

    private static final int DEFAULT_EXPIRATION_HOURS = 168;
    private static final int MIN_EXPIRATION_HOURS = 24;
    private static final int MAX_EXPIRATION_HOURS = 24 * 31 * 6;

    private Converter<AbstractOrderModel, CreateHostedCheckoutRequest> worldlineHostedCheckoutParamConverter;
    private boolean displayQRCode;

    @Override
    public void populate(AbstractOrderModel abstractOrderModel, CreatePaymentLinkRequest createPaymentLinkRequest) throws ConversionException {
        validateParameterNotNull(abstractOrderModel, "abstractOrderModel cannot be null!");

        final CreateHostedCheckoutRequest hostedCheckoutRequest = worldlineHostedCheckoutParamConverter.convert(abstractOrderModel);
        final Order order = hostedCheckoutRequest.getOrder();

        createPaymentLinkRequest.setOrder(order);
        createPaymentLinkRequest.setPaymentLinkOrder(getPaymentLinkOrder(order));
        createPaymentLinkRequest.setHostedCheckoutSpecificInput(getHostedCheckoutSpecificInput(hostedCheckoutRequest));
        createPaymentLinkRequest.setCardPaymentMethodSpecificInput(hostedCheckoutRequest.getCardPaymentMethodSpecificInput());
        createPaymentLinkRequest.setRedirectPaymentMethodSpecificInput(getRedirectPaymentMethodSpecificInput(hostedCheckoutRequest));
        createPaymentLinkRequest.setMobilePaymentMethodSpecificInput(hostedCheckoutRequest.getMobilePaymentMethodSpecificInput());
        createPaymentLinkRequest.setSepaDirectDebitPaymentMethodSpecificInput(hostedCheckoutRequest.getSepaDirectDebitPaymentMethodSpecificInput());
        createPaymentLinkRequest.setFraudFields(hostedCheckoutRequest.getFraudFields());
        createPaymentLinkRequest.setFeedbacks(hostedCheckoutRequest.getFeedbacks());
        createPaymentLinkRequest.setIsReusableLink(Boolean.FALSE);
        createPaymentLinkRequest.setDisplayQRCode(BooleanUtils.toBooleanObject(displayQRCode));
        createPaymentLinkRequest.setPaymentLinkSpecificInput(getPaymentLinkSpecificInput(abstractOrderModel));
    }

    protected HostedCheckoutSpecificInput getHostedCheckoutSpecificInput(CreateHostedCheckoutRequest hostedCheckoutRequest) {
        final HostedCheckoutSpecificInput hostedCheckoutSpecificInput = hostedCheckoutRequest.getHostedCheckoutSpecificInput();
        if (hostedCheckoutSpecificInput != null) {
            hostedCheckoutSpecificInput.setTokens(null);
            // Pay by Link never redirects the customer back to the storefront; the order is
            // unlocked purely via webhooks, so no returnUrl must be sent to Worldline.
            hostedCheckoutSpecificInput.setReturnUrl(null);
        }
        return hostedCheckoutSpecificInput;
    }

    protected RedirectPaymentMethodSpecificInput getRedirectPaymentMethodSpecificInput(CreateHostedCheckoutRequest hostedCheckoutRequest) {
        final RedirectPaymentMethodSpecificInput redirectPaymentMethodSpecificInput = hostedCheckoutRequest.getRedirectPaymentMethodSpecificInput();
        if (redirectPaymentMethodSpecificInput != null && redirectPaymentMethodSpecificInput.getRedirectionData() != null) {
            // See getHostedCheckoutSpecificInput: no returnUrl for Pay by Link.
            redirectPaymentMethodSpecificInput.getRedirectionData().setReturnUrl(null);
        }
        return redirectPaymentMethodSpecificInput;
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

    public void setDisplayQRCode(boolean displayQRCode) {
        this.displayQRCode = displayQRCode;
    }
}
