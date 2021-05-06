package com.ingenico.ogone.direct.service.impl;

import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.payment.enums.PaymentTransactionType;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.payment.model.PaymentTransactionModel;
import de.hybris.platform.servicelayer.model.ModelService;

import org.joda.time.DateTime;

import com.ingenico.direct.domain.PaymentResponse;
import com.ingenico.ogone.direct.service.IngenicoTransactionService;


public class IngenicoTransactionServiceImpl implements IngenicoTransactionService {

    private static final String INGENICO = "INGENICO";
    private ModelService modelService;

    @Override
    public PaymentTransactionModel createPaymentTransaction(AbstractOrderModel abstractOrderModel, PaymentResponse paymentResponse, PaymentTransactionType transactionType) {
        final PaymentTransactionModel paymentTransactionModel = createPaymentTransaction(
                abstractOrderModel,
                paymentResponse.getPaymentOutput().getReferences().getMerchantReference(),
                paymentResponse.getId());

        PaymentTransactionEntryModel paymentTransactionEntryModel = createPaymentTransactionEntry(
                paymentTransactionModel,
                abstractOrderModel,
                paymentResponse,
                transactionType
        );

        modelService.save(paymentTransactionEntryModel);

        List<PaymentTransactionEntryModel> entries = new ArrayList<>();
        entries.add(paymentTransactionEntryModel);
        paymentTransactionModel.setEntries(entries);
        modelService.refresh(paymentTransactionModel); //refresh is needed by order-process

        return paymentTransactionModel;
    }

    @Override
    public PaymentTransactionModel updatePaymentTransaction(PaymentTransactionModel paymentTransactionModel, PaymentResponse paymentResponse) {
        return null;
    }

    private PaymentTransactionModel createPaymentTransaction(
            final AbstractOrderModel abstractOrderModel,
            final String merchantCode,
            final String pspReference) {

        final PaymentTransactionModel paymentTransactionModel = modelService.create(PaymentTransactionModel.class);
        paymentTransactionModel.setCode(pspReference);
        paymentTransactionModel.setRequestId(pspReference);
        paymentTransactionModel.setRequestToken(merchantCode);
        paymentTransactionModel.setPaymentProvider(INGENICO);
        paymentTransactionModel.setOrder(abstractOrderModel);
        paymentTransactionModel.setCurrency(abstractOrderModel.getCurrency());
        paymentTransactionModel.setInfo(abstractOrderModel.getPaymentInfo());
        paymentTransactionModel.setPlannedAmount(BigDecimal.valueOf(abstractOrderModel.getTotalPrice()));

        modelService.save(paymentTransactionModel);

        return paymentTransactionModel;
    }

    private PaymentTransactionEntryModel createPaymentTransactionEntry(
            final PaymentTransactionModel paymentTransaction,
            final AbstractOrderModel abstractOrderModel,
            final PaymentResponse paymentResponse,
            final PaymentTransactionType transactionType) {
        final PaymentTransactionEntryModel transactionEntryModel = modelService.create(PaymentTransactionEntryModel.class);
        String code = paymentTransaction.getRequestId() + "_" + paymentTransaction.getEntries().size();
        transactionEntryModel.setCode(code);
        transactionEntryModel.setType(transactionType);
        transactionEntryModel.setPaymentTransaction(paymentTransaction);
        transactionEntryModel.setRequestId(paymentTransaction.getRequestId());
        transactionEntryModel.setRequestToken(paymentTransaction.getRequestToken());

        transactionEntryModel.setTime(DateTime.now().toDate());
        transactionEntryModel.setAmount(BigDecimal.valueOf(abstractOrderModel.getTotalPrice()));
        transactionEntryModel.setCurrency(abstractOrderModel.getCurrency());

        transactionEntryModel.setTransactionStatus(getTransactionStatus(paymentResponse));
        transactionEntryModel.setTransactionStatusDetails(paymentResponse.getStatusOutput().getStatusCategory());

        modelService.save(transactionEntryModel);

        return transactionEntryModel;
    }

    private String getTransactionStatus(PaymentResponse paymentResponse) {
        switch (PAYMENT_STATUS_ENUM.valueOf(paymentResponse.getStatus())) {
            case CREATED:
            case REJECTED:
            case REJECTED_CAPTURE:
            case CANCELLED:
                return PAYMENT_STATUS_CATEGORY_ENUM.REJECTED.getValue();
            case REDIRECTED:
                return PAYMENT_STATUS_CATEGORY_ENUM.STATUS_UNKNOWN.getValue();
            case PENDING_PAYMENT:
            case PENDING_COMPLETION:
            case PENDING_CAPTURE:
            case AUTHORIZATION_REQUESTED:
            case CAPTURE_REQUESTED:
            case CAPTURED:
                return PAYMENT_STATUS_CATEGORY_ENUM.SUCCESSFUL.getValue();
            default:
                return "NOT_SUPPORTED";
        }
    }


    public void setModelService(ModelService modelService) {
        this.modelService = modelService;
    }
}
