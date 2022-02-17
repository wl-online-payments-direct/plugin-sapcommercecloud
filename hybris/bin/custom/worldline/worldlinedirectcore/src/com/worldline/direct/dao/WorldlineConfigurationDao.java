package com.worldline.direct.dao;

import com.worldline.direct.model.WorldlineConfigurationModel;

public interface WorldlineConfigurationDao {

    WorldlineConfigurationModel findWorldlineConfigurationByWebhookKey(String apiKey);

    WorldlineConfigurationModel findWorldlineConfigurationByMerchantId(String merchantId);
}
