package com.ingenico.ogone.direct.cronjob;

import java.util.Date;
import java.util.List;

import de.hybris.platform.cronjob.enums.CronJobResult;
import de.hybris.platform.cronjob.enums.CronJobStatus;
import de.hybris.platform.cronjob.model.CronJobModel;
import de.hybris.platform.servicelayer.cronjob.AbstractJobPerformable;
import de.hybris.platform.servicelayer.cronjob.PerformResult;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.util.Config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingenico.direct.domain.WebhooksEvent;
import com.ingenico.ogone.direct.dao.IngenicoWebhookDao;
import com.ingenico.ogone.direct.enums.IngenicoWebhooksEventStatusEnum;
import com.ingenico.ogone.direct.model.IngenicoWebhooksEventModel;
import com.ingenico.ogone.direct.service.IngenicoWebhookService;

public class IngenicoProcessWebhooksEventJob extends AbstractJobPerformable<CronJobModel> {
    private static final Logger LOGGER = LoggerFactory.getLogger(IngenicoProcessWebhooksEventJob.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ModelService modelService;
    private IngenicoWebhookService ingenicoWebhookService;
    private IngenicoWebhookDao ingenicoWebhookDao;

    @Override
    public PerformResult perform(final CronJobModel cronJob) {
        LOGGER.debug("Start processing..");

        final int batchSize = Config.getInt("ingenico.webhooks.cronjob.batchsize", 500);
        final int maxAttempts = Config.getInt("ingenico.webhooks.cronjob.maxAttempts", 5);

        final List<IngenicoWebhooksEventModel> nonProcessedWebhooksEvents = ingenicoWebhookDao.getNonProcessedWebhooksEvents(batchSize);

        for (final IngenicoWebhooksEventModel webhooksEventModel : nonProcessedWebhooksEvents) {
            try {
                webhooksEventModel.setLastProcessedTime(new Date());

                final WebhooksEvent webhooksEvent = OBJECT_MAPPER.readValue(webhooksEventModel.getBody(), WebhooksEvent.class);

                ingenicoWebhookService.processWebhooksEvent(webhooksEvent);
                webhooksEventModel.setStatus(IngenicoWebhooksEventStatusEnum.PROCESSED);

            } catch (Exception e) {
                LOGGER.error("[INGENICO] unexpected error occurred !", e);
                final Integer attempts = webhooksEventModel.getAttempts();
                if (attempts <= maxAttempts) {
                    webhooksEventModel.setStatus(IngenicoWebhooksEventStatusEnum.FAILED);
                } else {
                    webhooksEventModel.setStatus(IngenicoWebhooksEventStatusEnum.MAX_ATTEMPT_REACHED);
                    LOGGER.error("[INGENICO] Max attempts reached, please fix the problem and retry again");
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

    public void setIngenicoWebhookService(IngenicoWebhookService ingenicoWebhookService) {
        this.ingenicoWebhookService = ingenicoWebhookService;
    }

    public void setIngenicoWebhookDao(IngenicoWebhookDao ingenicoWebhookDao) {
        this.ingenicoWebhookDao = ingenicoWebhookDao;
    }
}
