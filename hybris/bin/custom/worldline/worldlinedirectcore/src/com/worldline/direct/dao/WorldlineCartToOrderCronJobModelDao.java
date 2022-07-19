package com.worldline.direct.dao;

import de.hybris.platform.commerceservices.search.pagedata.PageableData;
import de.hybris.platform.commerceservices.search.pagedata.SearchPageData;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.orderscheduling.model.CartToOrderCronJobModel;

import java.util.List;


public interface WorldlineCartToOrderCronJobModelDao {
    /**
     * Gets the Scheduling job by code
     *
     * @param code Unique job identifier
     * @param user A user assigned to the cart
     * @return The CartToOrderCronJobModel identified by <param>code</param>
     */
    CartToOrderCronJobModel findCartToOrderCronJobByCode(String code, UserModel user);

    /**
     * Gets all order replenishment cron jobs for a given user.
     *
     * @param user A user
     * @return Replenishment cron jobs created by a user.
     */
    List<? extends CartToOrderCronJobModel> findCartToOrderCronJobsByUser(UserModel user);

    /**
     * Gets all order replenishment cron jobs for a given user.
     *
     * @param user         A user
     * @param pageableData Pagination info
     * @return Replenishment cron jobs created by a user.
     */
    SearchPageData<CartToOrderCronJobModel> findPagedCartToOrderCronJobsByUser(UserModel user, PageableData pageableData);

    /**
     * All orders created by a replenishment cron job
     *
     * @param jobCode      Unique cron job id
     * @param pageableData Pagination info
     * @return Orders created by a replenishment cron job
     */
    SearchPageData<OrderModel> findOrderByJob(String jobCode, PageableData pageableData);
}
