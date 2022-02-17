package com.worldline.direct.dao.impl;

import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import com.worldline.direct.dao.WorldlineTransactionDao;
import de.hybris.platform.payment.model.PaymentTransactionModel;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;

public class WorldlineTransactionDaoImpl implements WorldlineTransactionDao {

    private static final String Q_BY_KEY = "SELECT {" + PaymentTransactionModel.PK + "} FROM {" + PaymentTransactionModel._TYPECODE
            + "} WHERE {" + PaymentTransactionModel.CODE + "} = ?code AND {" + PaymentTransactionModel.PAYMENTPROVIDER + "} = ?paymentProvider " +
            "AND {" + PaymentTransactionModel.VERSIONID + "} IS NULL";

    private FlexibleSearchService flexibleSearchService;

    @Override
    public PaymentTransactionModel findPaymentTransaction(String reference) {
        FlexibleSearchQuery flexibleSearchQuery = new FlexibleSearchQuery(Q_BY_KEY);
        flexibleSearchQuery.addQueryParameter(PaymentTransactionModel.CODE, reference);
        flexibleSearchQuery.addQueryParameter(PaymentTransactionModel.PAYMENTPROVIDER, WorldlinedirectcoreConstants.PAYMENT_PROVIDER);

        return flexibleSearchService.searchUnique(flexibleSearchQuery);
    }

    public void setFlexibleSearchService(FlexibleSearchService flexibleSearchService) {
        this.flexibleSearchService = flexibleSearchService;
    }
}
