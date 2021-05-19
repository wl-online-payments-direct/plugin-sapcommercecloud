package com.ingenico.ogone.direct.facade.impl;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.ingenico.direct.RequestHeader;
import com.ingenico.direct.domain.WebhooksEvent;
import com.ingenico.ogone.direct.facade.IngenicoWebhookFacade;
import com.ingenico.ogone.direct.service.IngenicoWebhookService;

public class IngenicoWebhookFacadeImpl implements IngenicoWebhookFacade {

    private IngenicoWebhookService ingenicoWebhookService;


    @Override
    public WebhooksEvent retrieveWebhooksEvent(final String requestBody, final String keyId, final String signature) {
        validateParameterNotNull(requestBody, "requestBody cannot be null");

        List<RequestHeader> requestHeaderList = new ArrayList<>(2);
        requestHeaderList.add(new RequestHeader("X-GCS-KeyId", keyId));
        requestHeaderList.add(new RequestHeader("X-GCS-Signature", signature));

        return ingenicoWebhookService.unmarshal(requestBody, requestHeaderList, keyId);
    }

    @Override
    public void saveWebhooksEvent(WebhooksEvent webhooksEvent) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String webhookAsString = objectMapper.writeValueAsString(webhooksEvent);
        ingenicoWebhookService.saveWebhooksEvent(webhookAsString, webhooksEvent.getCreated());
    }

    public void setIngenicoWebhookService(IngenicoWebhookService ingenicoWebhookService) {
        this.ingenicoWebhookService = ingenicoWebhookService;
    }
}
