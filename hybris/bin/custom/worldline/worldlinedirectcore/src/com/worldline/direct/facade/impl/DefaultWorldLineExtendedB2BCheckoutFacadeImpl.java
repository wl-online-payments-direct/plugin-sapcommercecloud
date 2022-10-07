package com.worldline.direct.facade.impl;

import com.worldline.direct.service.WorldlineConfigurationService;
import com.worldline.direct.service.WorldlineScheduleOrderService;
import de.hybris.platform.b2bacceleratorfacades.checkout.data.PlaceOrderData;
import de.hybris.platform.b2bacceleratorfacades.exception.EntityValidationException;
import de.hybris.platform.b2bacceleratorfacades.order.data.B2BCommentData;
import de.hybris.platform.b2bacceleratorfacades.order.data.B2BReplenishmentRecurrenceEnum;
import de.hybris.platform.b2bacceleratorfacades.order.data.ScheduledCartData;
import de.hybris.platform.b2bacceleratorfacades.order.data.TriggerData;
import de.hybris.platform.b2bacceleratorfacades.order.impl.DefaultB2BCheckoutFacade;
import de.hybris.platform.b2bacceleratorservices.enums.CheckoutPaymentType;
import de.hybris.platform.commercefacades.order.data.AbstractOrderData;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.payment.PaymentInfoModel;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.cronjob.model.TriggerModel;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.orderscheduling.model.CartToOrderCronJobModel;
import de.hybris.platform.servicelayer.keygenerator.KeyGenerator;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

import static de.hybris.platform.util.localization.Localization.getLocalizedString;

public class DefaultWorldLineExtendedB2BCheckoutFacadeImpl extends DefaultB2BCheckoutFacade {
    public static final Logger LOG = LoggerFactory.getLogger(DefaultWorldLineExtendedB2BCheckoutFacadeImpl.class);
    private static final String CART_CHECKOUT_DELIVERYADDRESS_INVALID = "cart.deliveryAddress.invalid";
    private static final String CART_CHECKOUT_DELIVERYMODE_INVALID = "cart.deliveryMode.invalid";
    private static final String CART_CHECKOUT_PAYMENTINFO_EMPTY = "cart.paymentInfo.empty";
    private static final String CART_CHECKOUT_NOT_CALCULATED = "cart.not.calculated";
    private static final String CART_CHECKOUT_NO_QUOTE_DESCRIPTION = "cart.no.quote.description";
    private static final String CART_CHECKOUT_QUOTE_REQUIREMENTS_NOT_SATISFIED = "cart.quote.requirements.not.satisfied";
    private static final String CART_CHECKOUT_REPLENISHMENT_NO_STARTDATE = "cart.replenishment.no.startdate";
    private static final String CART_CHECKOUT_REPLENISHMENT_NO_FREQUENCY = "cart.replenishment.no.frequency";
    private KeyGenerator guidKeyGenerator;
    private WorldlineScheduleOrderService worldlineScheduleOrderService;
    private WorldlineConfigurationService worldlineConfigurationService;


    @Override
    public <T extends AbstractOrderData> T placeOrder(PlaceOrderData placeOrderData) throws InvalidCartException {
        if (isValidCheckoutCart(placeOrderData)) {
            // validate quote negotiation
            if (placeOrderData.getNegotiateQuote() != null && placeOrderData.getNegotiateQuote().equals(Boolean.TRUE)) {
                if (StringUtils.isBlank(placeOrderData.getQuoteRequestDescription())) {
                    throw new EntityValidationException(getLocalizedString(CART_CHECKOUT_NO_QUOTE_DESCRIPTION));
                } else {
                    final B2BCommentData b2BComment = new B2BCommentData();
                    b2BComment.setComment(placeOrderData.getQuoteRequestDescription());

                    final CartData cartData = new CartData();
                    cartData.setB2BComment(b2BComment);

                    updateCheckoutCart(cartData);
                }
            }

            // validate replenishment
            if (BooleanUtils.isTrue(placeOrderData.getReplenishmentOrder())) {
                if (placeOrderData.getReplenishmentStartDate() == null) {
                    throw new EntityValidationException(getLocalizedString(CART_CHECKOUT_REPLENISHMENT_NO_STARTDATE));
                }

                if (placeOrderData.getReplenishmentRecurrence().equals(B2BReplenishmentRecurrenceEnum.WEEKLY)
                        && CollectionUtils.isEmpty(placeOrderData.getNDaysOfWeek())) {
                    throw new EntityValidationException(getLocalizedString(CART_CHECKOUT_REPLENISHMENT_NO_FREQUENCY));
                }

                final TriggerData triggerData = new TriggerData();
                populateTriggerDataFromPlaceOrderData(placeOrderData, triggerData);
                final CartModel cartModel = getCart();
                final boolean cardPaymentType = CheckoutPaymentType.CARD.getCode().equals(cartModel.getPaymentType().getCode());

                if (worldlineConfigurationService.getCurrentWorldlineConfiguration().isFirstRecurringPayment() && BooleanUtils.isTrue(cardPaymentType)) {
                    return (T) scheduleOrderAndPlaceOrder(triggerData);
                } else {
                    return (T) scheduleOrder(triggerData);
                }
            }

            return (T) super.placeOrder();
        }

        return null;
    }

    private OrderData scheduleOrderAndPlaceOrder(TriggerData trigger) throws InvalidCartException {
        final CartModel cartModel = getCart();
        cartModel.setSite(getBaseSiteService().getCurrentBaseSite());
        cartModel.setStore(getBaseStoreService().getCurrentBaseStore());
        getModelService().save(cartModel);

        final AddressModel deliveryAddress = cartModel.getDeliveryAddress();
        final AddressModel paymentAddress = cartModel.getPaymentAddress();
        final PaymentInfoModel paymentInfo = cartModel.getPaymentInfo();
        final TriggerModel triggerModel = getModelService().create(TriggerModel.class);
        getTriggerPopulator().populate(trigger, triggerModel);

        // If Trigger is not relative, reset activeDate to next expected runtime
        if (BooleanUtils.isFalse(triggerModel.getRelative())) {
            // getNextTime(relavtiveDate) will skip the date, to avoid skipping the activation date, go back 1 day to test.
            final Calendar priorDayCalendar = Calendar.getInstance();
            priorDayCalendar.setTime(DateUtils.addDays(triggerModel.getActivationTime(), -1));

            final Date nextPotentialFire = getTriggerService().getNextTime(triggerModel, priorDayCalendar).getTime();

            if (!DateUtils.isSameDay(nextPotentialFire, triggerModel.getActivationTime())) {
                // Adjust activation time to next scheduled vis a vis the cron expression
                triggerModel.setActivationTime(nextPotentialFire);
            }
        }
        triggerModel.setActive(false);
        final CartToOrderCronJobModel scheduledCart = worldlineScheduleOrderService.createOrderFromCartCronJob(cartModel,
                deliveryAddress, paymentAddress, paymentInfo, Collections.singletonList(triggerModel));

        return placeFirstRecurringOrder(scheduledCart);
    }

    @Override
    protected boolean isValidCheckoutCart(PlaceOrderData placeOrderData) {
        final CartData cartData = getCheckoutCart();
        final boolean valid = true;

        if (!cartData.isCalculated()) {
            throw new EntityValidationException(getLocalizedString(CART_CHECKOUT_NOT_CALCULATED));
        }

        if (cartData.getDeliveryAddress() == null) {
            throw new EntityValidationException(getLocalizedString(CART_CHECKOUT_DELIVERYADDRESS_INVALID));
        }

        if (cartData.getDeliveryMode() == null) {
            throw new EntityValidationException(getLocalizedString(CART_CHECKOUT_DELIVERYMODE_INVALID));
        }

        final boolean accountPaymentType = CheckoutPaymentType.ACCOUNT.getCode().equals(cartData.getPaymentType().getCode());
        if (!accountPaymentType && cartData.getWorldlinePaymentInfo() == null) {
            throw new EntityValidationException(getLocalizedString(CART_CHECKOUT_PAYMENTINFO_EMPTY));
        }

        if (Boolean.TRUE.equals(placeOrderData.getNegotiateQuote()) && !cartData.getQuoteAllowed()) {
            throw new EntityValidationException(getLocalizedString(CART_CHECKOUT_QUOTE_REQUIREMENTS_NOT_SATISFIED));
        }

        return valid;
    }

    @Override
    protected void afterPlaceOrder(@SuppressWarnings("unused") final CartModel cartModel, final OrderModel orderModel) //NOSONAR
    {
        final boolean cardPaymentType = CheckoutPaymentType.CARD.getCode().equals(cartModel.getPaymentType().getCode());
        if (orderModel != null) {
            orderModel.setGuid(guidKeyGenerator.generate().toString());
            if (!cardPaymentType) {
                getCartService().removeSessionCart();
                getCartService().getSessionCart();
            }
            getModelService().save(orderModel);
            getModelService().refresh(orderModel);
        }
    }

    @Override
    public ScheduledCartData scheduleOrder(final TriggerData trigger) {
        final CartModel cartModel = getCart();
        cartModel.setSite(getBaseSiteService().getCurrentBaseSite());
        cartModel.setStore(getBaseStoreService().getCurrentBaseStore());
        getModelService().save(cartModel);

        final AddressModel deliveryAddress = cartModel.getDeliveryAddress();
        final AddressModel paymentAddress = cartModel.getPaymentAddress();
        final PaymentInfoModel paymentInfo = cartModel.getPaymentInfo();
        final TriggerModel triggerModel = getModelService().create(TriggerModel.class);
        getTriggerPopulator().populate(trigger, triggerModel);

        // If Trigger is not relative, reset activeDate to next expected runtime
        if (BooleanUtils.isFalse(triggerModel.getRelative())) {
            // getNextTime(relavtiveDate) will skip the date, to avoid skipping the activation date, go back 1 day to test.
            final Calendar priorDayCalendar = Calendar.getInstance();
            priorDayCalendar.setTime(DateUtils.addDays(triggerModel.getActivationTime(), -1));

            final Date nextPotentialFire = getTriggerService().getNextTime(triggerModel, priorDayCalendar).getTime();

            if (!DateUtils.isSameDay(nextPotentialFire, triggerModel.getActivationTime())) {
                // Adjust activation time to next scheduled vis a vis the cron expression
                triggerModel.setActivationTime(nextPotentialFire);
            }
        }
        final boolean cardPaymentType = CheckoutPaymentType.CARD.getCode().equals(cartModel.getPaymentType().getCode());
        if (cardPaymentType) {
            triggerModel.setActive(false);
        }
        // schedule cart
        final CartToOrderCronJobModel scheduledCart = worldlineScheduleOrderService.createOrderFromCartCronJob(cartModel,
                deliveryAddress, paymentAddress, paymentInfo, Collections.singletonList(triggerModel));

        ScheduledCartData scheduledCartData = null;
        if (scheduledCart != null) {
            scheduledCartData = getScheduledCartConverter().convert(scheduledCart);
            if (!cardPaymentType) {
                getCartService().removeSessionCart();
                getCartService().getSessionCart();
                // trigger an email.
                getEventService().publishEvent(initializeReplenishmentPlacedEvent(scheduledCart));
            }
        }


        return scheduledCartData;
    }

    private OrderData placeFirstRecurringOrder(CartToOrderCronJobModel scheduledCart) throws InvalidCartException {
            OrderData orderData = super.placeOrder();
            final OrderModel orderModel = getCustomerAccountService().getOrderForCode(orderData.getCode(), getBaseStoreService().getCurrentBaseStore());
            orderModel.setSchedulingCronJob(scheduledCart);
            getModelService().save(orderModel);
            return getOrderConverter().convert(orderModel);
    }


    @Required
    public void setGuidKeyGenerator(KeyGenerator guidKeyGenerator) {
        this.guidKeyGenerator = guidKeyGenerator;
    }

    @Required
    public void setWorldlineScheduleOrderService(WorldlineScheduleOrderService worldlineScheduleOrderService) {
        this.worldlineScheduleOrderService = worldlineScheduleOrderService;
    }
    @Required
    public void setWorldlineConfigurationService(WorldlineConfigurationService worldlineConfigurationService) {
        this.worldlineConfigurationService = worldlineConfigurationService;
    }

}
