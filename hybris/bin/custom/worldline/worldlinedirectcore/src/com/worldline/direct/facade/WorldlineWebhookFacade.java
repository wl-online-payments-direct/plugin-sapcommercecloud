package com.worldline.direct.facade;

import com.fasterxml.jackson.core.JsonProcessingException;

import com.ingenico.direct.domain.WebhooksEvent;
import com.worldline.direct.exception.WorldlineNonValidWebhooksEventException;

public interface WorldlineWebhookFacade {

    WebhooksEvent retrieveWebhooksEvent(String requestBody, String keyId, String signature);

    void saveWebhooksEvent(WebhooksEvent webhooksEvent) throws JsonProcessingException;

    void validateWebhooksEvent(WebhooksEvent webhooksEvent) throws WorldlineNonValidWebhooksEventException;
}
