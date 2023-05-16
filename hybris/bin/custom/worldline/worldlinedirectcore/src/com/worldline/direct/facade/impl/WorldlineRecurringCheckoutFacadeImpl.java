package com.worldline.direct.facade.impl;

import com.onlinepayments.domain.CreateHostedCheckoutResponse;
import com.onlinepayments.domain.GetHostedCheckoutResponse;
import com.onlinepayments.domain.PaymentResponse;
import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import com.worldline.direct.enums.RecurringPaymentEnum;
import com.worldline.direct.enums.WorldlineCheckoutTypesEnum;
import com.worldline.direct.exception.WorldlineNonAuthorizedPaymentException;
import com.worldline.direct.facade.WorldlineRecurringCheckoutFacade;
import com.worldline.direct.order.data.BrowserData;
import com.worldline.direct.service.WorldlineB2BPaymentService;
import com.worldline.direct.service.WorldlineCartToOrderService;
import com.worldline.direct.util.WorldlineUrlUtils;
import de.hybris.platform.b2bacceleratorfacades.order.data.ScheduledCartData;
import de.hybris.platform.commercefacades.order.data.AbstractOrderData;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.orderscheduling.model.CartToOrderCronJobModel;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorldlineRecurringCheckoutFacadeImpl extends WorldlineCheckoutFacadeImpl implements WorldlineRecurringCheckoutFacade {
    private final Logger LOGGER = LoggerFactory.getLogger(WorldlineRecurringCheckoutFacadeImpl.class);
    private WorldlineB2BPaymentService worldlineB2BPaymentService;
    private Converter<CartToOrderCronJobModel, ScheduledCartData> scheduledCartConverter;
    private WorldlineCartToOrderService worldlineCartToOrderService;

    @Override
    public CreateHostedCheckoutResponse createReplenishmentHostedCheckout(AbstractOrderData abstractOrderData, BrowserData browserData, RecurringPaymentEnum recurringPaymentType) throws InvalidCartException {
        final CreateHostedCheckoutResponse hostedCheckout;
        switch (recurringPaymentType) {
            case IMMEDIATE:
                OrderModel orderModel = customerAccountService.getOrderForCode(abstractOrderData.getCode(), baseStoreService.getCurrentBaseStore());
                hostedCheckout = worldlineB2BPaymentService.createImmediateRecurringOrderHostedCheckout(orderModel, browserData);
                storeReturnMac(orderModel, hostedCheckout.getRETURNMAC());

                break;
            case SCHEDULED:
            default:
                CartToOrderCronJobModel cartToOrderCronJob = worldlineCustomerAccountService.getCartToOrderCronJob(((ScheduledCartData)abstractOrderData).getJobCode());
                CartModel cart = cartToOrderCronJob.getCart();
                hostedCheckout = worldlineB2BPaymentService.createScheduledRecurringOrderHostedCheckout(cartToOrderCronJob, browserData);
                storeReturnMac(cart, hostedCheckout.getRETURNMAC());

                break;
        }
        hostedCheckout.setPartialRedirectUrl(WorldlineUrlUtils.buildFullURL(hostedCheckout.getPartialRedirectUrl()));
        return hostedCheckout;
    }


    @Override
    public ScheduledCartData authorisePaymentForSchudledReplenishmentHostedCheckout(String jobCode, String hostedCheckoutId) throws WorldlineNonAuthorizedPaymentException, InvalidCartException {
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
                saveOrUpdatePaymentToken(cartToOrderCronJob.getCart(), hostedCheckoutData, Boolean.TRUE);
                saveMandateIfNeeded(cartToOrderCronJob.getCart().getStore().getWorldlineConfiguration(), (WorldlinePaymentInfoModel) cartToOrderCronJob.getPaymentInfo(), hostedCheckoutData.getCreatedPaymentOutput().getPayment());
                return handlePaymentResponse(cartToOrderCronJob);
            default:
                LOGGER.error("Unexpected HostedCheckout Status value: " + hostedCheckoutData.getStatus());
                throw new IllegalStateException("Unexpected HostedCheckout Status value: " + hostedCheckoutData.getStatus());
        }
    }

    @Override
    public ScheduledCartData authorisePaymentForImmediateReplenishmentHostedCheckout(String orderCode, String hostedCheckoutId) throws WorldlineNonAuthorizedPaymentException, InvalidCartException {
        super.authorisePaymentForHostedCheckout(orderCode, hostedCheckoutId, Boolean.TRUE);
        OrderModel order = customerAccountService.getOrderForCode(orderCode, baseStoreService.getCurrentBaseStore());
        CartToOrderCronJobModel schedulingCronJob = order.getSchedulingCronJob();
        WorldlinePaymentInfoModel orderPaymentInfo = (WorldlinePaymentInfoModel) order.getPaymentInfo();
        WorldlinePaymentInfoModel recurringPaymentInfo = (WorldlinePaymentInfoModel) schedulingCronJob.getPaymentInfo();
        recurringPaymentInfo.setMandateDetail(orderPaymentInfo.getMandateDetail()); // if recurring payment should be done by SEPA DD
        recurringPaymentInfo.setToken(orderPaymentInfo.getToken()); // if recurring payment should be done by card
        modelService.save(recurringPaymentInfo);
        worldlineCartToOrderService.enableCartToOrderJob(schedulingCronJob,false);
        return scheduledCartConverter.convert(schedulingCronJob);
    }

    protected ScheduledCartData handlePaymentResponse(CartToOrderCronJobModel cartToOrderCronJobModel) {
        worldlineCartToOrderService.enableCartToOrderJob(cartToOrderCronJobModel, true);
        cartService.removeSessionCart();
        cartService.getSessionCart();
        return scheduledCartConverter.convert(cartToOrderCronJobModel);
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
