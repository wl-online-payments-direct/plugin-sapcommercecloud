package com.ingenico.ogone.direct.facade;

import java.util.List;

import com.ingenico.ogone.direct.order.data.IngenicoPaymentInfoData;

public interface IngenicoUserFacade {

    List<IngenicoPaymentInfoData> getIngenicoPaymentInfos(boolean saved);

    List<IngenicoPaymentInfoData> getIngenicoPaymentInfoDataForUniqueTokens();

    List<String> getSavedTokens();

    List<String> getSavedTokensForPaymentMethod(Integer paymentMethodId);

    List<IngenicoPaymentInfoData> getIngenicoPaymentInfoByToken(String token);

}
