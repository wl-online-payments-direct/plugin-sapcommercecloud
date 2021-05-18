package com.ingenico.ogone.direct.service;

import de.hybris.platform.store.BaseStoreModel;

import com.ingenico.ogone.direct.model.IngenicoConfigurationModel;

public interface IngenicoConfigurationService {

    IngenicoConfigurationModel getIngenicoConfiguration(BaseStoreModel baseStoreModel);

    IngenicoConfigurationModel getCurrentIngenicoConfiguration();

    String getMerchantId(BaseStoreModel baseStoreModel);

    String getCurrentMerchantId();

    String testConnection();
}
