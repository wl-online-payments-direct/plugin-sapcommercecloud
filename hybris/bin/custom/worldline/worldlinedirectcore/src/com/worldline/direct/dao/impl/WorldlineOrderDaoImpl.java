package com.worldline.direct.dao.impl;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.worldline.direct.dao.WorldlineOrderDao;
import de.hybris.platform.core.enums.PaymentStatus;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.order.daos.impl.DefaultOrderDao;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;

public class WorldlineOrderDaoImpl extends DefaultOrderDao implements WorldlineOrderDao {

    private static final String Q_TO_CAPTURE = "SELECT {" + OrderModel.PK + "} FROM {" + OrderModel._TYPECODE
            + "} WHERE {" + OrderModel.PAYMENTSTATUS + "} IN (?paymentStatuses) " +
            "AND {" + OrderModel.VERSIONID + "} IS NULL";

    private static final String Q_ORDER_BY_CODE = "SELECT {" + OrderModel.PK + "} FROM {" + OrderModel._TYPECODE
            + "} WHERE {" + OrderModel.CODE + "} = ?code " +
            "AND {" + OrderModel.VERSIONID + "} IS NULL";

    private static final List<PaymentStatus> PAYMENT_STATUSES = Collections.unmodifiableList(
            Arrays.asList(PaymentStatus.WORLDLINE_WAITING_CAPTURE, PaymentStatus.WORLDLINE_AUTHORIZED));

    private FlexibleSearchService flexibleSearchService;

    @Override
    public List<OrderModel> findWorldlineOrdersToCapture() {
        FlexibleSearchQuery flexibleSearchQuery = new FlexibleSearchQuery(Q_TO_CAPTURE);
        flexibleSearchQuery.addQueryParameter("paymentStatuses", PAYMENT_STATUSES);

        final SearchResult<OrderModel> search = flexibleSearchService.search(flexibleSearchQuery);
        return search.getResult();
    }

    @Override
    public OrderModel findWorldlineOrder(String orderCode) {
        validateParameterNotNull(orderCode, "orderCode cannot be null");
        FlexibleSearchQuery flexibleSearchQuery = new FlexibleSearchQuery(Q_ORDER_BY_CODE);
        flexibleSearchQuery.addQueryParameter("code", orderCode);

        return flexibleSearchService.searchUnique(flexibleSearchQuery);
    }

    @Override
    public void setFlexibleSearchService(FlexibleSearchService flexibleSearchService) {
        this.flexibleSearchService = flexibleSearchService;
    }
}
