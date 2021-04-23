package com.ingenico.ogone.direct.service.impl;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

import java.util.Objects;

import de.hybris.platform.store.BaseStoreModel;
import de.hybris.platform.store.services.BaseStoreService;

import org.springframework.cache.annotation.Cacheable;

import com.ingenico.ogone.direct.exception.IngenicoConfigurationNotFoundException;
import com.ingenico.ogone.direct.model.IngenicoConfigurationModel;
import com.ingenico.ogone.direct.service.IngenicoConfigurationService;

public class IngenicoConfigurationServiceImpl implements IngenicoConfigurationService {

    private BaseStoreService baseStoreService;

    @Override
    @Cacheable(value = "ingenicoConfiguration", key = "T(com.ingenico.ogone.direct.cache.IngenicoCacheKeyGenerator).generateKey(false,#baseStoreModel.uid,'config')")
    public IngenicoConfigurationModel getIngenicoConfiguration(final BaseStoreModel baseStoreModel) {
        validateParameterNotNull(baseStoreModel, "baseStore cannot be null");
        final IngenicoConfigurationModel ingenicoConfiguration = baseStoreModel.getIngenicoConfiguration();
        if (Objects.isNull(ingenicoConfiguration)) {
            throw new IngenicoConfigurationNotFoundException(baseStoreModel.getUid());
        }
        return ingenicoConfiguration;
    }

    @Override
    @Cacheable(value = "ingenicoConfiguration", key = "T(com.ingenico.ogone.direct.cache.IngenicoCacheKeyGenerator).generateKey(true,'config')")
    public IngenicoConfigurationModel getCurrentIngenicoConfiguration() {
        return getIngenicoConfiguration(baseStoreService.getCurrentBaseStore());
    }

    @Override
    @Cacheable(value = "ingenicoConfiguration", key = "T(com.ingenico.ogone.direct.cache.IngenicoCacheKeyGenerator).generateKey(false,#baseStoreModel.uid,'merchant')")
    public String getMerchantId(final BaseStoreModel baseStoreModel) {
        validateParameterNotNull(baseStoreModel, "baseStore cannot be null");
        final IngenicoConfigurationModel currentIngenicoConfiguration = getIngenicoConfiguration(baseStoreModel);
        return currentIngenicoConfiguration.getMerchantID();
    }

    @Override
    @Cacheable(value = "ingenicoConfiguration", key = "T(com.ingenico.ogone.direct.cache.IngenicoCacheKeyGenerator).generateKey(true,'merchant')")
    public String getCurrentMerchantId() {
        return getMerchantId(baseStoreService.getCurrentBaseStore());
    }


    public void setBaseStoreService(BaseStoreService baseStoreService) {
        this.baseStoreService = baseStoreService;
    }
}
