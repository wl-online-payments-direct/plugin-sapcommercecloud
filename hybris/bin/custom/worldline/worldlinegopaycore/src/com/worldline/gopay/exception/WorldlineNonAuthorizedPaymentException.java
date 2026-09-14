package com.worldline.gopay.exception;

import com.onlinepayments.domain.MerchantAction;
import com.onlinepayments.domain.PaymentResponse;
import com.worldline.gopay.constants.WorldlinegopaycoreConstants;

public class WorldlineNonAuthorizedPaymentException extends Exception {
    private PaymentResponse paymentResponse;
    private MerchantAction merchantAction;
    private WorldlinegopaycoreConstants.UNAUTHORIZED_REASON reason;

    public WorldlineNonAuthorizedPaymentException(PaymentResponse paymentResponse, WorldlinegopaycoreConstants.UNAUTHORIZED_REASON reason) {
        this.paymentResponse = paymentResponse;
        this.reason = reason;
    }

    public WorldlineNonAuthorizedPaymentException(PaymentResponse paymentResponse, MerchantAction merchantAction, WorldlinegopaycoreConstants.UNAUTHORIZED_REASON reason) {
        this.paymentResponse = paymentResponse;
        this.merchantAction = merchantAction;
        this.reason = reason;
    }

    public WorldlineNonAuthorizedPaymentException(WorldlinegopaycoreConstants.UNAUTHORIZED_REASON reason) {
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

    public WorldlinegopaycoreConstants.UNAUTHORIZED_REASON getReason() {
        return reason;
    }

    public void setReason(WorldlinegopaycoreConstants.UNAUTHORIZED_REASON reason) {
        this.reason = reason;
    }
}
