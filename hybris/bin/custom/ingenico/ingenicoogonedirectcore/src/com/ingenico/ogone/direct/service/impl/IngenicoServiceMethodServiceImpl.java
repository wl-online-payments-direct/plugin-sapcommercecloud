package com.ingenico.ogone.direct.service.impl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import com.ingenico.direct.Client;
import com.ingenico.direct.CommunicatorConfiguration;
import com.ingenico.direct.Factory;
import com.ingenico.direct.domain.TestConnection;
import com.ingenico.ogone.direct.model.IngenicoConfigurationModel;
import com.ingenico.ogone.direct.service.IngenicoServiceMethodsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IngenicoServiceMethodServiceImpl implements IngenicoServiceMethodsService {
   private final static Logger LOGGER = LoggerFactory.getLogger(IngenicoPaymentServiceImpl.class);

   private CommunicatorConfiguration communicatorConfiguration;

   @Override
   public String testConnection(IngenicoConfigurationModel ingenicoConfigurationModel) {
      try (Client client = (Client) Factory.createClient(getCommunicatorConfiguration(ingenicoConfigurationModel))) {
         final TestConnection testConnection = client.merchant(ingenicoConfigurationModel.getMerchantID()).services().testConnection();
         return testConnection.getResult();
      } catch (IOException e) {
         LOGGER.error("[ INGENICO ] Errors during performing Test connection operation", e);
         return e.getMessage();
      }
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
}
