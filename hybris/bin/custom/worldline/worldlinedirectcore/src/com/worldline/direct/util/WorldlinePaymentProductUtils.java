package com.worldline.direct.util;

import com.onlinepayments.domain.PaymentProduct;
import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import com.worldline.direct.order.data.WorldlinePaymentInfoData;
import com.worldline.direct.service.WorldlinePaymentModeService;
import de.hybris.platform.core.Registry;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


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
        WorldlinePaymentModeService worldlinePaymentModeService = Registry.getApplicationContext().getBean(WorldlinePaymentModeService.class);
        return (WorldlinedirectcoreConstants.PAYMENT_METHOD_TYPE.CARD.getValue().equals(worldlinePaymentInfoModel.getPaymentMethod()) &&
                !worldlinePaymentModeService.isIntersolve(String.valueOf(worldlinePaymentInfoModel.getId()))) ||
                WorldlinedirectcoreConstants.PAYMENT_METHOD_SEPA == worldlinePaymentInfoModel.getId() ||
                WorldlinedirectcoreConstants.PAYMENT_METHOD_GOOGLEPAY == worldlinePaymentInfoModel.getId();
    }

    public static boolean isMobileWallet(PaymentProduct paymentProduct) {
        return paymentProduct != null
                && WorldlinedirectcoreConstants.PAYMENT_METHOD_TYPE.MOBILE.getValue().equalsIgnoreCase(paymentProduct.getPaymentMethod());
    }

    /**
     * Returns the mobile wallet payment products (Google Pay, Apple Pay, ...), preserving the order of the given list.
     */
    public static List<PaymentProduct> getMobileWallets(List<PaymentProduct> paymentProducts) {
        if (paymentProducts == null) {
            return Collections.emptyList();
        }
        return paymentProducts.stream()
                .filter(WorldlinePaymentProductUtils::isMobileWallet)
                .collect(Collectors.toList());
    }

    /**
     * Returns everything but the mobile wallet payment products, preserving the order of the given list.
     */
    public static List<PaymentProduct> getNonMobileWallets(List<PaymentProduct> paymentProducts) {
        if (paymentProducts == null) {
            return Collections.emptyList();
        }
        return paymentProducts.stream()
                .filter(paymentProduct -> !isMobileWallet(paymentProduct))
                .collect(Collectors.toList());
    }

    public static boolean isCreditCard(WorldlinePaymentInfoData worldlinePaymentInfoData) {
        WorldlinePaymentModeService worldlinePaymentModeService = Registry.getApplicationContext().getBean(WorldlinePaymentModeService.class);
        return WorldlinedirectcoreConstants.PAYMENT_METHOD_TYPE.CARD.getValue().equals(worldlinePaymentInfoData.getPaymentMethod()) &&
                !worldlinePaymentModeService.isIntersolve(String.valueOf(worldlinePaymentInfoData.getId()));
    }
}
