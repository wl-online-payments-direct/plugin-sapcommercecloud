package com.ingenico.ogone.direct.dao.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.hybris.platform.core.enums.PaymentStatus;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.order.daos.impl.DefaultOrderDao;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;

import com.ingenico.ogone.direct.dao.IngenicoOrderDao;

public class IngenicoOrderDaoImpl extends DefaultOrderDao implements IngenicoOrderDao {

    private static final String Q_TO_CAPTURE = "SELECT {" + OrderModel.PK + "} FROM {" + OrderModel._TYPECODE
            + "} WHERE {" + OrderModel.PAYMENTSTATUS + "} IN (?paymentStatuses) " +
            "AND {" + OrderModel.VERSIONID + "} IS NULL";

    private static final List<PaymentStatus> PAYMENT_STATUSES = Collections.unmodifiableList(
            Arrays.asList(PaymentStatus.INGENICO_WAITING_CAPTURE, PaymentStatus.INGENICO_AUTHORIZED));

    private FlexibleSearchService flexibleSearchService;

    @Override
    public List<OrderModel> findIngenicoOrdersToCapture() {
        FlexibleSearchQuery flexibleSearchQuery = new FlexibleSearchQuery(Q_TO_CAPTURE);
        flexibleSearchQuery.addQueryParameter("paymentStatuses", PAYMENT_STATUSES);

        final SearchResult<OrderModel> search = flexibleSearchService.search(flexibleSearchQuery);
        return search.getResult();
    }

    @Override
    public void setFlexibleSearchService(FlexibleSearchService flexibleSearchService) {
        this.flexibleSearchService = flexibleSearchService;
    }
}
