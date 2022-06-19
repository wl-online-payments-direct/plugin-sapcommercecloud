package com.worldline.direct.service.impl;

import com.onlinepayments.Client;
import com.onlinepayments.domain.TestConnection;
import com.worldline.direct.factory.WorldlineClientFactory;
import com.worldline.direct.model.WorldlineConfigurationModel;
import com.worldline.direct.service.WorldlineServiceMethodsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class WorldlineServiceMethodServiceImpl implements WorldlineServiceMethodsService {
    private final static Logger LOGGER = LoggerFactory.getLogger(WorldlinePaymentServiceImpl.class);

    private WorldlineClientFactory worldlineClientFactory;

    @Override
    public String testConnection(WorldlineConfigurationModel worldlineConfigurationModel) {
        try (Client client = worldlineClientFactory.getClient(worldlineConfigurationModel)) {
            final TestConnection testConnection = client.merchant(worldlineConfigurationModel.getMerchantID()).services().testConnection();
         return testConnection.getResult();
      } catch (IOException e) {
         LOGGER.error("[ WORLDLINE ] Errors during performing Test connection operation", e);
         return e.getMessage();
      }
   }

   public void setWorldlineClientFactory(WorldlineClientFactory worldlineClientFactory) {
      this.worldlineClientFactory = worldlineClientFactory;
   }
}
