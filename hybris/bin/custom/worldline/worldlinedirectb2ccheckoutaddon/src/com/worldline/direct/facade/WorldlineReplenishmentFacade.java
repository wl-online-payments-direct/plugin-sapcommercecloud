package com.worldline.direct.facade;

import de.hybris.platform.b2bacceleratorfacades.order.data.ScheduledCartData;
import de.hybris.platform.commercefacades.order.data.OrderHistoryData;
import de.hybris.platform.commerceservices.search.pagedata.PageableData;
import de.hybris.platform.commerceservices.search.pagedata.SearchPageData;

public interface WorldlineReplenishmentFacade {

    SearchPageData<ScheduledCartData> getPagedReplenishmentHistory(PageableData pageableData);

    void cancelReplenishment(String jobCode, String user);

    ScheduledCartData getReplenishmentOrderDetailsForCode(String code, String user);

    SearchPageData<? extends OrderHistoryData> getPagedReplenishmentOrderHistory(String jobCode, PageableData pageableData);
}
