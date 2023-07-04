package com.worldline.direct.util;

import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import com.worldline.direct.order.data.WorldlinePaymentInfoData;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;

public class WorldlinePaymentProductUtils {


    public static boolean isPaymentByKlarna(WorldlinePaymentInfoModel worldlinePaymentInfoModel) {
        return WorldlinedirectcoreConstants.PAYMENT_METHOD_KLARNA_PAY_NOW== worldlinePaymentInfoModel.getId()|| WorldlinedirectcoreConstants.PAYMENT_METHOD_KLARNA_PAY_AFTER == worldlinePaymentInfoModel.getId();
    }

    public static boolean isPaymentBySepaDirectDebit(WorldlinePaymentInfoModel worldlinePaymentInfoModel) {
        return WorldlinedirectcoreConstants.PAYMENT_METHOD_SEPA == worldlinePaymentInfoModel.getId();
    }
    public static boolean isPaymentBySepaDirectDebit(WorldlinePaymentInfoData worldlinePaymentInfoData) {

        return WorldlinedirectcoreConstants.PAYMENT_METHOD_SEPA == worldlinePaymentInfoData.getId();
    }

    public static boolean isPaymentSupportingRecurring(WorldlinePaymentInfoModel worldlinePaymentInfoModel) {
        return (WorldlinedirectcoreConstants.PAYMENT_METHOD_TYPE.CARD.getValue().equals(worldlinePaymentInfoModel.getPaymentMethod()) &&
                WorldlinedirectcoreConstants.PAYMENT_METHOD_INTERSOLVE != worldlinePaymentInfoModel.getId()) ||
                WorldlinedirectcoreConstants.PAYMENT_METHOD_SEPA == worldlinePaymentInfoModel.getId();
    }

    public static boolean isPaymentSupportingRecurring(WorldlinePaymentInfoData worldlinePaymentInfoData) {
        return (worldlinePaymentInfoData.getPaymentMethod().equals(WorldlinedirectcoreConstants.PAYMENT_METHOD_TYPE.CARD.getValue()) &&
                WorldlinedirectcoreConstants.PAYMENT_METHOD_INTERSOLVE != worldlinePaymentInfoData.getId()) ||
                WorldlinedirectcoreConstants.PAYMENT_METHOD_SEPA == worldlinePaymentInfoData.getId();
    }

    public static boolean isCreditCard(WorldlinePaymentInfoData worldlinePaymentInfoData) {
        return WorldlinedirectcoreConstants.PAYMENT_METHOD_TYPE.CARD.getValue().equals(worldlinePaymentInfoData.getPaymentMethod()) &&
                WorldlinedirectcoreConstants.PAYMENT_METHOD_INTERSOLVE != worldlinePaymentInfoData.getId();
    }

}
