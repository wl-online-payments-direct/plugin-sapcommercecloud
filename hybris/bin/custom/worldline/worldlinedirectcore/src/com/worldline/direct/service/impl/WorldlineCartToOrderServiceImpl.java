package com.worldline.direct.service.impl;

import com.onlinepayments.domain.PaymentResponse;
import com.worldline.direct.service.WorldlineCartToOrderService;
import de.hybris.platform.b2bacceleratorservices.event.ReplenishmentOrderPlacedEvent;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.cronjob.model.TriggerModel;
import de.hybris.platform.orderscheduling.model.CartToOrderCronJobModel;
import de.hybris.platform.servicelayer.cronjob.CronJobService;
import de.hybris.platform.servicelayer.event.EventService;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.store.services.BaseStoreService;
import org.apache.commons.lang.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class WorldlineCartToOrderServiceImpl implements WorldlineCartToOrderService {
    private static final Logger LOG = LoggerFactory.getLogger(WorldlineCartToOrderServiceImpl.class);
    private ModelService modelService;
    private EventService eventService;
    private CommonI18NService commonI18NService;
    private BaseStoreService baseStoreService;
    private CronJobService cronJobService;

    @Override
    public void enableCartToOrderJob(CartToOrderCronJobModel cronJobModel, PaymentResponse paymentResponse) {
        Optional<TriggerModel> firstTrigger = cronJobModel.getTriggers().stream().findFirst();
        WorldlinePaymentInfoModel paymentInfo = (WorldlinePaymentInfoModel) cronJobModel.getPaymentInfo();
        cronJobModel.setSubmitted(true);

        modelService.saveAll(cronJobModel,paymentInfo);

        if (firstTrigger.isPresent()) {
            TriggerModel triggerModel = firstTrigger.get();
            triggerModel.setActive(true);
            modelService.save(triggerModel);
            if (BooleanUtils.isTrue(triggerModel.getRelative())) {
                cronJobService.performCronJob(cronJobModel);
            }
        }
        eventService.publishEvent(initializeReplenishmentPlacedEvent(cronJobModel));
    }

    protected ReplenishmentOrderPlacedEvent initializeReplenishmentPlacedEvent(final CartToOrderCronJobModel scheduledCart) {
        final ReplenishmentOrderPlacedEvent replenishmentOrderPlacedEvent = new ReplenishmentOrderPlacedEvent(scheduledCart);
        replenishmentOrderPlacedEvent.setCurrency(commonI18NService.getCurrentCurrency());
        replenishmentOrderPlacedEvent.setLanguage(commonI18NService.getCurrentLanguage());
        replenishmentOrderPlacedEvent.setCustomer((CustomerModel) scheduledCart.getCart().getUser());
        replenishmentOrderPlacedEvent.setBaseStore(baseStoreService.getCurrentBaseStore());

        return replenishmentOrderPlacedEvent;
    }


    @Override
    public void cancelCartToOrderJob(CartToOrderCronJobModel cronJobModel) {
        LOG.info("cancel cart to Order Cron Job");
        // TODO : mark cron Job for deletion
    }

    public void setEventService(EventService eventService) {
        this.eventService = eventService;
    }

    public void setCommonI18NService(CommonI18NService commonI18NService) {
        this.commonI18NService = commonI18NService;
    }

    public void setBaseStoreService(BaseStoreService baseStoreService) {
        this.baseStoreService = baseStoreService;
    }

    public void setModelService(ModelService modelService) {
        this.modelService = modelService;
    }

    public void setCronJobService(CronJobService cronJobService) {
        this.cronJobService = cronJobService;
    }
}
