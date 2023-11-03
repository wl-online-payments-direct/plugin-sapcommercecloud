package com.worldline.direct.service.impl;

import com.onlinepayments.domain.*;
import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import com.worldline.direct.dao.WorldlineTransactionDao;
import com.worldline.direct.service.WorldlineBusinessProcessService;
import com.worldline.direct.service.WorldlineTransactionService;
import com.worldline.direct.util.WorldlineAmountUtils;
import de.hybris.platform.core.enums.PaymentStatus;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.order.CalculationService;
import de.hybris.platform.order.exceptions.CalculationException;
import de.hybris.platform.payment.enums.PaymentTransactionType;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.payment.model.PaymentTransactionModel;
import de.hybris.platform.servicelayer.exceptions.ModelNotFoundException;
import de.hybris.platform.servicelayer.model.ModelService;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNullStandardMessage;


public class WorldlineTransactionServiceImpl implements WorldlineTransactionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldlineTransactionServiceImpl.class);

    private static final int PAYMENT_ID_LENGTH = 10;

    private static final int PAYMENT_ID_START_STRIP_LENGTH = 6;

    private WorldlineTransactionDao worldlineTransactionDao;
    private WorldlineBusinessProcessService worldlineBusinessProcessService;
    private WorldlineAmountUtils worldlineAmountUtils;
    private ModelService modelService;

    private CalculationService calculationService;


    @Override
    public PaymentTransactionModel getOrCreatePaymentTransaction(AbstractOrderModel abstractOrderModel, String merchantReference, String paymentTransactionId) {
        validateParameterNotNullStandardMessage("abstractOrderModel", abstractOrderModel);
        validateParameterNotNullStandardMessage("merchantReference", merchantReference);
        validateParameterNotNullStandardMessage("paymentTransactionId", paymentTransactionId);
        PaymentTransactionModel paymentTransaction;
        try {
            paymentTransaction = worldlineTransactionDao.findPaymentTransaction(getPaymentId(paymentTransactionId));
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
                                                                         AmountOfMoney amountOfMoney) {
        final PaymentTransactionModel paymentTransaction = getOrCreatePaymentTransaction(abstractOrderModel,
                merchantReference,
                paymentTransactionId);

        return updatePaymentTransaction(
                paymentTransaction,
                paymentTransactionId,
                status,
                amountOfMoney,
                PaymentTransactionType.AUTHORIZATION);
    }


    @Override
    public PaymentTransactionModel updatePaymentTransaction(PaymentTransactionModel paymentTransaction, String paymentTransactionId, String status, AmountOfMoney amountOfMoney, PaymentTransactionType paymentTransactionType) {
        validateParameterNotNullStandardMessage("paymentTransaction", paymentTransaction);
        validateParameterNotNullStandardMessage("paymentTransactionId", paymentTransactionId);
        validateParameterNotNullStandardMessage("status", status);
        validateParameterNotNullStandardMessage("amountOfMoney", amountOfMoney);
        validateParameterNotNullStandardMessage("paymentTransactionType", paymentTransactionType);
        createPaymentTransactionEntry(
                paymentTransaction,
                paymentTransactionId,
                paymentTransaction.getOrder(),
                status,
                amountOfMoney,
                paymentTransactionType
        );

        return paymentTransaction;
    }


    @Override
    public void processCapturedEvent(WebhooksEvent webhooksEvent) {
        validateParameterNotNullStandardMessage("webhooksEvent", webhooksEvent);
        validateParameterNotNullStandardMessage("webhooksEvent.payment", webhooksEvent.getPayment());
        LOGGER.debug("[WORLDLINE] PROCESS {} EVENT id :{}", webhooksEvent.getType(), webhooksEvent.getId());
        final String paymentTransactionId = webhooksEvent.getPayment().getId();
        final PaymentTransactionModel paymentTransaction = worldlineTransactionDao.findPaymentTransaction(getPaymentId(paymentTransactionId));

        final boolean alreadyProcessed = paymentTransaction.getEntries().stream()
                .filter(entry -> PaymentTransactionType.CAPTURE.equals(entry.getType()))
                .filter(entry -> webhooksEvent.getPayment().getStatus().equals(entry.getTransactionStatusDetails()))
                .anyMatch(entry -> entry.getRequestId().equals(paymentTransactionId));

        if (!alreadyProcessed) {
            updatePaymentTransaction(
                    paymentTransaction,
                    webhooksEvent.getPayment().getId(),
                    webhooksEvent.getPayment().getStatus(),
                    webhooksEvent.getPayment().getPaymentOutput().getAcquiredAmount() != null ? webhooksEvent.getPayment().getPaymentOutput().getAcquiredAmount() : webhooksEvent.getPayment().getPaymentOutput().getAmountOfMoney(),
                    PaymentTransactionType.CAPTURE
            );
        }
    }

    @Override
    public void processCapture(Capture capture) {
        validateParameterNotNullStandardMessage("capture", capture);
        final String paymentTransactionId = capture.getId();
        final PaymentTransactionModel paymentTransaction = worldlineTransactionDao.findPaymentTransaction(getPaymentId(paymentTransactionId));

        final boolean notProcessed = paymentTransaction.getEntries().stream()
                .filter(entry -> PaymentTransactionType.CAPTURE.equals(entry.getType()))
                .filter(entry -> capture.getStatus().equals(entry.getTransactionStatusDetails()))
                .noneMatch(entry -> entry.getRequestId().equals(paymentTransactionId));

        if (notProcessed) {
            createPaymentTransactionEntry(
                    paymentTransaction,
                    paymentTransactionId,
                    paymentTransaction.getOrder(),
                    capture.getStatus(),
                    capture.getCaptureOutput().getAcquiredAmount() != null ? capture.getCaptureOutput().getAcquiredAmount() : capture.getCaptureOutput().getAmountOfMoney(),
                    PaymentTransactionType.CAPTURE
            );
        }

    }


    @Override
    public void processRefundedEvent(WebhooksEvent webhooksEvent) {
        validateParameterNotNullStandardMessage("webhooksEvent", webhooksEvent);
        validateParameterNotNullStandardMessage("webhooksEvent.refund", webhooksEvent.getRefund());
        LOGGER.debug("[WORLDLINE] PROCESS {} EVENT id :{}", webhooksEvent.getType(), webhooksEvent.getId());
        final PaymentTransactionModel paymentTransaction = worldlineTransactionDao.findPaymentTransaction(getPaymentId(webhooksEvent.getRefund().getId()));
        updatePaymentTransaction(
                paymentTransaction,
                webhooksEvent.getRefund().getId(),
                webhooksEvent.getRefund().getStatus(),
                webhooksEvent.getRefund().getRefundOutput().getAmountOfMoney(),
                PaymentTransactionType.REFUND_FOLLOW_ON
        );

    }

    @Override
    public void savePaymentCost(AbstractOrderModel orderModel, AmountOfMoney surchargeAmount) {
        Double surcharge = worldlineAmountUtils.fromAmount(surchargeAmount.getAmount(), orderModel.getCurrency().getIsocode()).doubleValue();
        savePaymentCost(orderModel, surcharge);
    }

    @Override
    public void savePaymentCost(AbstractOrderModel orderModel, Double surcharge) {
        orderModel.setPaymentCost(surcharge);
        try {
            calculationService.calculateTotals(orderModel, true);
        } catch (CalculationException ex) {
            LOGGER.error("[ WORLDLINE ] Error was thrown while recalculating totals of cart/order.", ex);
        }
        modelService.refresh(orderModel);
    }

    private PaymentTransactionModel createPaymentTransaction(
            final AbstractOrderModel abstractOrderModel,
            final String merchantCode,
            final String pspReference) {
        validateParameterNotNullStandardMessage("order", abstractOrderModel);
        validateParameterNotNullStandardMessage("merchantCode", merchantCode);
        validateParameterNotNullStandardMessage("pspReference", pspReference);
        LOGGER.debug("[WORLDLINE] Create Payment Transaction for Order : {} ", abstractOrderModel.getCode());
        final PaymentTransactionModel paymentTransactionModel = modelService.create(PaymentTransactionModel.class);
        final String paymentId = getPaymentId(pspReference);
        paymentTransactionModel.setCode(paymentId);
        paymentTransactionModel.setRequestId(paymentId);
        paymentTransactionModel.setRequestToken(merchantCode);
        paymentTransactionModel.setPaymentProvider(WorldlinedirectcoreConstants.PAYMENT_PROVIDER);
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
            final AmountOfMoney amountOfMoney,
            final PaymentTransactionType transactionType) {
        validateParameterNotNullStandardMessage("order", abstractOrderModel);
        validateParameterNotNullStandardMessage("paymentTransaction", paymentTransaction);
        validateParameterNotNullStandardMessage("paymentTransactionId", paymentTransactionId);
        validateParameterNotNullStandardMessage("status", status);
        validateParameterNotNullStandardMessage("amountOfMoney", amountOfMoney);
        validateParameterNotNullStandardMessage("transactionType", transactionType);

        final PaymentTransactionEntryModel transactionEntryModel = modelService.create(PaymentTransactionEntryModel.class);
        LOGGER.debug("[WORLDLINE] Create Payment Transaction Entry for order {} (Transaction : {}, TransactionType : {}) ",
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

        final BigDecimal amount = worldlineAmountUtils.fromAmount(amountOfMoney.getAmount(), amountOfMoney.getCurrencyCode());
        transactionEntryModel.setAmount(amount);
        transactionEntryModel.setCurrency(abstractOrderModel.getCurrency());

        transactionEntryModel.setTransactionStatus(getTransactionStatus(status));
        transactionEntryModel.setTransactionStatusDetails(status);

        modelService.save(transactionEntryModel);
        modelService.refresh(paymentTransaction);
        setOrderPaymentStatus(paymentTransaction, transactionEntryModel);
        resumerProcess(abstractOrderModel,transactionType);
        return transactionEntryModel;
    }

    private void setOrderPaymentStatus(PaymentTransactionModel paymentTransactionModel, PaymentTransactionEntryModel transactionEntryModel) {
        final AbstractOrderModel order = paymentTransactionModel.getOrder();
        final String transactionStatus = transactionEntryModel.getTransactionStatus();
        switch (transactionEntryModel.getType()) {
            case AUTHORIZATION:
                if (WorldlinedirectcoreConstants.PAYMENT_STATUS_CATEGORY_ENUM.SUCCESSFUL.getValue().equals(transactionStatus)) {
                    order.setPaymentStatus(PaymentStatus.WORLDLINE_AUTHORIZED);
                } else if (WorldlinedirectcoreConstants.PAYMENT_STATUS_CATEGORY_ENUM.REJECTED.getValue().equals(transactionStatus)) {
                    order.setPaymentStatus(PaymentStatus.WORLDLINE_REJECTED);
                } else {
                    order.setPaymentStatus(PaymentStatus.WORLDLINE_WAITING_AUTH);
                }
                modelService.save(order);
                break;
            case CAPTURE:
                final String currencyIsoCode = order.getCurrency().getIsocode();
                BigDecimal remainingAmount = worldlineAmountUtils.fromAmount(order.getTotalPrice(), currencyIsoCode);
                for (PaymentTransactionEntryModel entry : getCapturePaymentTransactionEntries(paymentTransactionModel)) {
                    if (WorldlinedirectcoreConstants.PAYMENT_STATUS_CATEGORY_ENUM.REJECTED.getValue().equals(transactionStatus)) {
                        order.setPaymentStatus(PaymentStatus.WORLDLINE_REJECTED);
                        modelService.save(order);
                        return;
                    } else if (WorldlinedirectcoreConstants.PAYMENT_STATUS_CATEGORY_ENUM.SUCCESSFUL.getValue().equals(transactionStatus)) {
                        remainingAmount = remainingAmount.subtract(entry.getAmount());
                    }
                }

                final BigDecimal zero = worldlineAmountUtils.fromAmount(0.0d, currencyIsoCode);
                if (remainingAmount.compareTo(zero) <= 0) {
                    order.setPaymentStatus(PaymentStatus.WORLDLINE_CAPTURED);
                } else {
                    order.setPaymentStatus(PaymentStatus.WORLDLINE_WAITING_CAPTURE);
                }
                modelService.save(order);
                break;
        }
        modelService.refresh(order);
    }

    private List<PaymentTransactionEntryModel> getCapturePaymentTransactionEntries(final PaymentTransactionModel paymentTransactionModel) {
        return paymentTransactionModel.getEntries()
                .stream()
                .filter(entry -> PaymentTransactionType.CAPTURE.equals(entry.getType()))
                .collect(Collectors.toList());
    }

    private void resumerProcess(AbstractOrderModel order, PaymentTransactionType transactionType) {
        switch (transactionType){
            case AUTHORIZATION:
            case CAPTURE:
                worldlineBusinessProcessService.triggerOrderProcessEvent(order, WorldlinedirectcoreConstants.WORLDLINE_EVENT_PAYMENT);
                break;
            case REFUND_FOLLOW_ON:
                worldlineBusinessProcessService.triggerReturnProcessEvent((OrderModel) order, WorldlinedirectcoreConstants.WORLDLINE_EVENT_REFUND);
                break;
        }
    }



    private String getTransactionStatus(String status) {
        switch (WorldlinedirectcoreConstants.PAYMENT_STATUS_ENUM.valueOf(status)) {
            case CREATED:
            case REJECTED:
            case REJECTED_CAPTURE:
            case CANCELLED:
                return WorldlinedirectcoreConstants.PAYMENT_STATUS_CATEGORY_ENUM.REJECTED.getValue();
            case REDIRECTED:
            case REFUND_REQUESTED:
            case AUTHORIZATION_REQUESTED:
            case CAPTURE_REQUESTED:
                return WorldlinedirectcoreConstants.PAYMENT_STATUS_CATEGORY_ENUM.STATUS_UNKNOWN.getValue();
            case PENDING_PAYMENT:
            case PENDING_COMPLETION:
            case PENDING_CAPTURE:
            case CAPTURED:
            case REFUNDED:
                return WorldlinedirectcoreConstants.PAYMENT_STATUS_CATEGORY_ENUM.SUCCESSFUL.getValue();
            default:
                return "NOT_SUPPORTED";
        }
    }

    private String getPaymentId(String rawPaymentTransactionId) {
        String paymentTransactionId = StringUtils.split(rawPaymentTransactionId, "_")[0];
        if (paymentTransactionId.length() > PAYMENT_ID_LENGTH) {
            paymentTransactionId = paymentTransactionId.substring(PAYMENT_ID_START_STRIP_LENGTH, PAYMENT_ID_LENGTH + PAYMENT_ID_START_STRIP_LENGTH);
        }
        return paymentTransactionId;
    }


    public void setModelService(ModelService modelService) {
        this.modelService = modelService;
    }

    public void setWorldlineTransactionDao(WorldlineTransactionDao worldlineTransactionDao) {
        this.worldlineTransactionDao = worldlineTransactionDao;
    }

    public void setWorldlineBusinessProcessService(WorldlineBusinessProcessService worldlineBusinessProcessService) {
        this.worldlineBusinessProcessService = worldlineBusinessProcessService;
    }

    public void setWorldlineAmountUtils(WorldlineAmountUtils worldlineAmountUtils) {
        this.worldlineAmountUtils = worldlineAmountUtils;
    }

    public void setCalculationService(CalculationService calculationService) {
        this.calculationService = calculationService;
    }
}
