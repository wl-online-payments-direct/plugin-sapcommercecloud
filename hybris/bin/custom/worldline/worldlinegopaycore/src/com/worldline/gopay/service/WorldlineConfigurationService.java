package com.worldline.gopay.service;

import de.hybris.platform.store.BaseStoreModel;

import com.worldline.gopay.model.WorldlineConfigurationModel;

public interface WorldlineConfigurationService {

    WorldlineConfigurationModel getWorldlineConfigurationByWebhookKey(String webhookKey);

    WorldlineConfigurationModel getWorldlineConfigurationByMerchantId(String merchantId);

    WorldlineConfigurationModel getWorldlineConfiguration(BaseStoreModel baseStoreModel);

    WorldlineConfigurationModel getWorldlineConfiguration(String baseStoreId);

    WorldlineConfigurationModel getCurrentWorldlineConfiguration();

    String getMerchantId(BaseStoreModel baseStoreModel);

    String getCurrentMerchantId();

}
