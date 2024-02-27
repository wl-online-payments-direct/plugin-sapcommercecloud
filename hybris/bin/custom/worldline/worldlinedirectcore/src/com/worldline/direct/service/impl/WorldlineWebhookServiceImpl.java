package com.worldline.direct.service.impl;

import com.onlinepayments.RequestHeader;
import com.onlinepayments.domain.WebhooksEvent;
import com.onlinepayments.webhooks.InMemorySecretKeyStore;
import com.onlinepayments.webhooks.Webhooks;
import com.onlinepayments.webhooks.WebhooksHelper;
import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import com.worldline.direct.model.WorldlineConfigurationModel;
import com.worldline.direct.model.WorldlineWebhooksEventModel;
import com.worldline.direct.service.WorldlineConfigurationService;
import com.worldline.direct.service.WorldlineTransactionService;
import com.worldline.direct.service.WorldlineWebhookService;
import de.hybris.platform.servicelayer.model.ModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;
import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNullStandardMessage;

public class WorldlineWebhookServiceImpl implements WorldlineWebhookService {

    private final static Logger LOGGER = LoggerFactory.getLogger(WorldlineWebhookServiceImpl.class);

    private WorldlineConfigurationService worldlineConfigurationService;
    private WorldlineTransactionService worldlineTransactionService;
    private ModelService modelService;

    @Override
    public WebhooksHelper getWebHookHelper(String keyId) {
        final InMemorySecretKeyStore secretKeyStore = InMemorySecretKeyStore.INSTANCE;
        final WorldlineConfigurationModel currentWorldlineConfiguration = worldlineConfigurationService.getWorldlineConfigurationByWebhookKey(keyId);

        secretKeyStore.storeSecretKey(currentWorldlineConfiguration.getWebhookKeyId(), currentWorldlineConfiguration.getWebhookSecret());
        return Webhooks.createHelper(secretKeyStore);
    }

    @Override
    public WebhooksEvent unmarshal(String bodyStream, List<RequestHeader> requestHeaders, String keyId) {
        validateParameterNotNull(bodyStream, "bodyStream cannot be null");
        validateParameterNotNull(requestHeaders, "requestHeaders cannot be null");

        return getWebHookHelper(keyId).unmarshal(bodyStream, requestHeaders);
    }

    @Override
    public void saveWebhooksEvent(String webhooksEvent, String created) {
        validateParameterNotNull(webhooksEvent, "webhooksEvent cannot be null");
        validateParameterNotNull(created, "created cannot be null");
        final ZonedDateTime zdt = ZonedDateTime.parse(created);
        final Date createdTime = Date.from(zdt.toInstant());

        final WorldlineWebhooksEventModel webhooksEventModel = modelService.create(WorldlineWebhooksEventModel.class);
        webhooksEventModel.setBody(webhooksEvent);
        webhooksEventModel.setCreatedTime(createdTime);
        modelService.save(webhooksEventModel);
    }

    @Override
    public void processWebhooksEvent(WebhooksEvent webhooksEvent) {
        validateParameterNotNullStandardMessage("webhooksEvent", webhooksEvent);
        switch (WorldlinedirectcoreConstants.WEBHOOK_TYPE_ENUM.fromString(webhooksEvent.getType())) {
            case PAYMENT_CREATED:
            case PAYMENT_REDIRECTED:
            case PAYMENT_AUTH_REQUESTED:
            case PAYMENT_PENDING_APPROVAL:
            case PAYMENT_PENDING_COMPLETION:
            case PAYMENT_PENDING_CAPTURE:
            case PAYMENT_CANCELLED:
            case PAYMENT_REJECTED:
            case PAYMENT_CAPTURE_REQUEST:
            case PAYMENT_REFUND_REQUESTED:
                break;
            case PAYMENT_CAPTURED:
            case PAYMENT_REJECTED_CAPTURE:
                worldlineTransactionService.processCapturedEvent(webhooksEvent);
                break;
            case PAYMENT_REFUNDED:
                worldlineTransactionService.processRefundedEvent(webhooksEvent);
                break;
            default:
                break;

        }
        LOGGER.debug("[WORLDLINE] WEBHOOK with type {} has been processed", webhooksEvent.getType());
    }

    public void setModelService(ModelService modelService) {
        this.modelService = modelService;
    }

    public void setWorldlineConfigurationService(WorldlineConfigurationService worldlineConfigurationService) {
        this.worldlineConfigurationService = worldlineConfigurationService;
    }

    public void setWorldlineTransactionService(WorldlineTransactionService worldlineTransactionService) {
        this.worldlineTransactionService = worldlineTransactionService;
    }
}
