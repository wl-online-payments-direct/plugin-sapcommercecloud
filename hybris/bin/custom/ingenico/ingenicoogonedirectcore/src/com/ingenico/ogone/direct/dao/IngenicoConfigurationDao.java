package com.ingenico.ogone.direct.dao;

import com.ingenico.ogone.direct.model.IngenicoConfigurationModel;

public interface IngenicoConfigurationDao {

    IngenicoConfigurationModel findIngenicoConfigurationByWebhookKey(String apiKey);

    IngenicoConfigurationModel findIngenicoConfigurationByMerchantId(String merchantId);
}
