package com.worldline.direct.dao;

import de.hybris.platform.b2bacceleratorservices.dao.impl.DefaultB2BAcceleratorCartToOrderCronJobModelDao;
import de.hybris.platform.commerceservices.search.flexiblesearch.data.SortQueryData;
import de.hybris.platform.commerceservices.search.pagedata.PageableData;
import de.hybris.platform.commerceservices.search.pagedata.SearchPageData;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.cronjob.model.TriggerModel;
import de.hybris.platform.orderscheduling.model.CartToOrderCronJobModel;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.SearchResult;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultWorldlineB2BAcceleratorCartToOrderCronJobModelDao extends DefaultB2BAcceleratorCartToOrderCronJobModelDao {
    private static final String FIND_CARTTOORDERCRONJOB_BY_USER_QUERY = "SELECT {" + CartToOrderCronJobModel._TYPECODE + ":"
            + CartToOrderCronJobModel.PK + "} FROM { " + CartToOrderCronJobModel._TYPECODE + " as "
            + CartToOrderCronJobModel._TYPECODE + " JOIN " + CartModel._TYPECODE + " as " + CartModel._TYPECODE + " ON {"
            + CartToOrderCronJobModel._TYPECODE + ":" + CartToOrderCronJobModel.CART + "} = {" + CartModel._TYPECODE + ":"
            + CartModel.PK + "}} WHERE {" + CartModel._TYPECODE + ":" + CartModel.USER + "} = ?user AND {" + CartToOrderCronJobModel._TYPECODE + ":" + CartToOrderCronJobModel.SUBMITTED + "} = ?" + CartToOrderCronJobModel.SUBMITTED;


    private static final String FIND_CARTTOORDERCRONJOB_TRIGGER_BY_USER_QUERY = "SELECT {" + CartToOrderCronJobModel._TYPECODE
            + ":" + CartToOrderCronJobModel.PK + "} FROM { " + CartToOrderCronJobModel._TYPECODE + " as "
            + CartToOrderCronJobModel._TYPECODE + " JOIN " + CartModel._TYPECODE + " as " + CartModel._TYPECODE + " ON {"
            + CartToOrderCronJobModel._TYPECODE + ":" + CartToOrderCronJobModel.CART + "} = {" + CartModel._TYPECODE + ":"
            + CartModel.PK + "} LEFT JOIN " + TriggerModel._TYPECODE + " as " + TriggerModel._TYPECODE + " ON {"
            + CartToOrderCronJobModel._TYPECODE + ":" + CartToOrderCronJobModel.PK + "} = {" + TriggerModel._TYPECODE + ":"
            + TriggerModel.CRONJOB + "}} WHERE {" + CartModel._TYPECODE + ":" + CartModel.USER + "} = ?user AND {" + CartToOrderCronJobModel._TYPECODE + ":" + CartToOrderCronJobModel.SUBMITTED + "} = ?" + CartToOrderCronJobModel.SUBMITTED;

    private static final String FIND_CARTTOORDERCRONJOB_BY_CODE_AND_USER_QUERY = "SELECT {" + CartToOrderCronJobModel._TYPECODE + ":"
            + CartToOrderCronJobModel.PK + "} FROM { " + CartToOrderCronJobModel._TYPECODE + " as "
            + CartToOrderCronJobModel._TYPECODE + " JOIN " + CartModel._TYPECODE + " as " + CartModel._TYPECODE + " ON {"
            + CartToOrderCronJobModel._TYPECODE + ":" + CartToOrderCronJobModel.CART + "} = {" + CartModel._TYPECODE + ":"
            + CartModel.PK + "}} WHERE {" + CartModel._TYPECODE + ":" + CartModel.USER + "} = ?user  AND {"
            + CartToOrderCronJobModel._TYPECODE + ":" + CartToOrderCronJobModel.CODE + "} = ?code";

    private static final String SORT_JOBS_BY_DATE = " ORDER BY {" + CartToOrderCronJobModel._TYPECODE + ":"
            + CartToOrderCronJobModel.CREATIONTIME + "} DESC, {" + CartToOrderCronJobModel._TYPECODE + ":"
            + CartToOrderCronJobModel.PK + "}";

    private static final String SORT_JOBS_BY_CODE = " ORDER BY {" + CartToOrderCronJobModel._TYPECODE + ":"
            + CartToOrderCronJobModel.CODE + "}, {" + CartToOrderCronJobModel._TYPECODE + ":" + CartToOrderCronJobModel.CREATIONTIME
            + "} DESC, {" + CartToOrderCronJobModel._TYPECODE + ":" + CartToOrderCronJobModel.PK + "}";

    private static final String SORT_JOBS_BY_ACTIVATIONTIME = " ORDER BY {" + TriggerModel._TYPECODE + ":"
            + TriggerModel.ACTIVATIONTIME + "} ASC, {" + CartToOrderCronJobModel._TYPECODE + ":" + CartToOrderCronJobModel.CREATIONTIME
            + "} DESC, {" + CartToOrderCronJobModel._TYPECODE + ":" + CartToOrderCronJobModel.PK + "}";

    @Override
    public CartToOrderCronJobModel findCartToOrderCronJobByCode(final String code, final UserModel user) {
        final Map<String, Object> attr = new HashMap<String, Object>(2);
        attr.put(CartToOrderCronJobModel.CODE, code);
        attr.put(CartModel.USER, user);
        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_CARTTOORDERCRONJOB_BY_CODE_AND_USER_QUERY);
        query.getQueryParameters().putAll(attr);
        final SearchResult<CartToOrderCronJobModel> jobs = this.getFlexibleSearchService().search(query);
        final List<CartToOrderCronJobModel> result = jobs.getResult();
        return (result.iterator().hasNext() ? result.iterator().next() : null);
    }

    @Override
    public List<CartToOrderCronJobModel> findCartToOrderCronJobsByUser(final UserModel user) {
        final Map<String, Object> attr = new HashMap<String, Object>(1);
        attr.put(OrderModel.USER, user);
        attr.put(CartToOrderCronJobModel.SUBMITTED, true);
        final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_CARTTOORDERCRONJOB_BY_USER_QUERY + SORT_JOBS_BY_DATE);
        query.getQueryParameters().putAll(attr);
        final SearchResult<CartToOrderCronJobModel> result = this.getFlexibleSearchService().search(query);
        return result.getResult();
    }


    @Override
    public SearchPageData<CartToOrderCronJobModel> findPagedCartToOrderCronJobsByUser(final UserModel user,
                                                                                      final PageableData pageableData) {
        final Map<String, Object> queryParams = new HashMap<>(1);
        queryParams.put(OrderModel.USER, user);
        queryParams.put(CartToOrderCronJobModel.SUBMITTED, true);

        final List<SortQueryData> sortQueries = Arrays
                .asList(
                        createSortQueryData("byDate", FIND_CARTTOORDERCRONJOB_BY_USER_QUERY + SORT_JOBS_BY_DATE),
                        createSortQueryData("byReplenishmentNumber", FIND_CARTTOORDERCRONJOB_BY_USER_QUERY + SORT_JOBS_BY_CODE),
                        createSortQueryData("byNextOrderDate", FIND_CARTTOORDERCRONJOB_TRIGGER_BY_USER_QUERY + SORT_JOBS_BY_ACTIVATIONTIME));

        return getPagedFlexibleSearchService().search(sortQueries, "byDate", queryParams, pageableData);
    }
}
