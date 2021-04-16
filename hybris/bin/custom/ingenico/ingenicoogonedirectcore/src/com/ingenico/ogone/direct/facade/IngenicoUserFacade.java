package com.ingenico.ogone.direct.facade;

import java.util.List;

import com.ingenico.ogone.direct.order.data.IngenicoPaymentInfoData;

public interface IngenicoUserFacade {

    List<IngenicoPaymentInfoData> getIngenicoPaymentInfos(boolean saved);

    List<IngenicoPaymentInfoData> getIngenicoPaymentInfoByToken(String token);

}
