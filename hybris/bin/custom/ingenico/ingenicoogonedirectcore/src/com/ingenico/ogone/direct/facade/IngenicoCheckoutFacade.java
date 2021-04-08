package com.ingenico.ogone.direct.facade;

import java.util.List;

import com.ingenico.direct.domain.CreateHostedCheckoutResponse;
import com.ingenico.direct.domain.CreateHostedTokenizationResponse;
import com.ingenico.direct.domain.DirectoryEntry;
import com.ingenico.direct.domain.PaymentProduct;
import com.ingenico.ogone.direct.order.data.IngenicoPaymentInfoData;

public interface IngenicoCheckoutFacade {

    List<PaymentProduct> getAvailablePaymentMethods();

    PaymentProduct getPaymentMethodById(int paymentId);

    CreateHostedTokenizationResponse createHostedTokenization();

    List<DirectoryEntry> getIdealIssuers(List<PaymentProduct> paymentProducts);

    void handlePaymentInfo(IngenicoPaymentInfoData paymentInfoData);

    void fillIngenicoPaymentInfoData(IngenicoPaymentInfoData paymentInfoData, int paymentId);

    CreateHostedCheckoutResponse createHostedCheckout();
}
