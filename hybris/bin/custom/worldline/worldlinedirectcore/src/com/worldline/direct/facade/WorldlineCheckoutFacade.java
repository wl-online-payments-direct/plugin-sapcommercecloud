package com.worldline.direct.facade;

import java.util.List;

import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.order.InvalidCartException;

import com.ingenico.direct.domain.CreateHostedCheckoutResponse;
import com.ingenico.direct.domain.CreateHostedTokenizationResponse;
import com.ingenico.direct.domain.DirectoryEntry;
import com.ingenico.direct.domain.PaymentProduct;
import com.worldline.direct.enums.WorldlineCheckoutTypesEnum;
import com.worldline.direct.exception.WorldlineNonAuthorizedPaymentException;
import com.worldline.direct.exception.WorldlineNonValidPaymentProductException;
import com.worldline.direct.exception.WorldlineNonValidReturnMACException;
import com.worldline.direct.order.data.BrowserData;
import com.worldline.direct.order.data.WorldlineHostedTokenizationData;
import com.worldline.direct.order.data.WorldlinePaymentInfoData;

public interface WorldlineCheckoutFacade {

    List<PaymentProduct> getAvailablePaymentMethods();

    WorldlineCheckoutTypesEnum getWorldlineCheckoutType();

    PaymentProduct getPaymentMethodById(int paymentId);

    CreateHostedTokenizationResponse createHostedTokenization();

    List<DirectoryEntry> getIdealIssuers(List<PaymentProduct> paymentProducts);

    void handlePaymentInfo(WorldlinePaymentInfoData paymentInfoData);

    void fillWorldlinePaymentInfoData(WorldlinePaymentInfoData paymentInfoData, int paymentId, String paymentDirId, String hostedTokenizationId) throws WorldlineNonValidPaymentProductException;

    OrderData authorisePaymentForHostedTokenization(String orderCode, WorldlineHostedTokenizationData hostedTokenizationId) throws WorldlineNonAuthorizedPaymentException, InvalidCartException;

    OrderData handle3dsResponse(String ref, String paymentId) throws WorldlineNonAuthorizedPaymentException, InvalidCartException;

    CreateHostedCheckoutResponse createHostedCheckout(String orderCode, BrowserData browserData) throws InvalidCartException;

    OrderData authorisePaymentForHostedCheckout(String orderCode, String hostedCheckoutId) throws WorldlineNonAuthorizedPaymentException, InvalidCartException;

    void validateReturnMAC(OrderData orderDetails, String returnMAC) throws WorldlineNonValidReturnMACException;

}
