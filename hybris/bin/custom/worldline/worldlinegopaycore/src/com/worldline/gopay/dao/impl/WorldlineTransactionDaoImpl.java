package com.worldline.gopay.dao.impl;

import com.worldline.gopay.constants.WorldlinegopaycoreConstants;
import com.worldline.gopay.dao.WorldlineTransactionDao;
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
        flexibleSearchQuery.addQueryParameter(PaymentTransactionModel.PAYMENTPROVIDER, WorldlinegopaycoreConstants.PAYMENT_PROVIDER);

        return flexibleSearchService.searchUnique(flexibleSearchQuery);
    }

    public void setFlexibleSearchService(FlexibleSearchService flexibleSearchService) {
        this.flexibleSearchService = flexibleSearchService;
    }
}
