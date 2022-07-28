package com.worldline.direct.facade;

import com.onlinepayments.domain.*;
import com.worldline.direct.enums.WorldlineCheckoutTypesEnum;
import com.worldline.direct.exception.WorldlineNonAuthorizedPaymentException;
import com.worldline.direct.exception.WorldlineNonValidPaymentProductException;
import com.worldline.direct.exception.WorldlineNonValidReturnMACException;
import com.worldline.direct.order.data.BrowserData;
import com.worldline.direct.order.data.WorldlineHostedTokenizationData;
import com.worldline.direct.order.data.WorldlinePaymentInfoData;
import de.hybris.platform.commercefacades.order.data.AbstractOrderData;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.order.InvalidCartException;

import java.util.List;

public interface WorldlineCheckoutFacade {

    List<PaymentProduct> getAvailablePaymentMethods();

    WorldlineCheckoutTypesEnum getWorldlineCheckoutType();

    PaymentProduct getPaymentMethodById(int paymentId);

    CreateHostedTokenizationResponse createHostedTokenization();

    List<DirectoryEntry> getIdealIssuers(List<PaymentProduct> paymentProducts);

    void handlePaymentInfo(WorldlinePaymentInfoData paymentInfoData);

    void fillWorldlinePaymentInfoData(WorldlinePaymentInfoData paymentInfoData, String savedPaymentCode, Integer paymentId, String paymentDirId, String hostedTokenizationId) throws WorldlineNonValidPaymentProductException;

    void authorisePaymentForHostedTokenization(String orderCode, WorldlineHostedTokenizationData hostedTokenizationId) throws WorldlineNonAuthorizedPaymentException, InvalidCartException;

    void handle3dsResponse(String ref, String paymentId) throws WorldlineNonAuthorizedPaymentException, InvalidCartException;

    CreateHostedCheckoutResponse createHostedCheckout(String orderCode, BrowserData browserData) throws InvalidCartException;

    void authorisePaymentForHostedCheckout(String orderCode, String hostedCheckoutId) throws WorldlineNonAuthorizedPaymentException, InvalidCartException;

    void handlePaymentResponse(AbstractOrderModel abstractOrderModel, PaymentResponse paymentResponse) throws WorldlineNonAuthorizedPaymentException, InvalidCartException;

    void validateReturnMAC(AbstractOrderData orderDetails, String returnMAC) throws WorldlineNonValidReturnMACException;

}
