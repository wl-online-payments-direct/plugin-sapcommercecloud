package com.worldline.direct.cronjob;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinepayments.domain.WebhooksEvent;
import com.worldline.direct.dao.WorldlineWebhookDao;
import com.worldline.direct.enums.WorldlineWebhooksEventStatusEnum;
import com.worldline.direct.model.WorldlineWebhooksEventModel;
import com.worldline.direct.service.WorldlineWebhookService;
import de.hybris.platform.cronjob.enums.CronJobResult;
import de.hybris.platform.cronjob.enums.CronJobStatus;
import de.hybris.platform.cronjob.model.CronJobModel;
import de.hybris.platform.servicelayer.cronjob.AbstractJobPerformable;
import de.hybris.platform.servicelayer.cronjob.PerformResult;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.util.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

public class WorldlineProcessWebhooksEventJob extends AbstractJobPerformable<CronJobModel> {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldlineProcessWebhooksEventJob.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ModelService modelService;
    private WorldlineWebhookService worldlineWebhookService;
    private WorldlineWebhookDao worldlineWebhookDao;

    @Override
    public PerformResult perform(final CronJobModel cronJob) {
        LOGGER.debug("Start processing..");

        final int batchSize = Config.getInt("worldline.webhooks.cronjob.batchsize", 500);
        final int maxAttempts = Config.getInt("worldline.webhooks.cronjob.maxAttempts", 5);

        final List<WorldlineWebhooksEventModel> nonProcessedWebhooksEvents = worldlineWebhookDao.getNonProcessedWebhooksEvents(batchSize);

        for (final WorldlineWebhooksEventModel webhooksEventModel : nonProcessedWebhooksEvents) {
            try {
                webhooksEventModel.setLastProcessedTime(new Date());

                final WebhooksEvent webhooksEvent = OBJECT_MAPPER.readValue(webhooksEventModel.getBody(), WebhooksEvent.class);

                worldlineWebhookService.processWebhooksEvent(webhooksEvent);
                webhooksEventModel.setStatus(WorldlineWebhooksEventStatusEnum.PROCESSED);

            } catch (Exception e) {
                LOGGER.error("[WORLDLINE] unexpected error occurred !", e);
                final Integer attempts = webhooksEventModel.getAttempts();
                if (attempts <= maxAttempts) {
                    webhooksEventModel.setStatus(WorldlineWebhooksEventStatusEnum.FAILED);
                } else {
                    webhooksEventModel.setStatus(WorldlineWebhooksEventStatusEnum.MAX_ATTEMPT_REACHED);
                    LOGGER.error("[WORLDLINE] Max attempts reached, please fix the problem and retry again");
                }
            } finally {
                webhooksEventModel.setAttempts(webhooksEventModel.getAttempts() + 1);
                modelService.save(webhooksEventModel);
            }
        }
        LOGGER.debug("End processing.");
        return new PerformResult(CronJobResult.SUCCESS, CronJobStatus.FINISHED);
    }

    public ModelService getModelService() {
        return modelService;
    }

    @Override
    public void setModelService(ModelService modelService) {
        this.modelService = modelService;
    }

    public void setWorldlineWebhookService(WorldlineWebhookService worldlineWebhookService) {
        this.worldlineWebhookService = worldlineWebhookService;
    }

    public void setWorldlineWebhookDao(WorldlineWebhookDao worldlineWebhookDao) {
        this.worldlineWebhookDao = worldlineWebhookDao;
    }
}
