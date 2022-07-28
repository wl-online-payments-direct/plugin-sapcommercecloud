package com.worldline.direct.service;

import com.onlinepayments.RequestHeader;
import com.onlinepayments.domain.WebhooksEvent;
import com.onlinepayments.webhooks.WebhooksHelper;

import java.util.List;

public interface WorldlineWebhookService {

    WebhooksHelper getWebHookHelper(String keyId);

    WebhooksEvent unmarshal(String bodyStream, List<RequestHeader> requestHeaders, String keyId);

    void saveWebhooksEvent(String webhooksEvent, String created);

    void processWebhooksEvent(WebhooksEvent webhooksEvent);
}
