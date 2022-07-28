package com.worldline.direct.facade.impl;

import com.onlinepayments.domain.CreateHostedCheckoutResponse;
import com.onlinepayments.domain.GetHostedCheckoutResponse;
import com.onlinepayments.domain.PaymentResponse;
import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import com.worldline.direct.exception.WorldlineNonAuthorizedPaymentException;
import com.worldline.direct.facade.WorldlineRecurringCheckoutFacade;
import com.worldline.direct.order.data.BrowserData;
import com.worldline.direct.service.WorldlineB2BPaymentService;
import com.worldline.direct.service.WorldlineCartToOrderService;
import com.worldline.direct.util.WorldlinePaymentProductUtils;
import com.worldline.direct.util.WorldlineUrlUtils;
import de.hybris.platform.b2bacceleratorfacades.order.data.ScheduledCartData;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.orderscheduling.model.CartToOrderCronJobModel;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorldlineRecurringCheckoutFacadeImpl extends WorldlineCheckoutFacadeImpl implements WorldlineRecurringCheckoutFacade {
    private final Logger LOGGER = LoggerFactory.getLogger(WorldlineRecurringCheckoutFacadeImpl.class);
    private WorldlineB2BPaymentService worldlineB2BPaymentService;
    private Converter<CartToOrderCronJobModel, ScheduledCartData> scheduledCartConverter;
    private WorldlineCartToOrderService worldlineCartToOrderService;

    @Override
    public CreateHostedCheckoutResponse createReplenishmentHostedCheckout(String jobCode, BrowserData browserData) throws InvalidCartException {
        CartToOrderCronJobModel cartToOrderCronJob = worldlineCustomerAccountService.getCartToOrderCronJob(jobCode);
        CartModel cart = cartToOrderCronJob.getCart();
        final CreateHostedCheckoutResponse hostedCheckout = worldlineB2BPaymentService.createRecurringHostedCheckout(cartToOrderCronJob, browserData);
        storeReturnMac(cart, hostedCheckout.getRETURNMAC());
        hostedCheckout.setPartialRedirectUrl(WorldlineUrlUtils.buildFullURL(hostedCheckout.getPartialRedirectUrl()));
        return hostedCheckout;
    }


    @Override
    public ScheduledCartData authorisePaymentForReplenishmentHostedCheckout(String jobCode, String hostedCheckoutId) throws WorldlineNonAuthorizedPaymentException, InvalidCartException {
        GetHostedCheckoutResponse hostedCheckoutData = worldlinePaymentService.getHostedCheckout(hostedCheckoutId);
        CartToOrderCronJobModel cartToOrderCronJob = worldlineCustomerAccountService.getCartToOrderCronJob(jobCode);
        switch (WorldlinedirectcoreConstants.HOSTED_CHECKOUT_STATUS_ENUM.valueOf(hostedCheckoutData.getStatus())) {
            case CANCELLED_BY_CONSUMER:
            case CLIENT_NOT_ELIGIBLE_FOR_SELECTED_PAYMENT_PRODUCT:
                // TODO : check if we need to cancel the job / delete it
                worldlineCartToOrderService.cancelCartToOrderJob(cartToOrderCronJob);
                throw new WorldlineNonAuthorizedPaymentException(WorldlinedirectcoreConstants.UNAUTHORIZED_REASON.CANCELLED);
            case IN_PROGRESS:
                throw new WorldlineNonAuthorizedPaymentException(WorldlinedirectcoreConstants.UNAUTHORIZED_REASON.IN_PROGRESS);
            case PAYMENT_CREATED:
                return handlePaymentResponse(cartToOrderCronJob, hostedCheckoutData.getCreatedPaymentOutput().getPayment());
            default:
                LOGGER.error("Unexpected HostedCheckout Status value: " + hostedCheckoutData.getStatus());
                throw new IllegalStateException("Unexpected HostedCheckout Status value: " + hostedCheckoutData.getStatus());
        }
    }


    protected ScheduledCartData handlePaymentResponse(CartToOrderCronJobModel cartToOrderCronJobModel, PaymentResponse paymentResponse) throws WorldlineNonAuthorizedPaymentException, InvalidCartException {
        worldlineCartToOrderService.enableCartToOrderJob(cartToOrderCronJobModel, paymentResponse);
        saveMandate(cartToOrderCronJobModel, paymentResponse);
        cartService.removeSessionCart();
        cartService.getSessionCart();
        return scheduledCartConverter.convert(cartToOrderCronJobModel);
    }

    private void saveMandate(CartToOrderCronJobModel cartToOrderCronJobModel, PaymentResponse paymentResponse) {
        WorldlinePaymentInfoModel paymentInfo = (WorldlinePaymentInfoModel) cartToOrderCronJobModel.getPaymentInfo();
        if (WorldlinePaymentProductUtils.isPaymentBySepaDirectDebit(paymentInfo) && paymentResponse.getPaymentOutput().getSepaDirectDebitPaymentMethodSpecificOutput() != null && paymentResponse.getPaymentOutput().getSepaDirectDebitPaymentMethodSpecificOutput().getPaymentProduct771SpecificOutput() != null) {
            paymentInfo.setMandate(paymentResponse.getPaymentOutput().getSepaDirectDebitPaymentMethodSpecificOutput().getPaymentProduct771SpecificOutput().getMandateReference());
            modelService.save(paymentInfo);
            modelService.refresh(cartToOrderCronJobModel);
        }
    }

    public void setWorldlineB2BPaymentService(WorldlineB2BPaymentService worldlineB2BPaymentService) {
        this.worldlineB2BPaymentService = worldlineB2BPaymentService;
    }

    public void setScheduledCartConverter(Converter<CartToOrderCronJobModel, ScheduledCartData> scheduledCartConverter) {
        this.scheduledCartConverter = scheduledCartConverter;
    }

    public void setWorldlineCartToOrderService(WorldlineCartToOrderService worldlineCartToOrderService) {
        this.worldlineCartToOrderService = worldlineCartToOrderService;
    }
}
