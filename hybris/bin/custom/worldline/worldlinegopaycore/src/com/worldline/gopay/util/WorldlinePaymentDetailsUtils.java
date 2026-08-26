package com.worldline.gopay.util;

import com.onlinepayments.domain.AmountOfMoney;
import com.onlinepayments.domain.CardEssentials;
import com.onlinepayments.domain.CardFraudResults;
import com.onlinepayments.domain.CardPaymentMethodSpecificOutput;
import com.onlinepayments.domain.FraudResults;
import com.onlinepayments.domain.MobilePaymentMethodSpecificOutput;
import com.onlinepayments.domain.PaymentOutput;
import com.onlinepayments.domain.PaymentReferences;
import com.onlinepayments.domain.PaymentResponse;
import com.onlinepayments.domain.PaymentStatusOutput;
import com.onlinepayments.domain.RedirectPaymentMethodSpecificOutput;
import com.onlinepayments.domain.ThreeDSecureResults;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import org.apache.commons.lang.StringUtils;

public final class WorldlinePaymentDetailsUtils {

    private static final int CARD_LAST_FOUR_LENGTH = 4;

    private WorldlinePaymentDetailsUtils() {
    }

    public static void updatePaymentDetails(final WorldlinePaymentInfoModel paymentInfo, final PaymentResponse paymentResponse) {
        if (paymentInfo == null || paymentResponse == null || paymentResponse.getPaymentOutput() == null) {
            return;
        }

        final PaymentOutput paymentOutput = paymentResponse.getPaymentOutput();

        setIfNotBlank(paymentResponse.getId(), paymentInfo::setWorldlinePaymentId);
        setIfNotBlank(paymentResponse.getStatus(), paymentInfo::setWorldlineStatus);
        updateStatusOutput(paymentInfo, paymentResponse.getStatusOutput());
        updatePaymentOutput(paymentInfo, paymentOutput);
        updatePaymentProductId(paymentInfo, paymentOutput);
        updateCardDetails(paymentInfo, paymentOutput.getCardPaymentMethodSpecificOutput());
        updateFraudResult(paymentInfo, paymentOutput);
        updateThreeDSecureResults(paymentInfo, paymentOutput);
    }

    private static void updateStatusOutput(final WorldlinePaymentInfoModel paymentInfo, final PaymentStatusOutput statusOutput) {
        if (statusOutput != null && statusOutput.getStatusCode() != null) {
            paymentInfo.setWorldlineStatusCode(statusOutput.getStatusCode());
        }
    }

    private static void updatePaymentOutput(final WorldlinePaymentInfoModel paymentInfo, final PaymentOutput paymentOutput) {
        setIfNotBlank(paymentOutput.getPaymentMethod(), paymentInfo::setWorldlinePaymentMethod);
        updateMerchantReference(paymentInfo, paymentOutput.getReferences());
        updateAmount(paymentOutput.getAmountOfMoney(), paymentInfo::setWorldlineAmount, paymentInfo::setWorldlineCurrency);
        updateAmount(paymentOutput.getAcquiredAmount(), paymentInfo::setWorldlineAcquiredAmount, paymentInfo::setWorldlineAcquiredCurrency);
    }

    private static void updateMerchantReference(final WorldlinePaymentInfoModel paymentInfo, final PaymentReferences references) {
        if (references != null) {
            setIfNotBlank(references.getMerchantReference(), paymentInfo::setWorldlineMerchantReference);
        }
    }

    private static void updatePaymentProductId(final WorldlinePaymentInfoModel paymentInfo, final PaymentOutput paymentOutput) {
        final Integer paymentProductId = getPaymentProductId(paymentOutput);
        if (paymentProductId != null) {
            paymentInfo.setWorldlinePaymentProductId(paymentProductId);
        }
    }

    private static Integer getPaymentProductId(final PaymentOutput paymentOutput) {
        final MobilePaymentMethodSpecificOutput mobileOutput = paymentOutput.getMobilePaymentMethodSpecificOutput();
        if (mobileOutput != null && mobileOutput.getPaymentProductId() != null) {
            return mobileOutput.getPaymentProductId();
        }

        final CardPaymentMethodSpecificOutput cardOutput = paymentOutput.getCardPaymentMethodSpecificOutput();
        if (cardOutput != null && cardOutput.getPaymentProductId() != null) {
            return cardOutput.getPaymentProductId();
        }

        final RedirectPaymentMethodSpecificOutput redirectOutput = paymentOutput.getRedirectPaymentMethodSpecificOutput();
        if (redirectOutput != null) {
            return redirectOutput.getPaymentProductId();
        }

        return null;
    }

    private static void updateCardDetails(final WorldlinePaymentInfoModel paymentInfo, final CardPaymentMethodSpecificOutput cardOutput) {
        if (cardOutput == null || cardOutput.getCard() == null) {
            return;
        }

        final CardEssentials card = cardOutput.getCard();
        setIfNotBlank(card.getBin(), paymentInfo::setCardBin);
        setIfNotBlank(getLastFour(card.getCardNumber()), paymentInfo::setCardLastFour);
    }

    private static void updateFraudResult(final WorldlinePaymentInfoModel paymentInfo, final PaymentOutput paymentOutput) {
        final CardPaymentMethodSpecificOutput cardOutput = paymentOutput.getCardPaymentMethodSpecificOutput();
        if (cardOutput != null && cardOutput.getFraudResults() != null) {
            setIfNotBlank(cardOutput.getFraudResults().getFraudServiceResult(), paymentInfo::setFraudResult);
            return;
        }

        final MobilePaymentMethodSpecificOutput mobileOutput = paymentOutput.getMobilePaymentMethodSpecificOutput();
        if (mobileOutput != null && mobileOutput.getFraudResults() != null) {
            final CardFraudResults fraudResults = mobileOutput.getFraudResults();
            setIfNotBlank(fraudResults.getFraudServiceResult(), paymentInfo::setFraudResult);
            return;
        }

        final RedirectPaymentMethodSpecificOutput redirectOutput = paymentOutput.getRedirectPaymentMethodSpecificOutput();
        if (redirectOutput != null && redirectOutput.getFraudResults() != null) {
            final FraudResults fraudResults = redirectOutput.getFraudResults();
            setIfNotBlank(fraudResults.getFraudServiceResult(), paymentInfo::setFraudResult);
        }
    }

    private static void updateThreeDSecureResults(final WorldlinePaymentInfoModel paymentInfo, final PaymentOutput paymentOutput) {
        final ThreeDSecureResults threeDSecureResults = getThreeDSecureResults(paymentOutput);
        if (threeDSecureResults == null) {
            return;
        }

        setIfNotBlank(threeDSecureResults.getLiability(), paymentInfo::setLiability);
        setIfNotBlank(threeDSecureResults.getAppliedExemption(), paymentInfo::setAppliedExemption);
        setIfNotBlank(threeDSecureResults.getAuthenticationStatus(), paymentInfo::setAuthenticationStatus);
    }

    private static ThreeDSecureResults getThreeDSecureResults(final PaymentOutput paymentOutput) {
        final CardPaymentMethodSpecificOutput cardOutput = paymentOutput.getCardPaymentMethodSpecificOutput();
        if (cardOutput != null && cardOutput.getThreeDSecureResults() != null) {
            return cardOutput.getThreeDSecureResults();
        }

        final MobilePaymentMethodSpecificOutput mobileOutput = paymentOutput.getMobilePaymentMethodSpecificOutput();
        if (mobileOutput != null) {
            return mobileOutput.getThreeDSecureResults();
        }

        return null;
    }

    private static void updateAmount(final AmountOfMoney amountOfMoney, final LongSetter amountSetter, final StringSetter currencySetter) {
        if (amountOfMoney == null) {
            return;
        }
        if (amountOfMoney.getAmount() != null) {
            amountSetter.set(amountOfMoney.getAmount());
        }
        setIfNotBlank(amountOfMoney.getCurrencyCode(), currencySetter);
    }

    private static String getLastFour(final String cardNumber) {
        if (StringUtils.isBlank(cardNumber)) {
            return null;
        }
        if (cardNumber.length() <= CARD_LAST_FOUR_LENGTH) {
            return cardNumber;
        }
        return cardNumber.substring(cardNumber.length() - CARD_LAST_FOUR_LENGTH);
    }

    private static void setIfNotBlank(final String value, final StringSetter setter) {
        if (StringUtils.isNotBlank(value)) {
            setter.set(value);
        }
    }

    private interface StringSetter {
        void set(String value);
    }

    private interface LongSetter {
        void set(Long value);
    }
}
