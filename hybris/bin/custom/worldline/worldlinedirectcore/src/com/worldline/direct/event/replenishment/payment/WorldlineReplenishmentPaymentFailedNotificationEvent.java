package com.worldline.direct.event.replenishment.payment;

import de.hybris.platform.b2bacceleratorservices.model.process.ReplenishmentProcessModel;
import de.hybris.platform.servicelayer.event.events.AbstractEvent;

public class WorldlineReplenishmentPaymentFailedNotificationEvent extends AbstractEvent {

    private ReplenishmentProcessModel replenishmentProcessModel;
    public WorldlineReplenishmentPaymentFailedNotificationEvent(ReplenishmentProcessModel process) {
        this.replenishmentProcessModel=process;
    }

    public ReplenishmentProcessModel getReplenishmentProcessModel() {
        return replenishmentProcessModel;
    }

    public void setReplenishmentProcessModel(ReplenishmentProcessModel replenishmentProcessModel) {
        this.replenishmentProcessModel = replenishmentProcessModel;
    }
}
