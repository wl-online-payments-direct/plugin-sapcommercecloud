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
import de.hybris.platform.util.StandardDateRange;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import java.util.*;

import static de.hybris.platform.util.localization.Localization.getLocalizedString;

public class WorldLineExtendedB2CCheckoutFacadeImpl extends DefaultCheckoutFacade implements WorldlineDirectCheckoutFacade {
    public static final Logger LOG = LoggerFactory.getLogger(WorldLineExtendedB2CCheckoutFacadeImpl.class);
    private static final String CART_CHECKOUT_REPLENISHMENT_NO_STARTDATE = "cart.replenishment.no.startdate";
    private static final String CART_CHECKOUT_REPLENISHMENT_NO_FREQUENCY = "cart.replenishment.no.frequency";
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

    private OrderData scheduleOrderAndPlaceOrder(List<TriggerData> triggerDataList) throws InvalidCartException {
        final CartModel cartModel = getCart();
        cartModel.setSite(baseSiteService.getCurrentBaseSite());
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
            triggerPopulator.populate(triggerData, triggerModel);


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
            triggerModelList.add(triggerModel);
        }
        return triggerModelList;
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
                final Calendar calendarStartTime = Calendar.getInstance(i18NService.getCurrentTimeZone(), i18NService.getCurrentLocale());
                Date nextActivationDate = replenishmentStartDate.before(calendarStartTime.getTime()) ? calendarStartTime.getTime() : replenishmentStartDate;
                calendarStartTime.setTime(nextActivationDate);
                List<Integer> monthsOptions = getMonthsOptions(Integer.parseInt(placeOrderData.getNMonths()), calendarStartTime.get(Calendar.MONTH));
                monthsOptions.stream().forEach(month->{
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

    private TriggerData createTrigger(PlaceOrderData placeOrderData) {
        TriggerData triggerData = new TriggerData();


        final Date replenishmentStartDate = placeOrderData.getReplenishmentStartDate();
        final Calendar calendarStartTime = Calendar.getInstance(i18NService.getCurrentTimeZone(), i18NService.getCurrentLocale());
        triggerData
                .setActivationTime((replenishmentStartDate.before(calendarStartTime.getTime()) ? calendarStartTime.getTime() : replenishmentStartDate));

        final Date replenishmentEndDate = placeOrderData.getReplenishmentEndDate();
        if (Objects.nonNull(replenishmentEndDate) && replenishmentStartDate.before(replenishmentEndDate)) {
            triggerData.setDateRange(new StandardDateRange(replenishmentStartDate, replenishmentEndDate));
        }
        return triggerData;
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
    public ScheduledCartData scheduleOrder(final List<TriggerData> triggerDataList) {
        final CartModel cartModel = getCart();
        cartModel.setSite(baseSiteService.getCurrentBaseSite());
        cartModel.setStore(getBaseStoreService().getCurrentBaseStore());
        getModelService().save(cartModel);

        final AddressModel deliveryAddress = cartModel.getDeliveryAddress();
        final AddressModel paymentAddress = cartModel.getPaymentAddress();
        final PaymentInfoModel paymentInfo = cartModel.getPaymentInfo();
        List<TriggerModel> triggers = createTriggers(triggerDataList);
        // schedule cart
        final CartToOrderCronJobModel scheduledCart = worldlineScheduleOrderService.createOrderFromCartCronJob(cartModel,
                deliveryAddress, paymentAddress, paymentInfo, triggers);

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
