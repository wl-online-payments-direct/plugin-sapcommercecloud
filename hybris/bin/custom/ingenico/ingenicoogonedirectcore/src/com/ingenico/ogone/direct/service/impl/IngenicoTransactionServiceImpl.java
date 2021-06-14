package com.ingenico.ogone.direct.service.impl;

import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.INGENICO_EVENT_CAPTURE;
import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.INGENICO_EVENT_REFUND;
import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_PROVIDER;
import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_STATUS_CATEGORY_ENUM;
import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_STATUS_ENUM;
import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNullStandardMessage;

import java.math.BigDecimal;

import de.hybris.platform.core.enums.PaymentStatus;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.payment.enums.PaymentTransactionType;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.payment.model.PaymentTransactionModel;
import de.hybris.platform.servicelayer.exceptions.ModelNotFoundException;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.store.BaseStoreModel;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingenico.direct.domain.AmountOfMoney;
import com.ingenico.direct.domain.Capture;
import com.ingenico.direct.domain.WebhooksEvent;
import com.ingenico.ogone.direct.dao.IngenicoTransactionDao;
import com.ingenico.ogone.direct.model.IngenicoConfigurationModel;
import com.ingenico.ogone.direct.service.IngenicoBusinessProcessService;
import com.ingenico.ogone.direct.service.IngenicoPaymentService;
import com.ingenico.ogone.direct.service.IngenicoTransactionService;
import com.ingenico.ogone.direct.util.IngenicoAmountUtils;


public class IngenicoTransactionServiceImpl implements IngenicoTransactionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(IngenicoTransactionServiceImpl.class);

    private IngenicoTransactionDao ingenicoTransactionDao;
    private IngenicoBusinessProcessService ingenicoBusinessProcessService;
    private IngenicoPaymentService ingenicoPaymentService;
    private IngenicoAmountUtils ingenicoAmountUtils;
    private ModelService modelService;


    @Override
    public PaymentTransactionModel getOrCreatePaymentTransaction(AbstractOrderModel abstractOrderModel, String merchantReference, String paymentTransactionId) {
        validateParameterNotNullStandardMessage("abstractOrderModel", abstractOrderModel);
        validateParameterNotNullStandardMessage("merchantReference", merchantReference);
        validateParameterNotNullStandardMessage("paymentTransactionId", paymentTransactionId);
        PaymentTransactionModel paymentTransaction;
        try {
            paymentTransaction = ingenicoTransactionDao.findPaymentTransaction(getPaymentId(paymentTransactionId));
        } catch (ModelNotFoundException exception) {
            paymentTransaction = createPaymentTransaction(
                    abstractOrderModel,
                    merchantReference,
                    paymentTransactionId);
        }

        return paymentTransaction;
    }


    @Override
    public PaymentTransactionModel createAuthorizationPaymentTransaction(AbstractOrderModel abstractOrderModel,
                                                                         String merchantReference,
                                                                         String paymentTransactionId,
                                                                         String status,
                                                                         String statusDetails,
                                                                         AmountOfMoney amountOfMoney) {
        final PaymentTransactionModel paymentTransaction = getOrCreatePaymentTransaction(abstractOrderModel,
                merchantReference,
                paymentTransactionId);

        return updatePaymentTransaction(
                paymentTransaction,
                paymentTransactionId,
                status,
                statusDetails,
                amountOfMoney,
                PaymentTransactionType.AUTHORIZATION);
    }


    @Override
    public PaymentTransactionModel updatePaymentTransaction(PaymentTransactionModel paymentTransaction, String paymentTransactionId, String status, String statusDetails, AmountOfMoney amountOfMoney, PaymentTransactionType paymentTransactionType) {
        validateParameterNotNullStandardMessage("paymentTransaction", paymentTransaction);
        validateParameterNotNullStandardMessage("paymentTransactionId", paymentTransactionId);
        validateParameterNotNullStandardMessage("status", status);
        validateParameterNotNullStandardMessage("statusDetails", statusDetails);
        validateParameterNotNullStandardMessage("amountOfMoney", amountOfMoney);
        validateParameterNotNullStandardMessage("paymentTransactionType", paymentTransactionType);
        createPaymentTransactionEntry(
                paymentTransaction,
                paymentTransactionId,
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
        final String paymentTransactionId = webhooksEvent.getPayment().getId();
        final PaymentTransactionModel paymentTransaction = ingenicoTransactionDao.findPaymentTransaction(getPaymentId(paymentTransactionId));

        final boolean alreadyProcessed = paymentTransaction.getEntries().stream()
                .filter(entry -> PaymentTransactionType.CAPTURE.equals(entry.getType()))
                .anyMatch(entry -> entry.getRequestId().equals(paymentTransactionId));

        final OrderModel order = (OrderModel) paymentTransaction.getOrder();
        if (!alreadyProcessed) {
            updatePaymentTransaction(
                    paymentTransaction,
                    webhooksEvent.getPayment().getId(),
                    webhooksEvent.getPayment().getStatus(),
                    webhooksEvent.getPayment().getStatusOutput().getStatusCategory(),
                    webhooksEvent.getPayment().getPaymentOutput().getAmountOfMoney(),
                    PaymentTransactionType.CAPTURE
            );
        }
        //Trigger Captured event
        ingenicoBusinessProcessService.triggerOrderProcessEvent(order, INGENICO_EVENT_CAPTURE);
    }

    @Override
    public void processCapture(Capture capture) {
        validateParameterNotNullStandardMessage("capture", capture);
        final String paymentTransactionId = capture.getId();
        final PaymentTransactionModel paymentTransaction = ingenicoTransactionDao.findPaymentTransaction(getPaymentId(paymentTransactionId));

        final boolean notProcessed = paymentTransaction.getEntries().stream()
                .filter(entry -> PaymentTransactionType.CAPTURE.equals(entry.getType()))
                .noneMatch(entry -> entry.getRequestId().equals(paymentTransactionId));

        if (notProcessed) {
            createPaymentTransactionEntry(
                    paymentTransaction,
                    paymentTransactionId,
                    paymentTransaction.getOrder(),
                    capture.getStatus(),
                    "N/A",
                    capture.getCaptureOutput().getAmountOfMoney(),
                    PaymentTransactionType.CAPTURE
            );
            //Trigger Capture event
            ingenicoBusinessProcessService.triggerReturnProcessEvent((OrderModel) paymentTransaction.getOrder(), INGENICO_EVENT_CAPTURE);
        }

    }


    @Override
    public void processRefundedEvent(WebhooksEvent webhooksEvent) {
        validateParameterNotNullStandardMessage("webhooksEvent", webhooksEvent);
        validateParameterNotNullStandardMessage("webhooksEvent.refund", webhooksEvent.getRefund());
        LOGGER.debug("[INGENICO] PROCESS {} EVENT id :{}", webhooksEvent.getType(), webhooksEvent.getId());
        final PaymentTransactionModel paymentTransaction = ingenicoTransactionDao.findPaymentTransaction(getPaymentId(webhooksEvent.getPayment().getId()));
        final OrderModel order = (OrderModel) paymentTransaction.getOrder();
        updatePaymentTransaction(
                paymentTransaction,
                webhooksEvent.getRefund().getId(),
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
        final String paymentId = getPaymentId(pspReference);
        paymentTransactionModel.setCode(paymentId);
        paymentTransactionModel.setRequestId(paymentId);
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
            final String paymentTransactionId,
            final AbstractOrderModel abstractOrderModel,
            final String status,
            final String statusDetails,
            final AmountOfMoney amountOfMoney,
            final PaymentTransactionType transactionType) {
        validateParameterNotNullStandardMessage("order", abstractOrderModel);
        validateParameterNotNullStandardMessage("paymentTransaction", paymentTransaction);
        validateParameterNotNullStandardMessage("paymentTransactionId", paymentTransactionId);
        validateParameterNotNullStandardMessage("status", status);
        validateParameterNotNullStandardMessage("statusDetails", statusDetails);
        validateParameterNotNullStandardMessage("amountOfMoney", amountOfMoney);
        validateParameterNotNullStandardMessage("transactionType", transactionType);

        final PaymentTransactionEntryModel transactionEntryModel = modelService.create(PaymentTransactionEntryModel.class);
        LOGGER.debug("[INGENICO] Create Payment Transaction Entry for order {} (Transaction : {}, TransactionType : {}) ",
                abstractOrderModel.getCode(),
                paymentTransaction.getCode(),
                transactionType);
        String code = paymentTransactionId + "_" + paymentTransaction.getEntries().size();
        transactionEntryModel.setCode(code);
        transactionEntryModel.setType(transactionType);
        transactionEntryModel.setPaymentTransaction(paymentTransaction);
        transactionEntryModel.setRequestId(paymentTransactionId);
        transactionEntryModel.setRequestToken(paymentTransaction.getRequestToken());
        transactionEntryModel.setTime(DateTime.now().toDate());

        final BigDecimal amount = ingenicoAmountUtils.fromAmount(amountOfMoney.getAmount(), amountOfMoney.getCurrencyCode());
        transactionEntryModel.setAmount(amount);
        transactionEntryModel.setCurrency(abstractOrderModel.getCurrency());

        transactionEntryModel.setTransactionStatus(getTransactionStatus(status));
        transactionEntryModel.setTransactionStatusDetails(statusDetails);

        modelService.save(transactionEntryModel);

        setOrderPaymentStatusIfNeeded(abstractOrderModel, transactionEntryModel);

        return transactionEntryModel;
    }

    private void setOrderPaymentStatusIfNeeded(AbstractOrderModel abstractOrderModel, PaymentTransactionEntryModel transactionEntryModel) {
        if (transactionEntryModel.getType() == PaymentTransactionType.CAPTURE) {
            if (PAYMENT_STATUS_CATEGORY_ENUM.SUCCESSFUL.getValue().equals(transactionEntryModel.getTransactionStatus())) {
                final Long nonCapturedAmount = ingenicoPaymentService.getNonCapturedAmount(getIngenicoConfiguration(abstractOrderModel),
                        transactionEntryModel.getRequestId(),
                        transactionEntryModel.getPaymentTransaction().getPlannedAmount(),
                        transactionEntryModel.getCurrency().getIsocode());
                if (nonCapturedAmount > 0L) {
                    abstractOrderModel.setPaymentStatus(PaymentStatus.INGENICO_WAITING_CAPTURE);
                } else {
                    abstractOrderModel.setPaymentStatus(PaymentStatus.INGENICO_CAPTURED);
                }
            } else {
                abstractOrderModel.setPaymentStatus(PaymentStatus.INGENICO_REJECTED);
            }
            modelService.save(abstractOrderModel);
        }
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

    private IngenicoConfigurationModel getIngenicoConfiguration(AbstractOrderModel orderModel) {
        final BaseStoreModel store = orderModel.getStore();
        return store != null ? store.getIngenicoConfiguration() : null;
    }

    private String getPaymentId(String paymentTransactionId) {
        return StringUtils.split(paymentTransactionId, "_")[0];
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

    public void setIngenicoPaymentService(IngenicoPaymentService ingenicoPaymentService) {
        this.ingenicoPaymentService = ingenicoPaymentService;
    }
}
