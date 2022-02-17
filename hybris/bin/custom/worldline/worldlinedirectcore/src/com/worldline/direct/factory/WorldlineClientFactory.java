package com.worldline.direct.factory;

import com.ingenico.direct.Client;
import com.worldline.direct.model.WorldlineConfigurationModel;

public interface WorldlineClientFactory {

    Client getClient();

    Client getClient(WorldlineConfigurationModel worldlineConfigurationModel);
}
