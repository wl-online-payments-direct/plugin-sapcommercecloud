package com.worldline.direct.factory.impl;

import com.onlinepayments.Client;
import com.onlinepayments.CommunicatorConfiguration;
import com.onlinepayments.Factory;
import com.onlinepayments.merchant.MerchantClient;
import com.worldline.direct.factory.WorldlineClientFactory;
import com.worldline.direct.model.WorldlineConfigurationModel;
import com.worldline.direct.service.WorldlineConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

public class WorldlineClientFactoryImpl implements WorldlineClientFactory {
    private final static Logger LOGGER = LoggerFactory.getLogger(WorldlineClientFactoryImpl.class);

    private CommunicatorConfiguration communicatorConfiguration;
    private WorldlineConfigurationService worldlineConfigurationService;

    private Map<String, Map.Entry<Client, MerchantClient>> openConnections;

    public MerchantClient getMerchantClient(String storeId, String pspid) {

        if (openConnections == null) {
            openConnections = new HashMap<>();
        }
        if (!openConnections.containsKey(storeId)) {
            Client client = (Client) Factory.createClient(getCommunicatorConfiguration(storeId));
            MerchantClient merchant = client.merchant(pspid);
            openConnections.put(storeId, new AbstractMap.SimpleEntry<>(client, merchant));
        }
        return openConnections.get(storeId).getValue();
    }

    public Client getClient() {
        return (Client) Factory.createClient(getCommunicatorConfiguration());
    }

    public Client getClient(WorldlineConfigurationModel worldlineConfigurationModel) {
        return (Client) Factory.createClient(getCommunicatorConfiguration(worldlineConfigurationModel));
    }

    private CommunicatorConfiguration getCommunicatorConfiguration(String storeId) {
        final WorldlineConfigurationModel worldlineConfiguration = worldlineConfigurationService.getWorldlineConfiguration(storeId);
        return getCommunicatorConfiguration(worldlineConfiguration);
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
