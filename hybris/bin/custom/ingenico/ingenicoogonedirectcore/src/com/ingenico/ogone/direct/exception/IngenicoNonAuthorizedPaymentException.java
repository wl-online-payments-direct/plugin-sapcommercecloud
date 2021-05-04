package com.ingenico.ogone.direct.exception;

import com.ingenico.direct.domain.MerchantAction;
import com.ingenico.direct.domain.PaymentResponse;
import com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.UNAUTHORIZED_REASON;

public class IngenicoNonAuthorizedPaymentException extends Exception {
    private PaymentResponse paymentResponse;
    private MerchantAction merchantAction;
    private UNAUTHORIZED_REASON reason;

    public IngenicoNonAuthorizedPaymentException(PaymentResponse paymentResponse, UNAUTHORIZED_REASON reason) {
        this.paymentResponse = paymentResponse;
        this.reason = reason;
    }

    public IngenicoNonAuthorizedPaymentException(PaymentResponse paymentResponse, MerchantAction merchantAction, UNAUTHORIZED_REASON reason) {
        this.paymentResponse = paymentResponse;
        this.merchantAction = merchantAction;
        this.reason = reason;
    }

    public IngenicoNonAuthorizedPaymentException(UNAUTHORIZED_REASON reason) {
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

    public UNAUTHORIZED_REASON getReason() {
        return reason;
    }

    public void setReason(UNAUTHORIZED_REASON reason) {
        this.reason = reason;
    }
}
