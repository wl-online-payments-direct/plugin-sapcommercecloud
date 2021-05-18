package com.ingenico.ogone.direct.service.impl;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

import java.io.IOException;
import java.util.Objects;

import com.ingenico.direct.Client;
import com.ingenico.direct.domain.TestConnection;
import com.ingenico.ogone.direct.factory.IngenicoClientFactory;
import de.hybris.platform.store.BaseStoreModel;
import de.hybris.platform.store.services.BaseStoreService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;

import com.ingenico.ogone.direct.exception.IngenicoConfigurationNotFoundException;
import com.ingenico.ogone.direct.model.IngenicoConfigurationModel;
import com.ingenico.ogone.direct.service.IngenicoConfigurationService;

public class IngenicoConfigurationServiceImpl implements IngenicoConfigurationService {
    private final static Logger LOGGER = LoggerFactory.getLogger(IngenicoPaymentServiceImpl.class);

    private BaseStoreService baseStoreService;
    private IngenicoClientFactory ingenicoClientFactory;

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

    @Override public String testConnection() {
        String merchantId = getCurrentMerchantId();
        try (Client client = ingenicoClientFactory.getClient()) {
           final TestConnection testConnection = client.merchant(merchantId).services().testConnection();
           return testConnection.getResult();
        } catch (IOException e) {
            LOGGER.error("[ INGENICO ] Errors during getting PaymentProducts ", e);
            //TODO Throw Logical Exception
            return null;
        }
    }

    public void setBaseStoreService(BaseStoreService baseStoreService) {
        this.baseStoreService = baseStoreService;
    }

    public void setIngenicoClientFactory(IngenicoClientFactory ingenicoClientFactory) {
        this.ingenicoClientFactory = ingenicoClientFactory;
    }

}
