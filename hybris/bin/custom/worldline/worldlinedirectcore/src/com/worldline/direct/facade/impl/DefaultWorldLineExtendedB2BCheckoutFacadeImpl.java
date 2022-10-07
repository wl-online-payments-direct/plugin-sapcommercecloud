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
import de.hybris.platform.util.StandardDateRange;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import java.util.*;

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

                final List<TriggerData> triggerDataListData = new ArrayList<>();
                populateTriggerDataFromPlaceOrderData(placeOrderData, triggerDataListData);
                final CartModel cartModel = getCart();
                final boolean cardPaymentType = CheckoutPaymentType.CARD.getCode().equals(cartModel.getPaymentType().getCode());

                if (worldlineConfigurationService.getCurrentWorldlineConfiguration().isFirstRecurringPayment() && BooleanUtils.isTrue(cardPaymentType)) {
                    return (T) scheduleOrderAndPlaceOrder(triggerDataListData);
                } else {
                    return (T) scheduleOrder(triggerDataListData);
                }
            }

            return (T) super.placeOrder();
        }

        return null;
    }

    private OrderData scheduleOrderAndPlaceOrder(List<TriggerData> triggerDataList) throws InvalidCartException {
        final CartModel cartModel = getCart();
        cartModel.setSite(getBaseSiteService().getCurrentBaseSite());
        cartModel.setStore(getBaseStoreService().getCurrentBaseStore());
        getModelService().save(cartModel);

        final AddressModel deliveryAddress = cartModel.getDeliveryAddress();
        final AddressModel paymentAddress = cartModel.getPaymentAddress();
        final PaymentInfoModel paymentInfo = cartModel.getPaymentInfo();
        List<TriggerModel> triggerModelList = createTriggers(triggerDataList);
        final CartToOrderCronJobModel scheduledCart = worldlineScheduleOrderService.createOrderFromCartCronJob(cartModel,
                deliveryAddress, paymentAddress, paymentInfo, triggerModelList);

        OrderData orderData = placeFirstRecurringOrder(scheduledCart);
        return orderData;
    }

    private List<TriggerModel> createTriggers(List<TriggerData> triggerDataList) {
        List<TriggerModel> triggerModelList = new ArrayList<>();
        for (TriggerData triggerData : triggerDataList) {
            final TriggerModel triggerModel = getModelService().create(TriggerModel.class);
            getTriggerPopulator().populate(triggerData, triggerModel);


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
            triggerModelList.add(triggerModel);
        }
        return triggerModelList;
    }

    private TriggerData createTrigger(PlaceOrderData placeOrderData) {
        TriggerData triggerData = new TriggerData();


        final Date replenishmentStartDate = placeOrderData.getReplenishmentStartDate();
        final Calendar calendarStartTime = Calendar.getInstance(getI18NService().getCurrentTimeZone(), getI18NService().getCurrentLocale());
        triggerData
                .setActivationTime((replenishmentStartDate.before(calendarStartTime.getTime()) ? calendarStartTime.getTime() : replenishmentStartDate));

        final Date replenishmentEndDate = placeOrderData.getReplenishmentEndDate();
        if (Objects.nonNull(replenishmentEndDate) && replenishmentStartDate.before(replenishmentEndDate)) {
            triggerData.setDateRange(new StandardDateRange(replenishmentStartDate, replenishmentEndDate));
        }
        return triggerData;
    }

    protected void populateTriggerDataFromPlaceOrderData(final PlaceOrderData placeOrderData, final List<TriggerData> triggerDataList) {
        final B2BReplenishmentRecurrenceEnum recurrenceValue = placeOrderData.getReplenishmentRecurrence();
        switch (recurrenceValue) {
            case DAILY: {
                TriggerData triggerData = createTrigger(placeOrderData);
                triggerData.setDay(Integer.valueOf(placeOrderData.getNDays()));
                triggerData.setRelative(Boolean.TRUE);
                triggerDataList.add(triggerData);
                break;
            }
            case WEEKLY: {
                TriggerData triggerData = createTrigger(placeOrderData);
                triggerData.setDaysOfWeek(placeOrderData.getNDaysOfWeek());
                triggerData.setWeekInterval(Integer.valueOf(placeOrderData.getNWeeks()));
                triggerData.setHour(Integer.valueOf(0));
                triggerData.setMinute(Integer.valueOf(0));
                triggerDataList.add(triggerData);
                break;
            }
            case MONTHLY: {
                final Date replenishmentStartDate = placeOrderData.getReplenishmentStartDate();
                final Calendar calendarStartTime = Calendar.getInstance(getI18NService().getCurrentTimeZone(), getI18NService().getCurrentLocale());
                Date nextActivationDate = replenishmentStartDate.before(calendarStartTime.getTime()) ? calendarStartTime.getTime() : replenishmentStartDate;
                calendarStartTime.setTime(nextActivationDate);
                List<Integer> monthsOptions = getMonthsOptions(Integer.parseInt(placeOrderData.getNMonths()), calendarStartTime.get(Calendar.MONTH));
                monthsOptions.stream().forEach(month -> {
                    TriggerData triggerData = createTrigger(placeOrderData);
                    triggerData.setDay(Integer.valueOf(placeOrderData.getNthDayOfMonth()));
                    triggerData.setMonth(month);
                    triggerData.setRelative(Boolean.FALSE);
                    triggerDataList.add(triggerData);
                });
                break;
            }
            case YEARLY: {
                TriggerData triggerData = createTrigger(placeOrderData);
                triggerData.setYear(1);
                triggerData.setRelative(Boolean.TRUE);
                triggerDataList.add(triggerData);
                break;
            }
        }
    }

    private List<Integer> getMonthsOptions(Integer nMonths, Integer currentMonth) {
//nMonths == 1,2,3,4,6
        List<Integer> months = new ArrayList<>();
        if (nMonths != 1) {


            int remainderMonth = currentMonth % nMonths;
            for (int nbMonth = 0; nbMonth < 12; nbMonth++) {
                if (nbMonth % nMonths == remainderMonth) {
                    months.add(nbMonth);
                }
            }
        } else {
            months.add(-1);
        }
        return months;
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

    public ScheduledCartData scheduleOrder(final List<TriggerData> triggerDataList) {
        final CartModel cartModel = getCart();
        cartModel.setSite(getBaseSiteService().getCurrentBaseSite());
        cartModel.setStore(getBaseStoreService().getCurrentBaseStore());
        getModelService().save(cartModel);

        final AddressModel deliveryAddress = cartModel.getDeliveryAddress();
        final AddressModel paymentAddress = cartModel.getPaymentAddress();
        final PaymentInfoModel paymentInfo = cartModel.getPaymentInfo();
        List<TriggerModel> triggers = createTriggers(triggerDataList);

        final boolean cardPaymentType = CheckoutPaymentType.CARD.getCode().equals(cartModel.getPaymentType().getCode());
        // schedule cart
        final CartToOrderCronJobModel scheduledCart = worldlineScheduleOrderService.createOrderFromCartCronJob(cartModel,
                deliveryAddress, paymentAddress, paymentInfo, triggers);

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
