package com.worldline.direct.facade.impl;

import com.worldline.direct.facade.WorldlineDirectCheckoutFacade;
import com.worldline.direct.service.WorldlineConfigurationService;
import com.worldline.direct.service.WorldlineScheduleOrderService;
import de.hybris.platform.b2bacceleratorfacades.checkout.data.PlaceOrderData;
import de.hybris.platform.b2bacceleratorfacades.exception.EntityValidationException;
import de.hybris.platform.b2bacceleratorfacades.order.data.B2BDaysOfWeekData;
import de.hybris.platform.b2bacceleratorfacades.order.data.B2BReplenishmentRecurrenceEnum;
import de.hybris.platform.b2bacceleratorfacades.order.data.ScheduledCartData;
import de.hybris.platform.b2bacceleratorfacades.order.data.TriggerData;
import de.hybris.platform.b2bacceleratorservices.enums.CheckoutPaymentType;
import de.hybris.platform.commercefacades.order.data.AbstractOrderData;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commercefacades.order.impl.DefaultCheckoutFacade;
import de.hybris.platform.converters.Converters;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.payment.PaymentInfoModel;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.cronjob.enums.DayOfWeek;
import de.hybris.platform.cronjob.model.TriggerModel;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.orderscheduling.model.CartToOrderCronJobModel;
import de.hybris.platform.servicelayer.cronjob.TriggerService;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.servicelayer.i18n.I18NService;
import de.hybris.platform.servicelayer.keygenerator.KeyGenerator;
import de.hybris.platform.site.BaseSiteService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static de.hybris.platform.util.localization.Localization.getLocalizedString;

public class WorldLineExtendedB2CCheckoutFacadeImpl extends DefaultCheckoutFacade implements WorldlineDirectCheckoutFacade {
    private static final String CART_CHECKOUT_REPLENISHMENT_NO_STARTDATE = "cart.replenishment.no.startdate";
    private static final String CART_CHECKOUT_REPLENISHMENT_NO_FREQUENCY = "cart.replenishment.no.frequency";
    public static final Logger LOG = LoggerFactory.getLogger(WorldLineExtendedB2CCheckoutFacadeImpl.class);

    private KeyGenerator guidKeyGenerator;
    private I18NService i18NService;
    private BaseSiteService baseSiteService;
    private TriggerService triggerService;
    private WorldlineScheduleOrderService worldlineScheduleOrderService;
    private Converter<CartToOrderCronJobModel, ScheduledCartData> scheduledCartConverter;
    private Populator<TriggerData, TriggerModel> triggerPopulator;
    private Converter<DayOfWeek, B2BDaysOfWeekData> b2bDaysOfWeekConverter;
    private WorldlineConfigurationService worldlineConfigurationService;


    @Override
    protected void afterPlaceOrder(@SuppressWarnings("unused") final CartModel cartModel, final OrderModel orderModel) //NOSONAR
    {
        if (orderModel != null) {
            orderModel.setGuid(guidKeyGenerator.generate().toString());
            getModelService().save(orderModel);
            getModelService().refresh(orderModel);
        }
    }

    public void setGuidKeyGenerator(KeyGenerator guidKeyGenerator) {
        this.guidKeyGenerator = guidKeyGenerator;
    }

    @Override
    public <T extends AbstractOrderData> T placeOrder(PlaceOrderData placeOrderData) throws InvalidCartException {
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

    private OrderData scheduleOrderAndPlaceOrder(TriggerData trigger) throws InvalidCartException {
        final CartModel cartModel = getCart();
        cartModel.setSite(baseSiteService.getCurrentBaseSite());
        cartModel.setStore(getBaseStoreService().getCurrentBaseStore());
        getModelService().save(cartModel);

        final AddressModel deliveryAddress = cartModel.getDeliveryAddress();
        final AddressModel paymentAddress = cartModel.getPaymentAddress();
        final PaymentInfoModel paymentInfo = cartModel.getPaymentInfo();
        final TriggerModel triggerModel = getModelService().create(TriggerModel.class);
        triggerPopulator.populate(trigger, triggerModel);

        // If Trigger is not relative, reset activeDate to next expected runtime
        if (BooleanUtils.isFalse(triggerModel.getRelative())) {
            // getNextTime(relavtiveDate) will skip the date, to avoid skipping the activation date, go back 1 day to test.
            final Calendar priorDayCalendar = Calendar.getInstance();
            priorDayCalendar.setTime(DateUtils.addDays(triggerModel.getActivationTime(), -1));

            final Date nextPotentialFire = triggerService.getNextTime(triggerModel, priorDayCalendar).getTime();

            if (!DateUtils.isSameDay(nextPotentialFire, triggerModel.getActivationTime())) {
                // Adjust activation time to next scheduled vis a vis the cron expression
                triggerModel.setActivationTime(nextPotentialFire);
            }
        }
        triggerModel.setActive(false);
        final CartToOrderCronJobModel scheduledCart = worldlineScheduleOrderService.createOrderFromCartCronJob(cartModel,
                deliveryAddress, paymentAddress, paymentInfo, Collections.singletonList(triggerModel));

        OrderData orderData = placeFirstRecurringOrder(scheduledCart);
        return orderData;
    }


    protected void populateTriggerDataFromPlaceOrderData(final PlaceOrderData placeOrderData, final TriggerData triggerData) {
        final Date replenishmentStartDate = placeOrderData.getReplenishmentStartDate();
        final Calendar calendar = Calendar.getInstance(i18NService.getCurrentTimeZone(), i18NService.getCurrentLocale());
        triggerData
                .setActivationTime((replenishmentStartDate.before(calendar.getTime()) ? calendar.getTime() : replenishmentStartDate));

        final B2BReplenishmentRecurrenceEnum recurrenceValue = placeOrderData.getReplenishmentRecurrence();

        if (B2BReplenishmentRecurrenceEnum.DAILY.equals(recurrenceValue)) {
            triggerData.setDay(Integer.valueOf(placeOrderData.getNDays()));
            triggerData.setRelative(Boolean.TRUE);
        } else if (B2BReplenishmentRecurrenceEnum.WEEKLY.equals(recurrenceValue)) {
            triggerData.setDaysOfWeek(placeOrderData.getNDaysOfWeek());
            triggerData.setWeekInterval(Integer.valueOf(placeOrderData.getNWeeks()));
            triggerData.setHour(Integer.valueOf(0));
            triggerData.setMinute(Integer.valueOf(0));
        } else if (B2BReplenishmentRecurrenceEnum.MONTHLY.equals(recurrenceValue)) {
            triggerData.setDay(Integer.valueOf(placeOrderData.getNthDayOfMonth()));
            triggerData.setRelative(Boolean.FALSE);
        }
    }

    public ScheduledCartData scheduleOrder(final TriggerData trigger) {
        final CartModel cartModel = getCart();
        cartModel.setSite(baseSiteService.getCurrentBaseSite());
        cartModel.setStore(getBaseStoreService().getCurrentBaseStore());
        getModelService().save(cartModel);

        final AddressModel deliveryAddress = cartModel.getDeliveryAddress();
        final AddressModel paymentAddress = cartModel.getPaymentAddress();
        final PaymentInfoModel paymentInfo = cartModel.getPaymentInfo();
        final TriggerModel triggerModel = getModelService().create(TriggerModel.class);
        triggerPopulator.populate(trigger, triggerModel);

        // If Trigger is not relative, reset activeDate to next expected runtime
        if (BooleanUtils.isFalse(triggerModel.getRelative())) {
            // getNextTime(relavtiveDate) will skip the date, to avoid skipping the activation date, go back 1 day to test.
            final Calendar priorDayCalendar = Calendar.getInstance();
            priorDayCalendar.setTime(DateUtils.addDays(triggerModel.getActivationTime(), -1));

            final Date nextPotentialFire = triggerService.getNextTime(triggerModel, priorDayCalendar).getTime();

            if (!DateUtils.isSameDay(nextPotentialFire, triggerModel.getActivationTime())) {
                // Adjust activation time to next scheduled vis a vis the cron expression
                triggerModel.setActivationTime(nextPotentialFire);
            }
        }
        triggerModel.setActive(false);
        // schedule cart
        final CartToOrderCronJobModel scheduledCart = worldlineScheduleOrderService.createOrderFromCartCronJob(cartModel,
                deliveryAddress, paymentAddress, paymentInfo, Collections.singletonList(triggerModel));

        ScheduledCartData scheduledCartData = null;
        if (scheduledCart != null) {
            scheduledCartData = scheduledCartConverter.convert(scheduledCart);
        }

        return scheduledCartData;
    }


    @Override
    public List<B2BDaysOfWeekData> getDaysOfWeekForReplenishmentCheckoutSummary() {
        final List<DayOfWeek> daysOfWeek = getEnumerationService().getEnumerationValues(DayOfWeek._TYPECODE);

        return Converters.convertAll(daysOfWeek, b2bDaysOfWeekConverter);
    }

    private OrderData placeFirstRecurringOrder(CartToOrderCronJobModel scheduledCart) throws InvalidCartException {
        OrderData orderData = super.placeOrder();
        final OrderModel orderModel = getCustomerAccountService().getOrderForCode(orderData.getCode(), getBaseStoreService().getCurrentBaseStore());
        orderModel.setSchedulingCronJob(scheduledCart);
        getModelService().save(orderModel);
        return getOrderConverter().convert(orderModel);
    }


    @Required
    public void setI18NService(I18NService i18NService) {
        this.i18NService = i18NService;
    }

    @Required
    public void setBaseSiteService(BaseSiteService baseSiteService) {
        this.baseSiteService = baseSiteService;
    }

    @Required
    public void setTriggerService(TriggerService triggerService) {
        this.triggerService = triggerService;
    }

    @Required
    public void setScheduledCartConverter(Converter<CartToOrderCronJobModel, ScheduledCartData> scheduledCartConverter) {
        this.scheduledCartConverter = scheduledCartConverter;
    }

    @Required
    public void setTriggerPopulator(Populator<TriggerData, TriggerModel> triggerPopulator) {
        this.triggerPopulator = triggerPopulator;
    }

    @Required
    public void setB2bDaysOfWeekConverter(Converter<DayOfWeek, B2BDaysOfWeekData> b2bDaysOfWeekConverter) {
        this.b2bDaysOfWeekConverter = b2bDaysOfWeekConverter;
    }

    @Required
    public void setWorldlineScheduleOrderService(WorldlineScheduleOrderService worldlineScheduleOrderService) {
        this.worldlineScheduleOrderService = worldlineScheduleOrderService;
    }

    public void setWorldlineConfigurationService(WorldlineConfigurationService worldlineConfigurationService) {
        this.worldlineConfigurationService = worldlineConfigurationService;
    }
}
