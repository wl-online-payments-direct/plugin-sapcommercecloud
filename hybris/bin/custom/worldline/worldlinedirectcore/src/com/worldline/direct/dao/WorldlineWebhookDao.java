package com.worldline.direct.dao;

import java.util.List;

import com.worldline.direct.model.WorldlineWebhooksEventModel;

public interface WorldlineWebhookDao {

    List<WorldlineWebhooksEventModel> getNonProcessedWebhooksEvents(int batchSize);
}
