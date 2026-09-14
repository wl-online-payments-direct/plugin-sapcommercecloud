package com.worldline.gopay.dao;

import java.util.List;

import com.worldline.gopay.model.WorldlineWebhooksEventModel;

public interface WorldlineWebhookDao {

    List<WorldlineWebhooksEventModel> getNonProcessedWebhooksEvents(int batchSize);
}
