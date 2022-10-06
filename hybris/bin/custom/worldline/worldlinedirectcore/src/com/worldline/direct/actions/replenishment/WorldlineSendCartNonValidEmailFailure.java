package com.worldline.direct.actions.replenishment;

import com.worldline.direct.event.replenishment.validatecart.WorldlineReplenishmentCartNonValidNotificationEvent;
import de.hybris.platform.b2bacceleratorservices.model.process.ReplenishmentProcessModel;
import de.hybris.platform.processengine.action.AbstractProceduralAction;
import de.hybris.platform.servicelayer.event.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorldlineSendCartNonValidEmailFailure extends AbstractProceduralAction<ReplenishmentProcessModel> {
    private static final Logger LOG = LoggerFactory.getLogger(WorldlineSendCartNonValidEmailFailure.class);
    private EventService eventService;

    @Override
    public void executeAction(final ReplenishmentProcessModel process)
    {
        if (LOG.isInfoEnabled())
        {
            LOG.info("Process: {} in step {}", process.getCode(), getClass().getSimpleName());
        }
        eventService.publishEvent(new WorldlineReplenishmentCartNonValidNotificationEvent(process));
    }

    public void setEventService(EventService eventService) {
        this.eventService = eventService;
    }
}
