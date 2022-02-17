package com.worldline.direct.service;

import de.hybris.platform.store.BaseStoreModel;

import com.worldline.direct.model.WorldlineConfigurationModel;

public interface WorldlineConfigurationService {

    WorldlineConfigurationModel getWorldlineConfigurationByWebhookKey(String webhookKey);

    WorldlineConfigurationModel getWorldlineConfigurationByMerchantId(String merchantId);

    WorldlineConfigurationModel getWorldlineConfiguration(BaseStoreModel baseStoreModel);

    WorldlineConfigurationModel getCurrentWorldlineConfiguration();

    String getMerchantId(BaseStoreModel baseStoreModel);

    String getCurrentMerchantId();

}
