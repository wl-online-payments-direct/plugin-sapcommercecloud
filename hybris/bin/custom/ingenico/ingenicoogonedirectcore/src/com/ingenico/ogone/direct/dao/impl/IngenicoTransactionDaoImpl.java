package com.ingenico.ogone.direct.dao.impl;

import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_PROVIDER;

import de.hybris.platform.payment.model.PaymentTransactionModel;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;

import com.ingenico.ogone.direct.dao.IngenicoTransactionDao;

public class IngenicoTransactionDaoImpl implements IngenicoTransactionDao {

    private static final String Q_BY_KEY = "SELECT {" + PaymentTransactionModel.PK + "} FROM {" + PaymentTransactionModel._TYPECODE
            + "} WHERE {" + PaymentTransactionModel.CODE + "} = ?reference AND {" + PaymentTransactionModel.PAYMENTPROVIDER + "} = ?paymentProvider " +
            "AND {" + PaymentTransactionModel.VERSIONID + "} IS NULL";

    private FlexibleSearchService flexibleSearchService;

    @Override
    public PaymentTransactionModel findPaymentTransaction(String reference) {
        FlexibleSearchQuery flexibleSearchQuery = new FlexibleSearchQuery(Q_BY_KEY);
        flexibleSearchQuery.addQueryParameter(PaymentTransactionModel.CODE, reference);
        flexibleSearchQuery.addQueryParameter(PaymentTransactionModel.PAYMENTPROVIDER, PAYMENT_PROVIDER);

        return flexibleSearchService.searchUnique(flexibleSearchQuery);
    }

    public void setFlexibleSearchService(FlexibleSearchService flexibleSearchService) {
        this.flexibleSearchService = flexibleSearchService;
    }
}
