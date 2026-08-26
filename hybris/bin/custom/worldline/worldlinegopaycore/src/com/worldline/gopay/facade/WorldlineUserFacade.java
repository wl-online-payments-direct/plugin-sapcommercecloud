package com.worldline.gopay.facade;

import com.onlinepayments.domain.PaymentProduct;
import com.onlinepayments.domain.TokenResponse;
import com.worldline.gopay.enums.WorldlineCheckoutTypesEnum;
import com.worldline.gopay.order.data.WorldlinePaymentInfoData;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;

import java.util.List;

public interface WorldlineUserFacade {

    List<WorldlinePaymentInfoData> getWorldlinePaymentInfos(boolean saved);

    List<WorldlinePaymentInfoData> getWorldlinePaymentInfosForPaymentProducts(List<PaymentProduct> paymentProducts, boolean saved);

    List<String> getSavedTokens();

    List<String> getSavedTokensForPaymentMethod(Integer paymentMethodId);

    WorldlinePaymentInfoData getWorldlinePaymentInfoByToken(String token);

    WorldlinePaymentInfoData getWorldlinePaymentInfoByCode(String code);

    void saveWorldlinePaymentInfo(WorldlineCheckoutTypesEnum checkoutType, TokenResponse tokenResponse, PaymentProduct paymentProduct);

    void updateWorldlinePaymentInfo(WorldlinePaymentInfoModel paymentInfoModel, TokenResponse tokenResponse, String cronjobId, String storeId, String initialPaymentId);

    void deleteSavedWorldlinePaymentInfo(String code);

    void setDefaultPaymentInfo(WorldlinePaymentInfoData paymentInfoData);
}
