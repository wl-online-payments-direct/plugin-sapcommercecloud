package com.worldline.direct.dao.impl;

import com.worldline.direct.dao.WorldlineCartToOrderCronJobModelDao;
import de.hybris.platform.commerceservices.search.flexiblesearch.PagedFlexibleSearchService;
import de.hybris.platform.commerceservices.search.flexiblesearch.data.SortQueryData;
import de.hybris.platform.commerceservices.search.pagedata.PageableData;
import de.hybris.platform.commerceservices.search.pagedata.SearchPageData;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.cronjob.model.TriggerModel;
import de.hybris.platform.orderscheduling.model.CartToOrderCronJobModel;
import de.hybris.platform.servicelayer.internal.dao.DefaultGenericDao;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.SearchResult;
import org.springframework.beans.factory.annotation.Required;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DefaultWorldlineCartToOrderCronJobModelDao extends DefaultGenericDao<CartToOrderCronJobModel> implements
		WorldlineCartToOrderCronJobModelDao
{

	private static final String FIND_CARTTOORDERCRONJOB_BY_USER_QUERY = "SELECT {" + CartToOrderCronJobModel._TYPECODE + ":"
			+ CartToOrderCronJobModel.PK + "} FROM { " + CartToOrderCronJobModel._TYPECODE + " as "
			+ CartToOrderCronJobModel._TYPECODE + " JOIN " + CartModel._TYPECODE + " as " + CartModel._TYPECODE + " ON {"
			+ CartToOrderCronJobModel._TYPECODE + ":" + CartToOrderCronJobModel.CART + "} = {" + CartModel._TYPECODE + ":"
			+ CartModel.PK + "}} WHERE {" + CartModel._TYPECODE + ":" + CartModel.USER + "} = ?user AND {" + CartToOrderCronJobModel._TYPECODE + ":" + CartToOrderCronJobModel.SUBMITTED + "} = ?"+CartToOrderCronJobModel.SUBMITTED;


	private static final String FIND_CARTTOORDERCRONJOB_TRIGGER_BY_USER_QUERY = "SELECT {" + CartToOrderCronJobModel._TYPECODE
			+ ":" + CartToOrderCronJobModel.PK + "} FROM { " + CartToOrderCronJobModel._TYPECODE + " as "
			+ CartToOrderCronJobModel._TYPECODE + " JOIN " + CartModel._TYPECODE + " as " + CartModel._TYPECODE + " ON {"
			+ CartToOrderCronJobModel._TYPECODE + ":" + CartToOrderCronJobModel.CART + "} = {" + CartModel._TYPECODE + ":"
			+ CartModel.PK + "} LEFT JOIN " + TriggerModel._TYPECODE + " as " + TriggerModel._TYPECODE + " ON {"
			+ CartToOrderCronJobModel._TYPECODE + ":" + CartToOrderCronJobModel.PK + "} = {" + TriggerModel._TYPECODE + ":"
			+ TriggerModel.CRONJOB + "}} WHERE {" + CartModel._TYPECODE + ":" + CartModel.USER + "} = ?user AND {" + CartToOrderCronJobModel._TYPECODE + ":" + CartToOrderCronJobModel.SUBMITTED + "} = ?"+CartToOrderCronJobModel.SUBMITTED;

	private static final String FIND_CARTTOORDERCRONJOB_BY_CODE_AND_USER_QUERY = FIND_CARTTOORDERCRONJOB_BY_USER_QUERY + " AND {"
			+ CartToOrderCronJobModel._TYPECODE + ":" + CartToOrderCronJobModel.CODE + "} = ?code";

	private static final String FIND_ORDERS_FOR_SCHEDULE_BY_CODE_QUERY = "SELECT {" + OrderModel._TYPECODE + ":" + OrderModel.PK
			+ "} FROM { " + OrderModel._TYPECODE + " as " + OrderModel._TYPECODE + " JOIN " + CartToOrderCronJobModel._TYPECODE
			+ " as " + CartToOrderCronJobModel._TYPECODE + " ON {" + OrderModel._TYPECODE + ":" + OrderModel.SCHEDULINGCRONJOB
			+ " } = {" + CartToOrderCronJobModel._TYPECODE + ":" + CartToOrderCronJobModel.PK + " }} WHERE {"
			+ CartToOrderCronJobModel._TYPECODE + ":" + CartToOrderCronJobModel.CODE + "} = ?code"
            + " AND {" + OrderModel._TYPECODE + ":" + OrderModel.VERSIONID + "} IS NULL";


	private static final String SORT_JOBS_BY_DATE = " ORDER BY {" + CartToOrderCronJobModel._TYPECODE + ":"
			+ CartToOrderCronJobModel.CREATIONTIME + "} DESC, {" + CartToOrderCronJobModel._TYPECODE + ":"
			+ CartToOrderCronJobModel.PK + "}";

	private static final String SORT_JOBS_BY_CODE = " ORDER BY {" + CartToOrderCronJobModel._TYPECODE + ":"
			+ CartToOrderCronJobModel.CODE + "}, {" + CartToOrderCronJobModel._TYPECODE + ":" + CartToOrderCronJobModel.CREATIONTIME
			+ "} DESC, {" + CartToOrderCronJobModel._TYPECODE + ":" + CartToOrderCronJobModel.PK + "}";

	private static final String SORT_JOBS_BY_ACTIVATIONTIME = " ORDER BY {" + TriggerModel._TYPECODE + ":"
			+ TriggerModel.ACTIVATIONTIME + "} ASC, {" + CartToOrderCronJobModel._TYPECODE + ":" + CartToOrderCronJobModel.CREATIONTIME
            + "} DESC, {" + CartToOrderCronJobModel._TYPECODE + ":" + CartToOrderCronJobModel.PK + "}";


	private static final String SORT_ORDERS_BY_DATE = " ORDER BY {" + OrderModel._TYPECODE + ":" + OrderModel.CREATIONTIME
			+ "} DESC, {" + OrderModel._TYPECODE + ":" + OrderModel.PK + "}";

	private static final String SORT_ORDERS_BY_CODE = " ORDER BY {" + OrderModel._TYPECODE + ":" + OrderModel.CODE + "},{"
			+ OrderModel._TYPECODE + ":" + OrderModel.CREATIONTIME + "} DESC, {" + OrderModel._TYPECODE + ":" + OrderModel.PK + "}";

	public DefaultWorldlineCartToOrderCronJobModelDao()
	{
		super(CartToOrderCronJobModel._TYPECODE);
	}

	@Override
	public CartToOrderCronJobModel findCartToOrderCronJobByCode(final String code, final UserModel user)
	{
		final Map<String, Object> attr = new HashMap<>(2);
		attr.put(CartToOrderCronJobModel.CODE, code);
		attr.put(CartModel.USER, user);
		attr.put(CartToOrderCronJobModel.SUBMITTED, false);
		final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_CARTTOORDERCRONJOB_BY_CODE_AND_USER_QUERY);
		query.getQueryParameters().putAll(attr);
		final SearchResult<CartToOrderCronJobModel> jobs = this.getFlexibleSearchService().search(query);
		final List<CartToOrderCronJobModel> result = jobs.getResult();
		return (result.iterator().hasNext() ? result.iterator().next() : null);
	}

	@Override
	public List<CartToOrderCronJobModel> findCartToOrderCronJobsByUser(final UserModel user)
	{
		final Map<String, Object> attr = new HashMap<>(1);
		attr.put(OrderModel.USER, user);
		attr.put(CartToOrderCronJobModel.SUBMITTED, true);
		final FlexibleSearchQuery query = new FlexibleSearchQuery(FIND_CARTTOORDERCRONJOB_BY_USER_QUERY + SORT_JOBS_BY_DATE);
		query.getQueryParameters().putAll(attr);
		final SearchResult<CartToOrderCronJobModel> result = this.getFlexibleSearchService().search(query);
		return result.getResult();
	}


	@Override
	public SearchPageData<CartToOrderCronJobModel> findPagedCartToOrderCronJobsByUser(final UserModel user,
			final PageableData pageableData)
	{
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

	@Override
	public SearchPageData<OrderModel> findOrderByJob(final String jobCode, final PageableData pageableData)
	{
		final Map<String, Object> queryParams = new HashMap<String, Object>(1);
		queryParams.put(CartToOrderCronJobModel.CODE, jobCode);
		queryParams.put(TriggerModel.ACTIVE, true);

		final List<SortQueryData> sortQueries = Arrays.asList(
				createSortQueryData("byDate", FIND_ORDERS_FOR_SCHEDULE_BY_CODE_QUERY + SORT_ORDERS_BY_DATE),
				createSortQueryData("byOrderNumber", FIND_ORDERS_FOR_SCHEDULE_BY_CODE_QUERY + SORT_ORDERS_BY_CODE));

		return getPagedFlexibleSearchService().search(sortQueries, "byDate", queryParams, pageableData);
	}

	protected SortQueryData createSortQueryData(final String sortCode, final String query)
	{
		final SortQueryData result = new SortQueryData();
		result.setSortCode(sortCode);
		result.setQuery(query);
		return result;
	}

	private PagedFlexibleSearchService pagedFlexibleSearchService;

	protected PagedFlexibleSearchService getPagedFlexibleSearchService()
	{
		return pagedFlexibleSearchService;
	}

	@Required
	public void setPagedFlexibleSearchService(final PagedFlexibleSearchService pagedFlexibleSearchService)
	{
		this.pagedFlexibleSearchService = pagedFlexibleSearchService;
	}
}
