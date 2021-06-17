package com.ingenico.ogone.direct.facade;

import java.util.List;

import com.ingenico.direct.domain.PaymentProduct;
import com.ingenico.direct.domain.TokenResponse;
import com.ingenico.ogone.direct.order.data.IngenicoPaymentInfoData;

public interface IngenicoUserFacade {

    List<IngenicoPaymentInfoData> getIngenicoPaymentInfos(boolean saved);

    List<String> getSavedTokens();

    List<String> getSavedTokensForPaymentMethod(Integer paymentMethodId);

    IngenicoPaymentInfoData getIngenicoPaymentInfoByToken(String token);

    void saveIngenicoPaymentInfo(TokenResponse tokenResponse, PaymentProduct paymentProduct);

    void deleteSavedIngenicoPaymentInfo(String code);

}
