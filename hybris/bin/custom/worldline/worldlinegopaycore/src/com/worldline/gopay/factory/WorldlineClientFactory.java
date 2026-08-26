package com.worldline.gopay.factory;

import com.onlinepayments.Client;
import com.onlinepayments.merchant.MerchantClient;
import com.worldline.gopay.model.WorldlineConfigurationModel;

public interface WorldlineClientFactory {

    Client getClient();

    Client getClient(WorldlineConfigurationModel worldlineConfigurationModel);

    MerchantClient getMerchantClient(String storeId, String pspid);
}
