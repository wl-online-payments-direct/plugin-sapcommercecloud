package com.ingenico.ogone.direct.factory.impl;

import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.*;

import java.util.Properties;

import de.hybris.platform.servicelayer.config.ConfigurationService;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingenico.direct.Client;
import com.ingenico.direct.CommunicatorConfiguration;
import com.ingenico.direct.Factory;
import com.ingenico.ogone.direct.factory.IngenicoClientFactory;

public class IngenicoClientFactoryImpl implements IngenicoClientFactory {
    private final static Logger LOGGER = LoggerFactory.getLogger(IngenicoClientFactoryImpl.class);

    private ConfigurationService configurationService;


    public Client getClient() {
        return (Client) Factory.createClient(getIngenicoConfiguration());
    }

    /*
   Possible Cache
    */
    private CommunicatorConfiguration getIngenicoConfiguration() {
        Properties properties = new Properties();

        properties.computeIfAbsent(DIRECT_API_AUTHORIZATION_TYPE, key -> getConfiguration().getString(INGENICO_DIRECT_API_AUTHORIZATION_TYPE, null));
        properties.computeIfAbsent(DIRECT_API_CONNECT_TIMEOUT, key -> getConfiguration().getInt(INGENICO_DIRECT_API_CONNECT_TIMEOUT));
        properties.computeIfAbsent(DIRECT_API_SOCKET_TIMEOUT, key -> getConfiguration().getInt(INGENICO_DIRECT_API_SOCKET_TIMEOUT));
        properties.computeIfAbsent(DIRECT_API_MAX_CONNECTIONS, key -> getConfiguration().getInt(INGENICO_DIRECT_API_MAX_CONNECTIONS));

        properties.computeIfAbsent(DIRECT_API_PROXY_URI, key -> getConfiguration().getString(INGENICO_DIRECT_API_PROXY_URI, null));
        properties.computeIfAbsent(DIRECT_API_PROXY_USERNAME, key -> getConfiguration().getString(INGENICO_DIRECT_API_PROXY_USERNAME, null));
        properties.computeIfAbsent(DIRECT_API_PROXY_PASSWORD, key -> getConfiguration().getString(INGENICO_DIRECT_API_PROXY_PASSWORD, null));

        properties.computeIfAbsent(DIRECT_API_HTTPS_PROTOCOLS, key -> getConfiguration().getString(INGENICO_DIRECT_API_HTTPS_PROTOCOLS, null));

        properties.computeIfAbsent(DIRECT_API_INTEGRATOR, key -> getConfiguration().getString(INGENICO_DIRECT_API_INTEGRATOR, null));

        properties.computeIfAbsent(DIRECT_API_SHOPPING_CART_EXTENSION_CREATOR, key -> getConfiguration().getString(INGENICO_DIRECT_API_SHOPPING_CART_EXTENSION_CREATOR, null));
        properties.computeIfAbsent(DIRECT_API_SHOPPING_CART_EXTENSION_NAME, key -> getConfiguration().getString(INGENICO_DIRECT_API_SHOPPING_CART_EXTENSION_NAME, null));
        properties.computeIfAbsent(DIRECT_API_SHOPPING_CART_EXTENSION_VERSION, key -> getConfiguration().getString(INGENICO_DIRECT_API_SHOPPING_CART_EXTENSION_VERSION, null));
        properties.computeIfAbsent(DIRECT_API_SHOPPING_CART_EXTENSION_EXTENSION_ID, key -> getConfiguration().getString(INGENICO_DIRECT_API_SHOPPING_CART_EXTENSION_EXTENSION_ID, null));

        properties.computeIfAbsent(DIRECT_API_ENDPOINT_HOST, key -> getConfiguration().getString(INGENICO_DIRECT_API_ENDPOINT_HOST, null));
        properties.computeIfAbsent(DIRECT_API_ENDPOINT_SCHEME, key -> getConfiguration().getString(INGENICO_DIRECT_API_ENDPOINT_SCHEME, null));
        properties.computeIfAbsent(DIRECT_API_ENDPOINT_PORT, key -> getConfiguration().getString(INGENICO_DIRECT_API_ENDPOINT_PORT, null));

        return new CommunicatorConfiguration(properties)
                .withApiKeyId(getConfiguration().getString(INGENICO_CONNECT_API_API_KEY_ID))
                .withSecretApiKey(getConfiguration().getString(INGENICO_CONNECT_API_SECRET_API_KEY));
    }

    private Configuration getConfiguration() {
        return configurationService.getConfiguration();
    }

    protected ConfigurationService getConfigurationService() {
        return configurationService;
    }

    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }
}
