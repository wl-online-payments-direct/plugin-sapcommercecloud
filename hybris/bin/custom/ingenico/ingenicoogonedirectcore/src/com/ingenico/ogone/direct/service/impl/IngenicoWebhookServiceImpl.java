package com.ingenico.ogone.direct.service.impl;

import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.WEBHOOK_TYPE_ENUM;
import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;
import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNullStandardMessage;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

import de.hybris.platform.servicelayer.model.ModelService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingenico.direct.RequestHeader;
import com.ingenico.direct.domain.WebhooksEvent;
import com.ingenico.direct.webhooks.InMemorySecretKeyStore;
import com.ingenico.direct.webhooks.Webhooks;
import com.ingenico.direct.webhooks.WebhooksHelper;
import com.ingenico.ogone.direct.model.IngenicoConfigurationModel;
import com.ingenico.ogone.direct.model.IngenicoWebhooksEventModel;
import com.ingenico.ogone.direct.service.IngenicoConfigurationService;
import com.ingenico.ogone.direct.service.IngenicoTransactionService;
import com.ingenico.ogone.direct.service.IngenicoWebhookService;

public class IngenicoWebhookServiceImpl implements IngenicoWebhookService {

    private final static Logger LOGGER = LoggerFactory.getLogger(IngenicoWebhookServiceImpl.class);

    private IngenicoConfigurationService ingenicoConfigurationService;
    private IngenicoTransactionService ingenicoTransactionService;
    private ModelService modelService;

    @Override
    public WebhooksHelper getWebHookHelper(String keyId) {
        final InMemorySecretKeyStore secretKeyStore = InMemorySecretKeyStore.INSTANCE;
        final IngenicoConfigurationModel currentIngenicoConfiguration = ingenicoConfigurationService.getIngenicoConfigurationByWebhookKey(keyId);

        secretKeyStore.storeSecretKey(currentIngenicoConfiguration.getWebhookKeyId(), currentIngenicoConfiguration.getWebhookSecret());
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

        final IngenicoWebhooksEventModel webhooksEventModel = modelService.create(IngenicoWebhooksEventModel.class);
        webhooksEventModel.setBody(webhooksEvent);
        webhooksEventModel.setCreatedTime(createdTime);
        modelService.save(webhooksEventModel);
    }

    @Override
    public void processWebhooksEvent(WebhooksEvent webhooksEvent) {
        validateParameterNotNullStandardMessage("webhooksEvent", webhooksEvent);
        switch (WEBHOOK_TYPE_ENUM.fromString(webhooksEvent.getType())) {
            case PAYMENT_CREATED:
            case PAYMENT_REDIRECTED:
            case PAYMENT_AUTH_REQUESTED:
                break;
            case PAYMENT_PENDING_APPROVAL:
                // TODO need to APPROVE in our side ????
                break;
            case PAYMENT_PENDING_COMPLETION:
                // TODO need to COMPLETE in our side ????
                break;
            case PAYMENT_PENDING_CAPTURE:
                // TODO need to CAPTURE in our side ????
                break;
            case PAYMENT_CAPTURE_REQUEST:
            case PAYMENT_CAPTURED:
            case PAYMENT_REJECTED_CAPTURE:
                ingenicoTransactionService.processCapturedEvent(webhooksEvent);
                break;
            case PAYMENT_REJECTED:
                // TODO need to REJECT in our side ????
                break;
            case PAYMENT_CANCELLED:
                ingenicoTransactionService.processCancelledEvent(webhooksEvent);
                break;
            case PAYMENT_REFUNDED:
                ingenicoTransactionService.processRefundedEvent(webhooksEvent);
                break;
            default:
                break;

        }
        LOGGER.debug("[INGENICO] WEBHOOK with type {} has been processed", webhooksEvent.getType());
    }

    public void setIngenicoConfigurationService(IngenicoConfigurationService ingenicoConfigurationService) {
        this.ingenicoConfigurationService = ingenicoConfigurationService;
    }

    public void setModelService(ModelService modelService) {
        this.modelService = modelService;
    }

    public void setIngenicoTransactionService(IngenicoTransactionService ingenicoTransactionService) {
        this.ingenicoTransactionService = ingenicoTransactionService;
    }
}
