package com.worldline.direct.facade;

import java.util.List;

import com.ingenico.direct.domain.PaymentProduct;
import com.ingenico.direct.domain.TokenResponse;
import com.worldline.direct.order.data.WorldlinePaymentInfoData;

public interface WorldlineUserFacade {

    List<WorldlinePaymentInfoData> getWorldlinePaymentInfos(boolean saved);

    List<String> getSavedTokens();

    List<String> getSavedTokensForPaymentMethod(Integer paymentMethodId);

    WorldlinePaymentInfoData getWorldlinePaymentInfoByToken(String token);

    void saveWorldlinePaymentInfo(TokenResponse tokenResponse, PaymentProduct paymentProduct);

    void deleteSavedWorldlinePaymentInfo(String code);

}
