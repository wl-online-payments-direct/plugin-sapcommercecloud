package com.ingenico.ogone.direct.service.impl;

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

    private ModelService modelService;

    @Override
    public PaymentTransactionModel createPaymentTransaction(AbstractOrderModel abstractOrderModel, PaymentResponse paymentResponse) {
        final PaymentTransactionModel paymentTransactionModel = createPaymentTransaction(
                abstractOrderModel,
                paymentResponse.getPaymentOutput().getReferences().getMerchantReference(),
                paymentResponse.getId());

        PaymentTransactionEntryModel paymentTransactionEntryModel = createPaymentTransactionEntry(
                paymentTransactionModel,
                abstractOrderModel,
                paymentResponse
        );

        modelService.save(paymentTransactionEntryModel);

        List<PaymentTransactionEntryModel> entries = new ArrayList<>();
        entries.add(paymentTransactionEntryModel);
        paymentTransactionModel.setEntries(entries);
        modelService.refresh(paymentTransactionModel); //refresh is needed by order-process

        return paymentTransactionModel;
    }


    private PaymentTransactionModel createPaymentTransaction(
            final AbstractOrderModel abstractOrderModel,
            final String merchantCode,
            final String pspReference) {
        final PaymentTransactionModel paymentTransactionModel = modelService.create(PaymentTransactionModel.class);
        paymentTransactionModel.setCode(pspReference);
        paymentTransactionModel.setRequestId(pspReference);
        paymentTransactionModel.setRequestToken(merchantCode);
        paymentTransactionModel.setPaymentProvider("INGENICO");
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
            final PaymentResponse paymentResponse) {
        final PaymentTransactionEntryModel transactionEntryModel = modelService.create(PaymentTransactionEntryModel.class);

        String code = paymentTransaction.getRequestId() + "_" + paymentTransaction.getEntries().size();

        transactionEntryModel.setType(PaymentTransactionType.AUTHORIZATION);
        transactionEntryModel.setPaymentTransaction(paymentTransaction);
        transactionEntryModel.setRequestId(paymentTransaction.getRequestId());
        transactionEntryModel.setRequestToken("merchantCode");
        transactionEntryModel.setCode(code);
        transactionEntryModel.setTime(DateTime.now().toDate());
        transactionEntryModel.setTransactionStatus("status");
        transactionEntryModel.setTransactionStatusDetails("status details");
        transactionEntryModel.setAmount(BigDecimal.valueOf(abstractOrderModel.getTotalPrice()));
        transactionEntryModel.setCurrency(abstractOrderModel.getCurrency());

        modelService.save(transactionEntryModel);

        return transactionEntryModel;
    }

    public void setModelService(ModelService modelService) {
        this.modelService = modelService;
    }
}
