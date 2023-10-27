package com.worldline.direct.service.impl;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

import java.util.Objects;

import com.worldline.direct.dao.WorldlineConfigurationDao;
import com.worldline.direct.service.WorldlineConfigurationService;
import de.hybris.platform.store.BaseStoreModel;
import de.hybris.platform.store.services.BaseStoreService;

import com.worldline.direct.exception.WorldlineConfigurationNotFoundException;
import com.worldline.direct.model.WorldlineConfigurationModel;

public class WorldlineConfigurationServiceImpl implements WorldlineConfigurationService {

    private BaseStoreService baseStoreService;
    private WorldlineConfigurationDao worldlineConfigurationDao;

    @Override
    public WorldlineConfigurationModel getWorldlineConfigurationByWebhookKey(String webhookKey) {
        validateParameterNotNull(webhookKey, "webhookKey cannot be null");
        return worldlineConfigurationDao.findWorldlineConfigurationByWebhookKey(webhookKey);
    }

    @Override
    public WorldlineConfigurationModel getWorldlineConfigurationByMerchantId(String merchantId) {
        validateParameterNotNull(merchantId, "merchantId cannot be null");
        return worldlineConfigurationDao.findWorldlineConfigurationByMerchantId(merchantId);
    }

    @Override
    public WorldlineConfigurationModel getWorldlineConfiguration(final BaseStoreModel baseStoreModel) {
        validateParameterNotNull(baseStoreModel, "baseStore cannot be null");
        final WorldlineConfigurationModel worldlineConfiguration = baseStoreModel.getWorldlineConfiguration();
        if (Objects.isNull(worldlineConfiguration)) {
            throw new WorldlineConfigurationNotFoundException(baseStoreModel.getUid());
        }
        return worldlineConfiguration;
    }

    @Override
    public WorldlineConfigurationModel getWorldlineConfiguration(String baseStoreId) {
        return baseStoreService.getBaseStoreForUid(baseStoreId).getWorldlineConfiguration();
    }

    @Override
    public WorldlineConfigurationModel getCurrentWorldlineConfiguration() {
        return getWorldlineConfiguration(baseStoreService.getCurrentBaseStore());
    }

    @Override
    public String getMerchantId(final BaseStoreModel baseStoreModel) {
        validateParameterNotNull(baseStoreModel, "baseStore cannot be null");
        final WorldlineConfigurationModel currentWorldlineConfiguration = getWorldlineConfiguration(baseStoreModel);
        return currentWorldlineConfiguration.getMerchantID();
    }

    @Override
    public String getCurrentMerchantId() {
        return getMerchantId(baseStoreService.getCurrentBaseStore());
    }

    public void setBaseStoreService(BaseStoreService baseStoreService) {
        this.baseStoreService = baseStoreService;
    }

    public void setWorldlineConfigurationDao(WorldlineConfigurationDao worldlineConfigurationDao) {
        this.worldlineConfigurationDao = worldlineConfigurationDao;
    }
}
