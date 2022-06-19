package com.worldline.direct.factory;

import com.onlinepayments.Client;
import com.worldline.direct.model.WorldlineConfigurationModel;

public interface WorldlineClientFactory {

    Client getClient();

    Client getClient(WorldlineConfigurationModel worldlineConfigurationModel);
}
