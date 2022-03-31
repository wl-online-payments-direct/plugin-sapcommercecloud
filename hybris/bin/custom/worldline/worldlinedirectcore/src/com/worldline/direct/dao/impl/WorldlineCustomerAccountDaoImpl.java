package com.worldline.direct.dao.impl;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.worldline.direct.dao.WorldlineCustomerAccountDao;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.servicelayer.internal.dao.AbstractItemDao;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.SearchResult;

public class WorldlineCustomerAccountDaoImpl extends AbstractItemDao implements WorldlineCustomerAccountDao {

    private static final String FIND_WORLDLINE_PAYMENT_INFOS_BY_CUSTOMER_QUERY = "SELECT {" + WorldlinePaymentInfoModel.PK + "} FROM {"
            + WorldlinePaymentInfoModel._TYPECODE + "} WHERE {" + WorldlinePaymentInfoModel.USER + "} = ?customer AND {"
            + WorldlinePaymentInfoModel.DUPLICATE + "} = ?duplicate";

    private static final String FIND_WORLDLINE_PAYMENT_INFOS_BY_CUSTOMER_AND_TOKEN_QUERY = "SELECT {" + WorldlinePaymentInfoModel.PK + "} FROM {"
            + WorldlinePaymentInfoModel._TYPECODE + "} WHERE {" + WorldlinePaymentInfoModel.USER + "} = ?customer AND {"
            + WorldlinePaymentInfoModel.TOKEN + "}=?token AND {" + WorldlinePaymentInfoModel.DUPLICATE + "} = ?duplicate";

    private static final String FIND_SAVED_WORLDLINE_PAYMENT_INFOS_BY_CUSTOMER_QUERY = FIND_WORLDLINE_PAYMENT_INFOS_BY_CUSTOMER_QUERY
            + " AND {" + WorldlinePaymentInfoModel.SAVED + "} = ?saved";

    private static final String FIND_SAVED_WORLDLINE_PAYMENT_INFOS_BY_CUSTOMER_AND_TOKEN_QUERY = FIND_WORLDLINE_PAYMENT_INFOS_BY_CUSTOMER_AND_TOKEN_QUERY
            + " AND {" + WorldlinePaymentInfoModel.SAVED + "} = ?saved";

    @Override
    public List<WorldlinePaymentInfoModel> findWorldlinePaymentInfosByCustomer(CustomerModel customerModel, boolean saved) {
        validateParameterNotNull(customerModel, "Customer must not be null");
        final Map<String, Object> queryParams = new HashMap<String, Object>();
        queryParams.put("customer", customerModel);
        if (saved) {
            queryParams.put("saved", Boolean.TRUE);
        }
        queryParams.put("duplicate", Boolean.FALSE);
        final SearchResult<WorldlinePaymentInfoModel> result = getFlexibleSearchService().search(
                saved ? FIND_SAVED_WORLDLINE_PAYMENT_INFOS_BY_CUSTOMER_QUERY : FIND_WORLDLINE_PAYMENT_INFOS_BY_CUSTOMER_QUERY, queryParams);
        return result.getResult();
    }

    @Override
    public WorldlinePaymentInfoModel findWorldlinePaymentInfosByCustomerAndToken(CustomerModel customerModel, String token, boolean saved) {
        final Map<String, Object> queryParams = new HashMap<String, Object>();
        queryParams.put("customer", customerModel);
        queryParams.put("token", token);
        if (saved) {
            queryParams.put("saved", Boolean.TRUE);
        }
        queryParams.put("duplicate", Boolean.FALSE);
        FlexibleSearchQuery flexibleSearchQuery = new FlexibleSearchQuery(saved ?
                FIND_SAVED_WORLDLINE_PAYMENT_INFOS_BY_CUSTOMER_AND_TOKEN_QUERY : FIND_WORLDLINE_PAYMENT_INFOS_BY_CUSTOMER_AND_TOKEN_QUERY,
                queryParams);

        return getFlexibleSearchService().searchUnique(flexibleSearchQuery);

    }
}
