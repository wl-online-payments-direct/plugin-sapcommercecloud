package com.worldline.direct.service;

import java.util.List;

import com.ingenico.direct.RequestHeader;
import com.ingenico.direct.domain.WebhooksEvent;
import com.ingenico.direct.webhooks.WebhooksHelper;

public interface WorldlineWebhookService {

    WebhooksHelper getWebHookHelper(String keyId);

    WebhooksEvent unmarshal(String bodyStream, List<RequestHeader> requestHeaders, String keyId);

    void saveWebhooksEvent(String webhooksEvent, String created);

    void processWebhooksEvent(WebhooksEvent webhooksEvent);
}
