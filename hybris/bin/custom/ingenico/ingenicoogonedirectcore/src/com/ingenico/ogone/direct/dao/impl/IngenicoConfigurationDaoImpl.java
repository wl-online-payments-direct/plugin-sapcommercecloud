package com.ingenico.ogone.direct.dao.impl;

import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;

import com.ingenico.ogone.direct.dao.IngenicoConfigurationDao;
import com.ingenico.ogone.direct.model.IngenicoConfigurationModel;

public class IngenicoConfigurationDaoImpl implements IngenicoConfigurationDao {

    private static final String Q_BY_KEY = "SELECT {" + IngenicoConfigurationModel.PK + "} FROM {" + IngenicoConfigurationModel._TYPECODE
            + "} WHERE {" + IngenicoConfigurationModel.WEBHOOKKEYID + "} = ?webhookKeyId";

    private static final String Q_BY_MERCHANT = "SELECT {" + IngenicoConfigurationModel.PK + "} FROM {" + IngenicoConfigurationModel._TYPECODE
            + "} WHERE {" + IngenicoConfigurationModel.MERCHANTID + "} = ?merchantId";

    private FlexibleSearchService flexibleSearchService;

    @Override
    public IngenicoConfigurationModel findIngenicoConfigurationByWebhookKey(String webhookKey) {
        FlexibleSearchQuery flexibleSearchQuery = new FlexibleSearchQuery(Q_BY_KEY);
        flexibleSearchQuery.addQueryParameter(IngenicoConfigurationModel.WEBHOOKKEYID, webhookKey);

        return flexibleSearchService.searchUnique(flexibleSearchQuery);
    }

    @Override
    public IngenicoConfigurationModel findIngenicoConfigurationByMerchantId(String merchantId) {
        FlexibleSearchQuery flexibleSearchQuery = new FlexibleSearchQuery(Q_BY_MERCHANT);
        flexibleSearchQuery.addQueryParameter(IngenicoConfigurationModel.MERCHANTID, merchantId);

        return flexibleSearchService.searchUnique(flexibleSearchQuery);
    }

    public void setFlexibleSearchService(FlexibleSearchService flexibleSearchService) {
        this.flexibleSearchService = flexibleSearchService;
    }
}
