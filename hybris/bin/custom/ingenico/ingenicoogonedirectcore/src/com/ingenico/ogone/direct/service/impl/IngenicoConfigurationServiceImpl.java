package com.ingenico.ogone.direct.service.impl;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

import java.util.Objects;

import de.hybris.platform.store.BaseStoreModel;
import de.hybris.platform.store.services.BaseStoreService;

import com.ingenico.ogone.direct.dao.IngenicoConfigurationDao;
import com.ingenico.ogone.direct.exception.IngenicoConfigurationNotFoundException;
import com.ingenico.ogone.direct.model.IngenicoConfigurationModel;
import com.ingenico.ogone.direct.service.IngenicoConfigurationService;

public class IngenicoConfigurationServiceImpl implements IngenicoConfigurationService {

    private BaseStoreService baseStoreService;
    private IngenicoConfigurationDao ingenicoConfigurationDao;

    @Override
    public IngenicoConfigurationModel getIngenicoConfigurationByWebhookKey(String webhookKey) {
        validateParameterNotNull(webhookKey, "webhookKey cannot be null");
        return ingenicoConfigurationDao.findIngenicoConfigurationByWebhookKey(webhookKey);
    }

    @Override
    public IngenicoConfigurationModel getIngenicoConfiguration(final BaseStoreModel baseStoreModel) {
        validateParameterNotNull(baseStoreModel, "baseStore cannot be null");
        final IngenicoConfigurationModel ingenicoConfiguration = baseStoreModel.getIngenicoConfiguration();
        if (Objects.isNull(ingenicoConfiguration)) {
            throw new IngenicoConfigurationNotFoundException(baseStoreModel.getUid());
        }
        return ingenicoConfiguration;
    }

    @Override
    public IngenicoConfigurationModel getCurrentIngenicoConfiguration() {
        return getIngenicoConfiguration(baseStoreService.getCurrentBaseStore());
    }

    @Override
    public String getMerchantId(final BaseStoreModel baseStoreModel) {
        validateParameterNotNull(baseStoreModel, "baseStore cannot be null");
        final IngenicoConfigurationModel currentIngenicoConfiguration = getIngenicoConfiguration(baseStoreModel);
        return currentIngenicoConfiguration.getMerchantID();
    }

    @Override
    public String getCurrentMerchantId() {
        return getMerchantId(baseStoreService.getCurrentBaseStore());
    }

    public void setBaseStoreService(BaseStoreService baseStoreService) {
        this.baseStoreService = baseStoreService;
    }

    public void setIngenicoConfigurationDao(IngenicoConfigurationDao ingenicoConfigurationDao) {
        this.ingenicoConfigurationDao = ingenicoConfigurationDao;
    }
}
