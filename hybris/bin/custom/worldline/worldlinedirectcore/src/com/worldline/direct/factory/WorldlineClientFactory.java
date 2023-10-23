package com.worldline.direct.factory;

import com.onlinepayments.Client;
import com.onlinepayments.merchant.MerchantClient;
import com.worldline.direct.model.WorldlineConfigurationModel;

public interface WorldlineClientFactory {

    Client getClient();

    Client getClient(WorldlineConfigurationModel worldlineConfigurationModel);

    MerchantClient getMerchantClient(String storeId, String pspid);
}
