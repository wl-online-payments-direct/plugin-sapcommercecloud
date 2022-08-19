package com.worldline.direct.cronjob;

import de.hybris.platform.b2bacceleratorservices.model.process.ReplenishmentProcessModel;
import de.hybris.platform.cronjob.enums.CronJobResult;
import de.hybris.platform.cronjob.enums.CronJobStatus;
import de.hybris.platform.orderscheduling.model.CartToOrderCronJobModel;
import de.hybris.platform.processengine.BusinessProcessService;
import de.hybris.platform.processengine.enums.ProcessState;
import de.hybris.platform.servicelayer.cronjob.AbstractJobPerformable;
import de.hybris.platform.servicelayer.cronjob.PerformResult;
import de.hybris.platform.servicelayer.cronjob.TriggerService;
import de.hybris.platform.servicelayer.i18n.I18NService;
import org.apache.commons.lang.BooleanUtils;
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
        if (BooleanUtils.isTrue(cronJob.isSubmitted()))
        {
        final String replenishmentOrderProcessCode = "worldlineReplenishmentOrderProcess" + cronJob.getCode() + System.currentTimeMillis();
        final ReplenishmentProcessModel businessProcessModel = getBusinessProcessService()
                .createProcess(replenishmentOrderProcessCode, "worldlineReplenishmentOrderProcess");
        businessProcessModel.setCartToOrderCronJob(cronJob);
        modelService.save(businessProcessModel);
        getBusinessProcessService().startProcess(businessProcessModel);
        modelService.refresh(businessProcessModel);
        while (businessProcessModel.getProcessState().equals(ProcessState.RUNNING)
                || businessProcessModel.getProcessState().equals(ProcessState.CREATED)) {
            modelService.refresh(businessProcessModel);

            try {
                Thread.sleep(1000);
            } catch (final InterruptedException e) {
                LOG.warn("Thread interrupted " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }

        final PerformResult cronJobResult;
        if (businessProcessModel.getProcessState().equals(ProcessState.SUCCEEDED)) {
            cronJobResult = new PerformResult(CronJobResult.SUCCESS, CronJobStatus.FINISHED);
        } else if (businessProcessModel.getProcessState().equals(ProcessState.ERROR)
                || businessProcessModel.getProcessState().equals(ProcessState.FAILED)) {
            cronJobResult = new PerformResult(CronJobResult.ERROR, CronJobStatus.UNKNOWN);
        } else {
            cronJobResult = new PerformResult(CronJobResult.FAILURE, CronJobStatus.FINISHED);
        }
        return cronJobResult;
        }else {
            LOG.error("payment was not received for this replenishment");
            final PerformResult cronJobResult;
            cronJobResult = new PerformResult(CronJobResult.FAILURE, CronJobStatus.FINISHED);
            return cronJobResult;
        }
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
