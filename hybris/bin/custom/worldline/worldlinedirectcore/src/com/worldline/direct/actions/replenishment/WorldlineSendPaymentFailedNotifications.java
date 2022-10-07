package com.worldline.direct.actions.replenishment;

import com.worldline.direct.event.replenishment.payment.WorldlineReplenishmentPaymentFailedNotificationEvent;
import de.hybris.platform.b2bacceleratorservices.model.process.ReplenishmentProcessModel;
import de.hybris.platform.processengine.action.AbstractProceduralAction;
import de.hybris.platform.servicelayer.event.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

public class WorldlineSendPaymentFailedNotifications extends AbstractProceduralAction<ReplenishmentProcessModel>
{
    private static final Logger LOG = LoggerFactory.getLogger(WorldlineSendPaymentFailedNotifications.class);
    private EventService eventService;

    @Override
    public void executeAction(final ReplenishmentProcessModel process)
    {
        if (LOG.isInfoEnabled())
        {
            LOG.info("Process: {} in step {}", process.getCode(), getClass().getSimpleName());
        }
        getEventService().publishEvent(new WorldlineReplenishmentPaymentFailedNotificationEvent(process));
    }

    protected EventService getEventService()
    {
        return eventService;
    }

    @Required
    public void setEventService(final EventService eventService)
    {
        this.eventService = eventService;
    }
}
