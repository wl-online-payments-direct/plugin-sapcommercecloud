package com.ingenico.ogone.direct.facade;

import com.fasterxml.jackson.core.JsonProcessingException;

import com.ingenico.direct.domain.WebhooksEvent;

public interface IngenicoWebhookFacade {

    WebhooksEvent retrieveWebhooksEvent(String requestBody, String keyId, String signature);

    void saveWebhooksEvent(WebhooksEvent webhooksEvent) throws JsonProcessingException;
}
