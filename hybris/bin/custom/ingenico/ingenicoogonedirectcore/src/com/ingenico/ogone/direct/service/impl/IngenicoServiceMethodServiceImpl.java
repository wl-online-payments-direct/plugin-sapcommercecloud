package com.ingenico.ogone.direct.service.impl;

import java.io.IOException;

import com.ingenico.direct.Client;
import com.ingenico.direct.domain.TestConnection;
import com.ingenico.ogone.direct.factory.IngenicoClientFactory;
import com.ingenico.ogone.direct.model.IngenicoConfigurationModel;
import com.ingenico.ogone.direct.service.IngenicoServiceMethodsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IngenicoServiceMethodServiceImpl implements IngenicoServiceMethodsService {
   private final static Logger LOGGER = LoggerFactory.getLogger(IngenicoPaymentServiceImpl.class);

   private IngenicoClientFactory ingenicoClientFactory;

   @Override
   public String testConnection(IngenicoConfigurationModel ingenicoConfigurationModel) {
      try (Client client = ingenicoClientFactory.getClient(ingenicoConfigurationModel)) {
         final TestConnection testConnection = client.merchant(ingenicoConfigurationModel.getMerchantID()).services().testConnection();
         return testConnection.getResult();
      } catch (IOException e) {
         LOGGER.error("[ INGENICO ] Errors during performing Test connection operation", e);
         return e.getMessage();
      }
   }

   public void setIngenicoClientFactory(IngenicoClientFactory ingenicoClientFactory) {
      this.ingenicoClientFactory = ingenicoClientFactory;
   }
}
