package com.ingenico.ogone.direct.factory;

import com.ingenico.direct.Client;
import com.ingenico.ogone.direct.model.IngenicoConfigurationModel;

public interface IngenicoClientFactory {

    Client getClient();

    Client getClient(IngenicoConfigurationModel ingenicoConfigurationModel);
}
