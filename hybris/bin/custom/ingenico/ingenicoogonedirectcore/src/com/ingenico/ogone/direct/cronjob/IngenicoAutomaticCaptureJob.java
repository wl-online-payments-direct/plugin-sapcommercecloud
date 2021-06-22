package com.ingenico.ogone.direct.cronjob;

import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.INGENICO_EVENT_CAPTURE;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.cronjob.enums.CronJobResult;
import de.hybris.platform.cronjob.enums.CronJobStatus;
import de.hybris.platform.cronjob.model.CronJobModel;
import de.hybris.platform.payment.enums.PaymentTransactionType;
import de.hybris.platform.payment.model.PaymentTransactionModel;
import de.hybris.platform.servicelayer.cronjob.AbstractJobPerformable;
import de.hybris.platform.servicelayer.cronjob.PerformResult;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.store.BaseStoreModel;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingenico.direct.domain.CaptureResponse;
import com.ingenico.direct.domain.CapturesResponse;
import com.ingenico.ogone.direct.dao.IngenicoOrderDao;
import com.ingenico.ogone.direct.model.IngenicoConfigurationModel;
import com.ingenico.ogone.direct.service.IngenicoBusinessProcessService;
import com.ingenico.ogone.direct.service.IngenicoPaymentService;
import com.ingenico.ogone.direct.service.IngenicoTransactionService;
import com.ingenico.ogone.direct.util.IngenicoAmountUtils;

public class IngenicoAutomaticCaptureJob extends AbstractJobPerformable<CronJobModel> {
    private static final Logger LOGGER = LoggerFactory.getLogger(IngenicoAutomaticCaptureJob.class);

    private IngenicoOrderDao ingenicoOrderDao;
    private IngenicoPaymentService ingenicoPaymentService;
    private IngenicoTransactionService ingenicoTransactionService;
    private IngenicoBusinessProcessService ingenicoBusinessProcessService;
    private IngenicoAmountUtils ingenicoAmountUtils;

    @Override
    public PerformResult perform(final CronJobModel cronJob) {
        LOGGER.info("[INGENICO] Start processing..");

        final List<OrderModel> ordersToCapture = ingenicoOrderDao.findIngenicoOrdersToCapture();

        if (CollectionUtils.isEmpty(ordersToCapture)) {
            LOGGER.info("[INGENICO] 0 order to capture.");
            LOGGER.info("[INGENICO] End processing.");
            return new PerformResult(CronJobResult.SUCCESS, CronJobStatus.FINISHED);
        }

        int fail = 0;
        for (final OrderModel orderModel : ordersToCapture) {
            try {
                if (CollectionUtils.isEmpty(orderModel.getPaymentTransactions())) {
                    LOGGER.error("[INGENICO] Order {} has no PaymentTransaction", orderModel.getCode());
                    fail++;
                    continue;
                }

                final IngenicoConfigurationModel ingenicoConfiguration = getIngenicoConfiguration(orderModel);
                if (ingenicoConfiguration == null) {
                    LOGGER.error("[INGENICO] Order {} has no IngenicoConfiguration in the attached Store", orderModel.getCode());
                    fail++;
                    continue;
                }

                final int captureTimeFrame = ingenicoConfiguration.getCaptureTimeFrame() == null ? 0 : ingenicoConfiguration.getCaptureTimeFrame().intValue();
                final LocalDate actualTime = new Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                final LocalDate creationTime = orderModel.getCreationtime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                if (Period.between(creationTime, actualTime).getDays() < captureTimeFrame) {
                    LOGGER.info("[INGENICO] Order {} skipped", orderModel.getCode());
                    continue;
                }

                final PaymentTransactionModel lastPaymentTransaction = getLastPaymentTransaction(orderModel);
                final String paymentId = lastPaymentTransaction.getRequestId() + "_0";

                final CapturesResponse captures = ingenicoPaymentService.getCaptures(ingenicoConfiguration, paymentId);

                if (CollectionUtils.isNotEmpty(captures.getCaptures())) {
                    captures.getCaptures().forEach(capture -> ingenicoTransactionService.processCapture(capture));
                }

                final Long nonCapturedAmount = ingenicoPaymentService.getNonCapturedAmount(ingenicoConfiguration,
                        captures,
                        lastPaymentTransaction.getPlannedAmount(),
                        orderModel.getCurrency().getIsocode());
                if (nonCapturedAmount > 0) {

                    final BigDecimal amount = ingenicoAmountUtils.fromAmount(nonCapturedAmount, orderModel.getCurrency().getIsocode());
                    final CaptureResponse captureResponse = ingenicoPaymentService.capturePayment(ingenicoConfiguration,
                            paymentId,
                            amount,
                            orderModel.getCurrency().getIsocode());
                    ingenicoTransactionService.updatePaymentTransaction(lastPaymentTransaction,
                            captureResponse.getId(),
                            captureResponse.getStatus(),
                            captureResponse.getStatus(),
                            captureResponse.getCaptureOutput().getAmountOfMoney(),
                            PaymentTransactionType.CAPTURE);
                    LOGGER.info("[INGENICO] Order {}, remaining amount {} captured", orderModel.getCode(), amount);
                }

                ingenicoBusinessProcessService.triggerOrderProcessEvent(orderModel, INGENICO_EVENT_CAPTURE);

                LOGGER.info("[INGENICO] Order {} has been processed", orderModel.getCode());


            } catch (Exception e) {
                LOGGER.error("[INGENICO] Order {} ,unexpected error! {}", orderModel.getCode(), e);
                fail++;
            }
        }
        LOGGER.info("[INGENICO] End processing.");
        if (fail > 0) {
            LOGGER.info("[INGENICO] {} errors occurred!!", fail);
            return new PerformResult(CronJobResult.ERROR, CronJobStatus.FINISHED);
        }
        return new PerformResult(CronJobResult.SUCCESS, CronJobStatus.FINISHED);
    }

    private PaymentTransactionModel getLastPaymentTransaction(OrderModel orderModel) {
        return orderModel.getPaymentTransactions().get(orderModel.getPaymentTransactions().size() - 1);
    }

    private IngenicoConfigurationModel getIngenicoConfiguration(OrderModel orderModel) {
        final BaseStoreModel store = orderModel.getStore();
        return store != null ? store.getIngenicoConfiguration() : null;
    }

    public ModelService getModelService() {
        return modelService;
    }

    @Override
    public void setModelService(ModelService modelService) {
        this.modelService = modelService;
    }


    public void setIngenicoOrderDao(IngenicoOrderDao ingenicoOrderDao) {
        this.ingenicoOrderDao = ingenicoOrderDao;
    }

    public void setIngenicoPaymentService(IngenicoPaymentService ingenicoPaymentService) {
        this.ingenicoPaymentService = ingenicoPaymentService;
    }

    public void setIngenicoTransactionService(IngenicoTransactionService ingenicoTransactionService) {
        this.ingenicoTransactionService = ingenicoTransactionService;
    }

    public void setIngenicoBusinessProcessService(IngenicoBusinessProcessService ingenicoBusinessProcessService) {
        this.ingenicoBusinessProcessService = ingenicoBusinessProcessService;
    }

    public void setIngenicoAmountUtils(IngenicoAmountUtils ingenicoAmountUtils) {
        this.ingenicoAmountUtils = ingenicoAmountUtils;
    }
}
