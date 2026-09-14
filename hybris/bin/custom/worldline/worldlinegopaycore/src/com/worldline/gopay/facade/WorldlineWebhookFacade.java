package com.worldline.gopay.facade;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.onlinepayments.domain.WebhooksEvent;
import com.worldline.gopay.exception.WorldlineNonValidWebhooksEventException;

public interface WorldlineWebhookFacade {

    WebhooksEvent retrieveWebhooksEvent(String requestBody, String keyId, String signature);

    void saveWebhooksEvent(WebhooksEvent webhooksEvent) throws JsonProcessingException;

    void validateWebhooksEvent(WebhooksEvent webhooksEvent) throws WorldlineNonValidWebhooksEventException;
}
