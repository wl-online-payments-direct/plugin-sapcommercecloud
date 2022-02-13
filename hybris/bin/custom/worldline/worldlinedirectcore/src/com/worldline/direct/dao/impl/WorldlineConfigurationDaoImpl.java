package com.worldline.direct.dao.impl;

import com.worldline.direct.dao.WorldlineConfigurationDao;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;

import com.worldline.direct.model.WorldlineConfigurationModel;

public class WorldlineConfigurationDaoImpl implements WorldlineConfigurationDao {

    private static final String Q_BY_KEY = "SELECT {" + WorldlineConfigurationModel.PK + "} FROM {" + WorldlineConfigurationModel._TYPECODE
            + "} WHERE {" + WorldlineConfigurationModel.WEBHOOKKEYID + "} = ?webhookKeyId";

    private static final String Q_BY_MERCHANT = "SELECT {" + WorldlineConfigurationModel.PK + "} FROM {" + WorldlineConfigurationModel._TYPECODE
            + "} WHERE {" + WorldlineConfigurationModel.MERCHANTID + "} = ?merchantId";

    private FlexibleSearchService flexibleSearchService;

    @Override
    public WorldlineConfigurationModel findWorldlineConfigurationByWebhookKey(String webhookKey) {
        FlexibleSearchQuery flexibleSearchQuery = new FlexibleSearchQuery(Q_BY_KEY);
        flexibleSearchQuery.addQueryParameter(WorldlineConfigurationModel.WEBHOOKKEYID, webhookKey);

        return flexibleSearchService.searchUnique(flexibleSearchQuery);
    }

    @Override
    public WorldlineConfigurationModel findWorldlineConfigurationByMerchantId(String merchantId) {
        FlexibleSearchQuery flexibleSearchQuery = new FlexibleSearchQuery(Q_BY_MERCHANT);
        flexibleSearchQuery.addQueryParameter(WorldlineConfigurationModel.MERCHANTID, merchantId);

        return flexibleSearchService.searchUnique(flexibleSearchQuery);
    }

    public void setFlexibleSearchService(FlexibleSearchService flexibleSearchService) {
        this.flexibleSearchService = flexibleSearchService;
    }
}
