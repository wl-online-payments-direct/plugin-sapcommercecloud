package com.ingenico.ogone.direct.service.impl;

import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.INGENICO_EVENT_CAPTURED;
import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.INGENICO_EVENT_REFUNDED;
import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_PROVIDER;
import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_STATUS_CATEGORY_ENUM;
import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_STATUS_ENUM;
import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNullStandardMessage;

import java.math.BigDecimal;

import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.payment.enums.PaymentTransactionType;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.payment.model.PaymentTransactionModel;
import de.hybris.platform.servicelayer.exceptions.ModelNotFoundException;
import de.hybris.platform.servicelayer.model.ModelService;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingenico.direct.domain.AmountOfMoney;
import com.ingenico.direct.domain.PaymentResponse;
import com.ingenico.direct.domain.RefundResponse;
import com.ingenico.direct.domain.WebhooksEvent;
import com.ingenico.ogone.direct.dao.IngenicoTransactionDao;
import com.ingenico.ogone.direct.service.IngenicoBusinessProcessService;
import com.ingenico.ogone.direct.service.IngenicoTransactionService;
import com.ingenico.ogone.direct.util.IngenicoAmountUtils;


public class IngenicoTransactionServiceImpl implements IngenicoTransactionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(IngenicoTransactionServiceImpl.class);

    private IngenicoTransactionDao ingenicoTransactionDao;
    private IngenicoBusinessProcessService ingenicoBusinessProcessService;
    private IngenicoAmountUtils ingenicoAmountUtils;
    private ModelService modelService;


    @Override
    public PaymentTransactionModel createOrUpdatePaymentTransaction(AbstractOrderModel abstractOrderModel, PaymentResponse paymentResponse, PaymentTransactionType transactionType) {
        LOGGER.debug("[INGENICO] Create Payment Transaction for Order : {} ", abstractOrderModel.getCode());

        PaymentTransactionModel paymentTransactionModel;
        try {
            paymentTransactionModel = ingenicoTransactionDao.findPaymentTransaction(paymentResponse.getId());
        } catch (ModelNotFoundException exception) {
            final String merchantReference = paymentResponse.getPaymentOutput().getReferences().getMerchantReference();
            paymentTransactionModel = createPaymentTransaction(
                    abstractOrderModel,
                    merchantReference,
                    paymentResponse.getId());
        }

        createPaymentTransactionEntry(
                paymentTransactionModel,
                abstractOrderModel,
                paymentResponse,
                transactionType
        );

        modelService.refresh(paymentTransactionModel);
        return paymentTransactionModel;
    }

    @Override
    public PaymentTransactionModel updatePaymentTransaction(PaymentTransactionModel paymentTransactionModel, PaymentResponse paymentResponse) {
        return null;
    }


    @Override
    public void processCapturedEvent(WebhooksEvent webhooksEvent) {
        validateParameterNotNullStandardMessage("webhooksEvent", webhooksEvent);
        validateParameterNotNullStandardMessage("webhooksEvent.payment", webhooksEvent.getPayment());
        LOGGER.debug("[INGENICO] PROCESS {} EVENT id :{}", webhooksEvent.getType(), webhooksEvent.getId());
        final PaymentTransactionModel paymentTransaction = ingenicoTransactionDao.findPaymentTransaction(webhooksEvent.getPayment().getId());
        final OrderModel order = (OrderModel) paymentTransaction.getOrder();
        createPaymentTransactionEntry(
                paymentTransaction,
                order,
                webhooksEvent.getPayment(),
                PaymentTransactionType.CAPTURE
        );
        //Trigger Captured event
        ingenicoBusinessProcessService.triggerOrderProcessEvent(order, INGENICO_EVENT_CAPTURED);
    }

    @Override
    public void processCancelledEvent(WebhooksEvent webhooksEvent) {
        validateParameterNotNullStandardMessage("webhooksEvent", webhooksEvent);
        validateParameterNotNullStandardMessage("webhooksEvent.payment", webhooksEvent.getPayment());
        LOGGER.debug("[INGENICO] PROCESS {} EVENT id :{}", webhooksEvent.getType(), webhooksEvent.getId());
        final PaymentTransactionModel paymentTransaction = ingenicoTransactionDao.findPaymentTransaction(webhooksEvent.getPayment().getId());
        createPaymentTransactionEntry(
                paymentTransaction,
                paymentTransaction.getOrder(),
                webhooksEvent.getPayment(),
                PaymentTransactionType.CANCEL
        );
    }

    @Override
    public void processRefundedEvent(WebhooksEvent webhooksEvent) {
        validateParameterNotNullStandardMessage("webhooksEvent", webhooksEvent);
        validateParameterNotNullStandardMessage("webhooksEvent.refund", webhooksEvent.getRefund());
        LOGGER.debug("[INGENICO] PROCESS {} EVENT id :{}", webhooksEvent.getType(), webhooksEvent.getId());
        final PaymentTransactionModel paymentTransaction = ingenicoTransactionDao.findPaymentTransaction(webhooksEvent.getPayment().getId());
        final OrderModel order = (OrderModel) paymentTransaction.getOrder();
        createPaymentTransactionEntry(
                paymentTransaction,
                order,
                webhooksEvent.getRefund(),
                PaymentTransactionType.REFUND_FOLLOW_ON
        );
        //Trigger Refunded event
        ingenicoBusinessProcessService.triggerReturnProcessEvent(order, INGENICO_EVENT_REFUNDED);
    }

    private PaymentTransactionModel createPaymentTransaction(
            final AbstractOrderModel abstractOrderModel,
            final String merchantCode,
            final String pspReference) {

        final PaymentTransactionModel paymentTransactionModel = modelService.create(PaymentTransactionModel.class);
        paymentTransactionModel.setCode(pspReference);
        paymentTransactionModel.setRequestId(pspReference);
        paymentTransactionModel.setRequestToken(merchantCode);
        paymentTransactionModel.setPaymentProvider(PAYMENT_PROVIDER);
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

        final AmountOfMoney amountOfMoney = paymentResponse.getPaymentOutput().getAmountOfMoney();
        final BigDecimal amount = ingenicoAmountUtils.fromAmount(amountOfMoney.getAmount(), amountOfMoney.getCurrencyCode());
        transactionEntryModel.setAmount(amount);
        transactionEntryModel.setCurrency(abstractOrderModel.getCurrency());

        transactionEntryModel.setTransactionStatus(getTransactionStatus(paymentResponse.getStatus()));
        transactionEntryModel.setTransactionStatusDetails(paymentResponse.getStatusOutput().getStatusCategory());

        modelService.save(transactionEntryModel);

        return transactionEntryModel;
    }

    private PaymentTransactionEntryModel createPaymentTransactionEntry(
            final PaymentTransactionModel paymentTransaction,
            final AbstractOrderModel abstractOrderModel,
            final RefundResponse refundResponse,
            final PaymentTransactionType transactionType) {
        final PaymentTransactionEntryModel transactionEntryModel = modelService.create(PaymentTransactionEntryModel.class);
        String code = paymentTransaction.getRequestId() + "_" + paymentTransaction.getEntries().size();
        transactionEntryModel.setCode(code);
        transactionEntryModel.setType(transactionType);
        transactionEntryModel.setPaymentTransaction(paymentTransaction);
        transactionEntryModel.setRequestId(paymentTransaction.getRequestId());
        transactionEntryModel.setRequestToken(paymentTransaction.getRequestToken());
        transactionEntryModel.setTime(DateTime.now().toDate());

        final AmountOfMoney amountOfMoney = refundResponse.getRefundOutput().getAmountOfMoney();
        final BigDecimal amount = ingenicoAmountUtils.fromAmount(amountOfMoney.getAmount(), amountOfMoney.getCurrencyCode());
        transactionEntryModel.setAmount(amount);
        transactionEntryModel.setCurrency(abstractOrderModel.getCurrency());

        transactionEntryModel.setTransactionStatus(getTransactionStatus(refundResponse.getStatus()));
        transactionEntryModel.setTransactionStatusDetails(refundResponse.getStatusOutput().getStatusCategory());

        modelService.save(transactionEntryModel);

        return transactionEntryModel;
    }

    private String getTransactionStatus(String status) {
        switch (PAYMENT_STATUS_ENUM.valueOf(status)) {
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

    public void setIngenicoTransactionDao(IngenicoTransactionDao ingenicoTransactionDao) {
        this.ingenicoTransactionDao = ingenicoTransactionDao;
    }

    public void setIngenicoBusinessProcessService(IngenicoBusinessProcessService ingenicoBusinessProcessService) {
        this.ingenicoBusinessProcessService = ingenicoBusinessProcessService;
    }

    public void setIngenicoAmountUtils(IngenicoAmountUtils ingenicoAmountUtils) {
        this.ingenicoAmountUtils = ingenicoAmountUtils;
    }
}
