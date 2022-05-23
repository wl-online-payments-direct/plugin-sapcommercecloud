package com.worldline.direct.facade;

import com.ingenico.direct.domain.PaymentProduct;
import com.ingenico.direct.domain.TokenResponse;
import com.worldline.direct.order.data.WorldlinePaymentInfoData;

import java.util.List;

public interface WorldlineUserFacade {

    List<WorldlinePaymentInfoData> getWorldlinePaymentInfos(boolean saved);

    List<WorldlinePaymentInfoData> getWorldlinePaymentInfosForPaymentProducts(List<PaymentProduct> paymentProducts, boolean saved);

    List<String> getSavedTokens();

    List<String> getSavedTokensForPaymentMethod(Integer paymentMethodId);

    WorldlinePaymentInfoData getWorldlinePaymentInfoByToken(String token);

    WorldlinePaymentInfoData getWorldlinePaymentInfoByCode(String code);

    void saveWorldlinePaymentInfo(TokenResponse tokenResponse, PaymentProduct paymentProduct);

    void deleteSavedWorldlinePaymentInfo(String code);

    void setDefaultPaymentInfo(WorldlinePaymentInfoData paymentInfoData);
}
