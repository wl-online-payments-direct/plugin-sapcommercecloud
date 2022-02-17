package com.worldline.direct.facade.impl;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.ingenico.direct.RequestHeader;
import com.ingenico.direct.domain.WebhooksEvent;
import com.worldline.direct.dao.WorldlineOrderDao;
import com.worldline.direct.exception.WorldlineNonValidWebhooksEventException;
import com.worldline.direct.facade.WorldlineWebhookFacade;
import com.worldline.direct.service.WorldlineWebhookService;

public class WorldlineWebhookFacadeImpl implements WorldlineWebhookFacade {

    private WorldlineWebhookService worldlineWebhookService;
    private WorldlineOrderDao worldlineOrderDao;


    @Override
    public WebhooksEvent retrieveWebhooksEvent(final String requestBody, final String keyId, final String signature) {
        validateParameterNotNull(requestBody, "requestBody cannot be null");

        List<RequestHeader> requestHeaderList = new ArrayList<>(2);
        requestHeaderList.add(new RequestHeader("X-GCS-KeyId", keyId));
        requestHeaderList.add(new RequestHeader("X-GCS-Signature", signature));

        return worldlineWebhookService.unmarshal(requestBody, requestHeaderList, keyId);
    }

    @Override
    public void saveWebhooksEvent(WebhooksEvent webhooksEvent) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String webhookAsString = objectMapper.writeValueAsString(webhooksEvent);
        worldlineWebhookService.saveWebhooksEvent(webhookAsString, webhooksEvent.getCreated());
    }

    @Override
    public void validateWebhooksEvent(WebhooksEvent webhooksEvent) throws WorldlineNonValidWebhooksEventException {
        validateParameterNotNull(webhooksEvent, "webhooksEvent cannot be null");
        try {
            String orderCode = null;
            if (webhooksEvent.getPayment() != null) {
                orderCode = webhooksEvent.getPayment().getPaymentOutput().getReferences().getMerchantReference();
            } else if (webhooksEvent.getRefund() != null) {
                orderCode = webhooksEvent.getRefund().getRefundOutput().getReferences().getMerchantReference();
            }
            worldlineOrderDao.findWorldlineOrder(orderCode);
        } catch (Exception exception) {
            throw new WorldlineNonValidWebhooksEventException(exception.getMessage());
        }
    }

    public void setWorldlineWebhookService(WorldlineWebhookService worldlineWebhookService) {
        this.worldlineWebhookService = worldlineWebhookService;
    }

    public void setWorldlineOrderDao(WorldlineOrderDao worldlineOrderDao) {
        this.worldlineOrderDao = worldlineOrderDao;
    }
}
