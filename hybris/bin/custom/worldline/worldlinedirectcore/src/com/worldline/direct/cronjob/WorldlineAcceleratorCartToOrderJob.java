package com.worldline.direct.cronjob;

import de.hybris.platform.b2bacceleratorservices.model.process.ReplenishmentProcessModel;
import de.hybris.platform.cronjob.enums.CronJobResult;
import de.hybris.platform.cronjob.enums.CronJobStatus;
import de.hybris.platform.orderscheduling.model.CartToOrderCronJobModel;
import de.hybris.platform.processengine.BusinessProcessService;
import de.hybris.platform.servicelayer.cronjob.AbstractJobPerformable;
import de.hybris.platform.servicelayer.cronjob.PerformResult;
import de.hybris.platform.servicelayer.cronjob.TriggerService;
import de.hybris.platform.servicelayer.i18n.I18NService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;

public class WorldlineAcceleratorCartToOrderJob extends AbstractJobPerformable<CartToOrderCronJobModel> {

    private static final Logger LOG = Logger.getLogger(WorldlineAcceleratorCartToOrderJob.class);
    private BusinessProcessService businessProcessService;
    private TriggerService triggerService;
    private I18NService i18NService;

    @Override
    public PerformResult perform(final CartToOrderCronJobModel cronJob) {
        LOG.info("starting Worldline Accelerator Cart To Order Job");
        final String replenishmentOrderProcessCode = "worldlineReplenishmentOrderProcess" + cronJob.getCode() + System.currentTimeMillis();
        final ReplenishmentProcessModel businessProcessModel = getBusinessProcessService()
                .createProcess(replenishmentOrderProcessCode, "worldlineReplenishmentOrderProcess");
        businessProcessModel.setCartToOrderCronJob(cronJob);
        modelService.save(businessProcessModel);
        getBusinessProcessService().startProcess(businessProcessModel);
        modelService.refresh(businessProcessModel);
        return new PerformResult(CronJobResult.SUCCESS, CronJobStatus.FINISHED);
    }

    protected BusinessProcessService getBusinessProcessService() {
        return businessProcessService;
    }

    @Required
    public void setBusinessProcessService(final BusinessProcessService businessProcessService) {
        this.businessProcessService = businessProcessService;
    }

    protected TriggerService getTriggerService() {
        return triggerService;
    }

    @Required
    public void setTriggerService(final TriggerService triggerService) {
        this.triggerService = triggerService;
    }

    protected I18NService getI18NService() {
        return i18NService;
    }

    @Required
    public void setI18NService(final I18NService i18NService) {
        this.i18NService = i18NService;
    }
}
