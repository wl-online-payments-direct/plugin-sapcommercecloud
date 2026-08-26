package com.worldline.gopay.dao;

import com.worldline.gopay.model.WorldlineConfigurationModel;

public interface WorldlineConfigurationDao {

    WorldlineConfigurationModel findWorldlineConfigurationByWebhookKey(String apiKey);

    WorldlineConfigurationModel findWorldlineConfigurationByMerchantId(String merchantId);
}
