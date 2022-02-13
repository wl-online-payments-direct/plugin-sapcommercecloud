package com.worldline.direct.factory.impl;

import java.net.URI;
import java.net.URISyntaxException;

import com.worldline.direct.service.WorldlineConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingenico.direct.Client;
import com.ingenico.direct.CommunicatorConfiguration;
import com.ingenico.direct.Factory;
import com.worldline.direct.factory.WorldlineClientFactory;
import com.worldline.direct.model.WorldlineConfigurationModel;

public class WorldlineClientFactoryImpl implements WorldlineClientFactory {
    private final static Logger LOGGER = LoggerFactory.getLogger(WorldlineClientFactoryImpl.class);

    private CommunicatorConfiguration communicatorConfiguration;
    private WorldlineConfigurationService worldlineConfigurationService;


    public Client getClient() {
        return (Client) Factory.createClient(getCommunicatorConfiguration());
    }

    public Client getClient(WorldlineConfigurationModel worldlineConfigurationModel) {
        return (Client) Factory.createClient(getCommunicatorConfiguration(worldlineConfigurationModel));
    }

    private CommunicatorConfiguration getCommunicatorConfiguration() {
        final WorldlineConfigurationModel worldlineConfiguration = worldlineConfigurationService.getCurrentWorldlineConfiguration();
        return getCommunicatorConfiguration(worldlineConfiguration);
    }

    private CommunicatorConfiguration getCommunicatorConfiguration(WorldlineConfigurationModel worldlineConfiguration) {
        return communicatorConfiguration.withApiEndpoint(createURI(worldlineConfiguration.getEndpointURL()))
              .withApiKeyId(worldlineConfiguration.getApiKey())
              .withSecretApiKey(worldlineConfiguration.getApiSecret());
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

    public void setWorldlineConfigurationService(WorldlineConfigurationService worldlineConfigurationService) {
        this.worldlineConfigurationService = worldlineConfigurationService;
    }
}
