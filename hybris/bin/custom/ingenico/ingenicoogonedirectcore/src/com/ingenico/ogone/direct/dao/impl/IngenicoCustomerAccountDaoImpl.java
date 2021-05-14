package com.ingenico.ogone.direct.dao.impl;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hybris.platform.core.model.order.payment.IngenicoPaymentInfoModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.servicelayer.internal.dao.AbstractItemDao;
import de.hybris.platform.servicelayer.search.SearchResult;

import com.ingenico.ogone.direct.dao.IngenicoCustomerAccountDao;

public class IngenicoCustomerAccountDaoImpl extends AbstractItemDao implements IngenicoCustomerAccountDao {

    private static final String FIND_INGENICO_PAYMENT_INFOS_BY_CUSTOMER_QUERY = "SELECT {" + IngenicoPaymentInfoModel.PK + "} FROM {"
            + IngenicoPaymentInfoModel._TYPECODE + "} WHERE {" + IngenicoPaymentInfoModel.USER + "} = ?customer AND {"
            + IngenicoPaymentInfoModel.DUPLICATE + "} = ?duplicate";

    private static final String FIND_SAVED_INGENICO_PAYMENT_INFOS_BY_CUSTOMER_QUERY = FIND_INGENICO_PAYMENT_INFOS_BY_CUSTOMER_QUERY
            + " AND {" + IngenicoPaymentInfoModel.SAVED + "} = ?saved";

    @Override
    public List<IngenicoPaymentInfoModel> findIgenicoPaymentInfosByCustomer(CustomerModel customerModel, boolean saved) {
        validateParameterNotNull(customerModel, "Customer must not be null");
        final Map<String, Object> queryParams = new HashMap<String, Object>();
        queryParams.put("customer", customerModel);
        if (saved) {
            queryParams.put("saved", Boolean.TRUE);
        }
        queryParams.put("duplicate", Boolean.FALSE);
        final SearchResult<IngenicoPaymentInfoModel> result = getFlexibleSearchService().search(
                saved ? FIND_SAVED_INGENICO_PAYMENT_INFOS_BY_CUSTOMER_QUERY : FIND_INGENICO_PAYMENT_INFOS_BY_CUSTOMER_QUERY, queryParams);
        return result.getResult();
    }
}
