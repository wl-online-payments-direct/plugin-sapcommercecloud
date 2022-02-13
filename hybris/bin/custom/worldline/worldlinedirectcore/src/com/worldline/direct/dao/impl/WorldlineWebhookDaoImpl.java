package com.worldline.direct.dao.impl;

import java.util.Arrays;
import java.util.List;

import com.worldline.direct.dao.WorldlineWebhookDao;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.worldline.direct.enums.WorldlineWebhooksEventStatusEnum;
import com.worldline.direct.model.WorldlineWebhooksEventModel;

public class WorldlineWebhookDaoImpl implements WorldlineWebhookDao {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldlineWebhookDaoImpl.class);

    private static final String Q_NON_PROCESSED = "SELECT {pk} FROM {" + WorldlineWebhooksEventModel._TYPECODE + "} " +
            "WHERE {" + WorldlineWebhooksEventModel.STATUS + "} in (?status) order by {"+WorldlineWebhooksEventModel.CREATEDTIME+"} asc";

    private static final List<WorldlineWebhooksEventStatusEnum> statuses = Arrays.asList(
            WorldlineWebhooksEventStatusEnum.CREATED,
            WorldlineWebhooksEventStatusEnum.FAILED);

    private FlexibleSearchService flexibleSearchService;

    @Override
    public List<WorldlineWebhooksEventModel> getNonProcessedWebhooksEvents(int batchSize) {
        final FlexibleSearchQuery selectNonProcessedWebhooksEventQuery = new FlexibleSearchQuery(Q_NON_PROCESSED);
        selectNonProcessedWebhooksEventQuery.setCount(batchSize);
        selectNonProcessedWebhooksEventQuery.addQueryParameter(WorldlineWebhooksEventModel.STATUS, statuses);

        LOGGER.debug("[WORLDLINE] Querying Non Processed WorldlineWebhooksEvent");
        final SearchResult<WorldlineWebhooksEventModel> search = flexibleSearchService.search(selectNonProcessedWebhooksEventQuery);
        LOGGER.debug("[WORLDLINE] {} items found ", search.getCount());

        return search.getResult();
    }

    public void setFlexibleSearchService(FlexibleSearchService flexibleSearchService) {
        this.flexibleSearchService = flexibleSearchService;
    }
}
