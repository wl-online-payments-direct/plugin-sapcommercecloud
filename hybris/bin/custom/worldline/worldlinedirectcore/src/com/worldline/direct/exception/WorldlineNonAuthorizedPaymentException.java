package com.worldline.direct.exception;

import com.onlinepayments.domain.MerchantAction;
import com.onlinepayments.domain.PaymentResponse;
import com.worldline.direct.constants.WorldlinedirectcoreConstants;

public class WorldlineNonAuthorizedPaymentException extends Exception {
    private PaymentResponse paymentResponse;
    private MerchantAction merchantAction;
    private WorldlinedirectcoreConstants.UNAUTHORIZED_REASON reason;

    public WorldlineNonAuthorizedPaymentException(PaymentResponse paymentResponse, WorldlinedirectcoreConstants.UNAUTHORIZED_REASON reason) {
        this.paymentResponse = paymentResponse;
        this.reason = reason;
    }

    public WorldlineNonAuthorizedPaymentException(PaymentResponse paymentResponse, MerchantAction merchantAction, WorldlinedirectcoreConstants.UNAUTHORIZED_REASON reason) {
        this.paymentResponse = paymentResponse;
        this.merchantAction = merchantAction;
        this.reason = reason;
    }

    public WorldlineNonAuthorizedPaymentException(WorldlinedirectcoreConstants.UNAUTHORIZED_REASON reason) {
        this.reason = reason;
    }

    public PaymentResponse getPaymentResponse() {
        return paymentResponse;
    }

    public void setPaymentResponse(PaymentResponse paymentResponse) {
        this.paymentResponse = paymentResponse;
    }

    public MerchantAction getMerchantAction() {
        return merchantAction;
    }

    public void setMerchantAction(MerchantAction merchantAction) {
        this.merchantAction = merchantAction;
    }

    public WorldlinedirectcoreConstants.UNAUTHORIZED_REASON getReason() {
        return reason;
    }

    public void setReason(WorldlinedirectcoreConstants.UNAUTHORIZED_REASON reason) {
        this.reason = reason;
    }
}
