package com.ingenico.ogone.direct.factory.impl;

import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingenico.direct.Client;
import com.ingenico.direct.CommunicatorConfiguration;
import com.ingenico.direct.Factory;
import com.ingenico.ogone.direct.factory.IngenicoClientFactory;
import com.ingenico.ogone.direct.model.IngenicoConfigurationModel;
import com.ingenico.ogone.direct.service.IngenicoConfigurationService;

public class IngenicoClientFactoryImpl implements IngenicoClientFactory {
    private final static Logger LOGGER = LoggerFactory.getLogger(IngenicoClientFactoryImpl.class);

    private CommunicatorConfiguration communicatorConfiguration;
    private IngenicoConfigurationService ingenicoConfigurationService;


    public Client getClient() {
        return (Client) Factory.createClient(getCommunicatorConfiguration());
    }

    public Client getClient(IngenicoConfigurationModel ingenicoConfigurationModel) {
        return (Client) Factory.createClient(getCommunicatorConfiguration(ingenicoConfigurationModel));
    }

    private CommunicatorConfiguration getCommunicatorConfiguration() {
        final IngenicoConfigurationModel ingenicoConfiguration = ingenicoConfigurationService.getCurrentIngenicoConfiguration();
        return communicatorConfiguration.withApiEndpoint(createURI(ingenicoConfiguration.getEndpointURL()))
                .withApiKeyId(ingenicoConfiguration.getApiKey())
                .withSecretApiKey(ingenicoConfiguration.getApiSecret());
    }

    private CommunicatorConfiguration getCommunicatorConfiguration(IngenicoConfigurationModel ingenicoConfiguration) {
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

    public void setIngenicoConfigurationService(IngenicoConfigurationService ingenicoConfigurationService) {
        this.ingenicoConfigurationService = ingenicoConfigurationService;
    }
}
