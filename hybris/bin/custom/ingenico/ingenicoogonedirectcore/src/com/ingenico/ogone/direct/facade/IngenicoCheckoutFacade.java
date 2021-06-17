package com.ingenico.ogone.direct.facade;

import java.util.List;

import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.order.InvalidCartException;

import com.ingenico.direct.domain.CreateHostedCheckoutResponse;
import com.ingenico.direct.domain.CreateHostedTokenizationResponse;
import com.ingenico.direct.domain.DirectoryEntry;
import com.ingenico.direct.domain.PaymentProduct;
import com.ingenico.ogone.direct.enums.IngenicoCheckoutTypesEnum;
import com.ingenico.ogone.direct.exception.IngenicoNonAuthorizedPaymentException;
import com.ingenico.ogone.direct.exception.IngenicoNonValidPaymentProductException;
import com.ingenico.ogone.direct.exception.IngenicoNonValidReturnMACException;
import com.ingenico.ogone.direct.order.data.BrowserData;
import com.ingenico.ogone.direct.order.data.IngenicoHostedTokenizationData;
import com.ingenico.ogone.direct.order.data.IngenicoPaymentInfoData;

public interface IngenicoCheckoutFacade {

    List<PaymentProduct> getAvailablePaymentMethods();

    IngenicoCheckoutTypesEnum getIngenicoCheckoutType();

    PaymentProduct getPaymentMethodById(int paymentId);

    CreateHostedTokenizationResponse createHostedTokenization();

    List<DirectoryEntry> getIdealIssuers(List<PaymentProduct> paymentProducts);

    void handlePaymentInfo(IngenicoPaymentInfoData paymentInfoData);

    void fillIngenicoPaymentInfoData(IngenicoPaymentInfoData paymentInfoData, int paymentId, String paymentDirId, String hostedTokenizationId) throws IngenicoNonValidPaymentProductException;

    OrderData authorisePaymentForHostedTokenization(IngenicoHostedTokenizationData hostedTokenizationId) throws IngenicoNonAuthorizedPaymentException, InvalidCartException;

    OrderData handle3dsResponse(String ref, String returnMAC, String paymentId) throws IngenicoNonAuthorizedPaymentException, InvalidCartException;

    CreateHostedCheckoutResponse createHostedCheckout(BrowserData browserData) throws InvalidCartException;

    OrderData authorisePaymentForHostedCheckout(String hostedCheckoutId) throws IngenicoNonAuthorizedPaymentException, InvalidCartException;

    void validateReturnMAC(String returnMAC) throws IngenicoNonValidReturnMACException;

}
