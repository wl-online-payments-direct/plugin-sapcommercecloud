package com.worldline.direct.util;

import com.worldline.direct.constants.WorldlinedirectcoreConstants;

public class WorldlinePaymentProductUtils {


    public boolean isPaymentByKlarna(Integer paymentID) {
        return WorldlinedirectcoreConstants.PAYMENT_METHOD_KLARNA_PAY_NOW == paymentID || WorldlinedirectcoreConstants.PAYMENT_METHOD_KLARNA_PAY_AFTER == paymentID;
    }

}
