package com.ingenico.ogone.direct.dao.impl;

import java.util.Arrays;
import java.util.List;

import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingenico.ogone.direct.dao.IngenicoWebhookDao;
import com.ingenico.ogone.direct.enums.IngenicoWebhooksEventStatusEnum;
import com.ingenico.ogone.direct.model.IngenicoWebhooksEventModel;

public class IngenicoWebhookDaoImpl implements IngenicoWebhookDao {
    private static final Logger LOGGER = LoggerFactory.getLogger(IngenicoWebhookDaoImpl.class);

    private static final String Q_NON_PROCESSED = "SELECT {pk} FROM {" + IngenicoWebhooksEventModel._TYPECODE + "} " +
            "WHERE {" + IngenicoWebhooksEventModel.STATUS + "} in (?status) order by {"+IngenicoWebhooksEventModel.CREATEDTIME+"} asc";

    private static final List<IngenicoWebhooksEventStatusEnum> statuses = Arrays.asList(
            IngenicoWebhooksEventStatusEnum.CREATED,
            IngenicoWebhooksEventStatusEnum.FAILED);

    private FlexibleSearchService flexibleSearchService;

    @Override
    public List<IngenicoWebhooksEventModel> getNonProcessedWebhooksEvents(int batchSize) {
        final FlexibleSearchQuery selectNonProcessedWebhooksEventQuery = new FlexibleSearchQuery(Q_NON_PROCESSED);
        selectNonProcessedWebhooksEventQuery.setCount(batchSize);
        selectNonProcessedWebhooksEventQuery.addQueryParameter(IngenicoWebhooksEventModel.STATUS, statuses);

        LOGGER.debug("[INGENICO] Querying Non Processed IngenicoWebhooksEvent");
        final SearchResult<IngenicoWebhooksEventModel> search = flexibleSearchService.search(selectNonProcessedWebhooksEventQuery);
        LOGGER.debug("[INGENICO] {} items found ", search.getCount());

        return search.getResult();
    }

    public void setFlexibleSearchService(FlexibleSearchService flexibleSearchService) {
        this.flexibleSearchService = flexibleSearchService;
    }
}
