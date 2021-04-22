package com.ingenico.ogone.direct.factory.impl;

import java.net.URI;
import java.net.URISyntaxException;

import de.hybris.platform.store.BaseStoreModel;
import de.hybris.platform.store.services.BaseStoreService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;

import com.ingenico.direct.Client;
import com.ingenico.direct.CommunicatorConfiguration;
import com.ingenico.direct.Factory;
import com.ingenico.ogone.direct.factory.IngenicoClientFactory;
import com.ingenico.ogone.direct.model.IngenicoConfigurationModel;
import com.ingenico.ogone.direct.service.IngenicoConfigurationService;

public class IngenicoClientFactoryImpl implements IngenicoClientFactory {
    private final static Logger LOGGER = LoggerFactory.getLogger(IngenicoClientFactoryImpl.class);

    private BaseStoreService baseStoreService;
    private CommunicatorConfiguration communicatorConfiguration;
    private IngenicoConfigurationService ingenicoConfigurationService;


    public Client getClient() {
        final BaseStoreModel currentBaseStore = baseStoreService.getCurrentBaseStore();
        return (Client) Factory.createClient(getCommunicatorConfiguration(currentBaseStore));
    }

    @Cacheable(value = "communicatorConfiguration", key = "#currentBaseStore")
    public CommunicatorConfiguration getCommunicatorConfiguration(BaseStoreModel currentBaseStore) {
        final IngenicoConfigurationModel ingenicoConfiguration = ingenicoConfigurationService.getCurrentIngenicoConfiguration();
        return communicatorConfiguration.withApiEndpoint(createURI(ingenicoConfiguration.getEndpointURL()))
                .withApiKeyId(ingenicoConfiguration.getApiKey())
                .withSecretApiKey(ingenicoConfiguration.getApiSecret());
    }

    private URI createURI(String host) {
        try {
            return new URI(host);
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Unable to construct API endpoint URI", exception);
        }
    }

    public void setCommunicatorConfiguration(CommunicatorConfiguration communicatorConfiguration) {
        this.communicatorConfiguration = communicatorConfiguration;
    }

    public void setBaseStoreService(BaseStoreService baseStoreService) {
        this.baseStoreService = baseStoreService;
    }

    public void setIngenicoConfigurationService(IngenicoConfigurationService ingenicoConfigurationService) {
        this.ingenicoConfigurationService = ingenicoConfigurationService;
    }
}
