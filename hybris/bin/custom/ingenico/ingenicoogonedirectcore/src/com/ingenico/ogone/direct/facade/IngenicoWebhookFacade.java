package com.ingenico.ogone.direct.facade;

import com.fasterxml.jackson.core.JsonProcessingException;

import com.ingenico.direct.domain.WebhooksEvent;
import com.ingenico.ogone.direct.exception.IngenicoNonValidWebhooksEventException;

public interface IngenicoWebhookFacade {

    WebhooksEvent retrieveWebhooksEvent(String requestBody, String keyId, String signature);

    void saveWebhooksEvent(WebhooksEvent webhooksEvent) throws JsonProcessingException;

    void validateWebhooksEvent(WebhooksEvent webhooksEvent) throws IngenicoNonValidWebhooksEventException;
}
