package com.ingenico.ogone.direct.service.impl;

import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.INGENICO_EVENT_CAPTURE;
import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.INGENICO_EVENT_REFUND;
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
import de.hybris.platform.servicelayer.model.ModelService;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingenico.direct.domain.AmountOfMoney;
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
    public PaymentTransactionModel createAuthorizationPaymentTransaction(AbstractOrderModel abstractOrderModel,
                                                                         String merchantReference,
                                                                         String paymentTransactionId,
                                                                         String status,
                                                                         String statusDetails,
                                                                         AmountOfMoney amountOfMoney) {
        final PaymentTransactionModel paymentTransactionModel = createPaymentTransaction(
                abstractOrderModel,
                merchantReference,
                paymentTransactionId);

        createPaymentTransactionEntry(
                paymentTransactionModel,
                abstractOrderModel,
                status,
                statusDetails,
                amountOfMoney,
                PaymentTransactionType.AUTHORIZATION
        );

        modelService.refresh(paymentTransactionModel);
        return paymentTransactionModel;
    }


    @Override
    public PaymentTransactionModel createAuthorizedPaymentTransaction(AbstractOrderModel abstractOrderModel,
                                                                      String merchantReference,
                                                                      String paymentTransactionId,
                                                                      AmountOfMoney amountOfMoney) {
        return createAuthorizationPaymentTransaction(abstractOrderModel,
                merchantReference,
                paymentTransactionId,
                PAYMENT_STATUS_ENUM.PENDING_CAPTURE.getValue(),
                PAYMENT_STATUS_ENUM.PENDING_CAPTURE.getValue(),
                amountOfMoney);
    }

    @Override
    public PaymentTransactionModel updatePaymentTransaction(PaymentTransactionModel paymentTransaction, String status, String statusDetails, AmountOfMoney amountOfMoney, PaymentTransactionType paymentTransactionType) {
        validateParameterNotNullStandardMessage("paymentTransaction", paymentTransaction);
        validateParameterNotNullStandardMessage("status", status);
        validateParameterNotNullStandardMessage("statusDetails", statusDetails);
        validateParameterNotNullStandardMessage("amountOfMoney", amountOfMoney);
        validateParameterNotNullStandardMessage("paymentTransactionType", paymentTransactionType);
        createPaymentTransactionEntry(
                paymentTransaction,
                paymentTransaction.getOrder(),
                status,
                statusDetails,
                amountOfMoney,
                paymentTransactionType
        );
        modelService.refresh(paymentTransaction);
        return paymentTransaction;
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
                paymentTransaction.getOrder(),
                webhooksEvent.getPayment().getStatus(),
                webhooksEvent.getPayment().getStatusOutput().getStatusCategory(),
                webhooksEvent.getPayment().getPaymentOutput().getAmountOfMoney(),
                PaymentTransactionType.CAPTURE
        );
        //Trigger Captured event
        ingenicoBusinessProcessService.triggerOrderProcessEvent(order, INGENICO_EVENT_CAPTURE);
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
                webhooksEvent.getPayment().getStatus(),
                webhooksEvent.getPayment().getStatusOutput().getStatusCategory(),
                webhooksEvent.getPayment().getPaymentOutput().getAmountOfMoney(),
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
                paymentTransaction.getOrder(),
                webhooksEvent.getRefund().getStatus(),
                webhooksEvent.getRefund().getStatusOutput().getStatusCategory(),
                webhooksEvent.getRefund().getRefundOutput().getAmountOfMoney(),
                PaymentTransactionType.REFUND_FOLLOW_ON
        );
        //Trigger Refunded event
        ingenicoBusinessProcessService.triggerReturnProcessEvent(order, INGENICO_EVENT_REFUND);
    }

    private PaymentTransactionModel createPaymentTransaction(
            final AbstractOrderModel abstractOrderModel,
            final String merchantCode,
            final String pspReference) {
        validateParameterNotNullStandardMessage("order", abstractOrderModel);
        validateParameterNotNullStandardMessage("merchantCode", merchantCode);
        validateParameterNotNullStandardMessage("pspReference", pspReference);
        LOGGER.debug("[INGENICO] Create Payment Transaction for Order : {} ", abstractOrderModel.getCode());
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
            final String status,
            final String statusDetails,
            final AmountOfMoney amountOfMoney,
            final PaymentTransactionType transactionType) {
        validateParameterNotNullStandardMessage("order", abstractOrderModel);
        validateParameterNotNullStandardMessage("paymentTransaction", paymentTransaction);
        validateParameterNotNullStandardMessage("status", status);
        validateParameterNotNullStandardMessage("statusDetails", statusDetails);
        validateParameterNotNullStandardMessage("amountOfMoney", amountOfMoney);
        validateParameterNotNullStandardMessage("transactionType", transactionType);

        final PaymentTransactionEntryModel transactionEntryModel = modelService.create(PaymentTransactionEntryModel.class);
        LOGGER.debug("[INGENICO] Create Payment Transaction Entry for order {} (Transaction : {}, TransactionType : {}) ",
                abstractOrderModel.getCode(),
                paymentTransaction.getCode(),
                transactionType);
        String code = paymentTransaction.getRequestId() + "_" + paymentTransaction.getEntries().size();
        transactionEntryModel.setCode(code);
        transactionEntryModel.setType(transactionType);
        transactionEntryModel.setPaymentTransaction(paymentTransaction);
        transactionEntryModel.setRequestId(paymentTransaction.getRequestId());
        transactionEntryModel.setRequestToken(paymentTransaction.getRequestToken());
        transactionEntryModel.setTime(DateTime.now().toDate());

        final BigDecimal amount = ingenicoAmountUtils.fromAmount(amountOfMoney.getAmount(), amountOfMoney.getCurrencyCode());
        transactionEntryModel.setAmount(amount);
        transactionEntryModel.setCurrency(abstractOrderModel.getCurrency());

        transactionEntryModel.setTransactionStatus(getTransactionStatus(status));
        transactionEntryModel.setTransactionStatusDetails(statusDetails);

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
