package com.ingenico.ogone.direct.dao;

import java.util.List;

import com.ingenico.ogone.direct.model.IngenicoWebhooksEventModel;

public interface IngenicoWebhookDao {

    List<IngenicoWebhooksEventModel> getNonProcessedWebhooksEvents(int batchSize);
}
