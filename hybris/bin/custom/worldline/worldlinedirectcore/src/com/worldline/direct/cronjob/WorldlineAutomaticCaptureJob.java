package com.worldline.direct.cronjob;

import static com.worldline.direct.constants.WorldlinedirectcoreConstants.WORLDLINE_EVENT_PAYMENT;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import com.worldline.direct.dao.WorldlineOrderDao;
import com.worldline.direct.service.WorldlineBusinessProcessService;
import com.worldline.direct.service.WorldlinePaymentService;
import com.worldline.direct.service.WorldlineTransactionService;
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
import com.worldline.direct.model.WorldlineConfigurationModel;
import com.worldline.direct.util.WorldlineAmountUtils;

public class WorldlineAutomaticCaptureJob extends AbstractJobPerformable<CronJobModel> {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldlineAutomaticCaptureJob.class);

    private WorldlineOrderDao worldlineOrderDao;
    private WorldlinePaymentService worldlinePaymentService;
    private WorldlineTransactionService worldlineTransactionService;
    private WorldlineBusinessProcessService worldlineBusinessProcessService;
    private WorldlineAmountUtils worldlineAmountUtils;

    @Override
    public PerformResult perform(final CronJobModel cronJob) {
        LOGGER.info("[WORLDLINE] Start processing..");

        final List<OrderModel> ordersToCapture = worldlineOrderDao.findWorldlineOrdersToCapture();

        if (CollectionUtils.isEmpty(ordersToCapture)) {
            LOGGER.info("[WORLDLINE] 0 order to capture.");
            LOGGER.info("[WORLDLINE] End processing.");
            return new PerformResult(CronJobResult.SUCCESS, CronJobStatus.FINISHED);
        }

        int fail = 0;
        for (final OrderModel orderModel : ordersToCapture) {
            try {
                if (CollectionUtils.isEmpty(orderModel.getPaymentTransactions())) {
                    LOGGER.error("[WORLDLINE] Order {} has no PaymentTransaction", orderModel.getCode());
                    fail++;
                    continue;
                }

                final WorldlineConfigurationModel worldlineConfiguration = getWorldlineConfiguration(orderModel);
                if (worldlineConfiguration == null) {
                    LOGGER.error("[WORLDLINE] Order {} has no WorldlineConfiguration in the attached Store", orderModel.getCode());
                    fail++;
                    continue;
                }

                final int captureTimeFrame = worldlineConfiguration.getCaptureTimeFrame() == null ? 0 : worldlineConfiguration.getCaptureTimeFrame().intValue();
                final LocalDate actualTime = new Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                final LocalDate creationTime = orderModel.getCreationtime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                if (Period.between(creationTime, actualTime).getDays() < captureTimeFrame) {
                    LOGGER.info("[WORLDLINE] Order {} skipped", orderModel.getCode());
                    continue;
                }

                final PaymentTransactionModel lastPaymentTransaction = getLastPaymentTransaction(orderModel);
                final String paymentId = lastPaymentTransaction.getRequestId() + "_0";

                final CapturesResponse captures = worldlinePaymentService.getCaptures(worldlineConfiguration, paymentId);

                if (CollectionUtils.isNotEmpty(captures.getCaptures())) {
                    captures.getCaptures().forEach(capture -> worldlineTransactionService.processCapture(capture));
                }

                final Long nonCapturedAmount = worldlinePaymentService.getNonCapturedAmount(worldlineConfiguration,
                        paymentId,
                        captures,
                        lastPaymentTransaction.getPlannedAmount(),
                        orderModel.getCurrency().getIsocode());
                if (nonCapturedAmount > 0) {

                    final BigDecimal amount = worldlineAmountUtils.fromAmount(nonCapturedAmount, orderModel.getCurrency().getIsocode());
                    final CaptureResponse captureResponse = worldlinePaymentService.capturePayment(worldlineConfiguration,
                            paymentId,
                            amount,
                            orderModel.getCurrency().getIsocode(),
                            Boolean.TRUE);
                    worldlineTransactionService.updatePaymentTransaction(lastPaymentTransaction,
                            captureResponse.getId(),
                            captureResponse.getStatus(),
                            captureResponse.getCaptureOutput().getAmountOfMoney(),
                            PaymentTransactionType.CAPTURE);
                    LOGGER.info("[WORLDLINE] Order {}, remaining amount {} captured", orderModel.getCode(), amount);
                }

                worldlineBusinessProcessService.triggerOrderProcessEvent(orderModel, WORLDLINE_EVENT_PAYMENT);

                LOGGER.info("[WORLDLINE] Order {} has been processed", orderModel.getCode());


            } catch (Exception e) {
                LOGGER.error("[WORLDLINE] Order {} ,unexpected error! {}", orderModel.getCode(), e);
                fail++;
            }
        }
        LOGGER.info("[WORLDLINE] End processing.");
        if (fail > 0) {
            LOGGER.info("[WORLDLINE] {} errors occurred!!", fail);
            return new PerformResult(CronJobResult.ERROR, CronJobStatus.FINISHED);
        }
        return new PerformResult(CronJobResult.SUCCESS, CronJobStatus.FINISHED);
    }

    private PaymentTransactionModel getLastPaymentTransaction(OrderModel orderModel) {
        return orderModel.getPaymentTransactions().get(orderModel.getPaymentTransactions().size() - 1);
    }

    private WorldlineConfigurationModel getWorldlineConfiguration(OrderModel orderModel) {
        final BaseStoreModel store = orderModel.getStore();
        return store != null ? store.getWorldlineConfiguration() : null;
    }

    public ModelService getModelService() {
        return modelService;
    }

    @Override
    public void setModelService(ModelService modelService) {
        this.modelService = modelService;
    }


    public void setWorldlineOrderDao(WorldlineOrderDao worldlineOrderDao) {
        this.worldlineOrderDao = worldlineOrderDao;
    }

    public void setWorldlinePaymentService(WorldlinePaymentService worldlinePaymentService) {
        this.worldlinePaymentService = worldlinePaymentService;
    }

    public void setWorldlineTransactionService(WorldlineTransactionService worldlineTransactionService) {
        this.worldlineTransactionService = worldlineTransactionService;
    }

    public void setWorldlineBusinessProcessService(WorldlineBusinessProcessService worldlineBusinessProcessService) {
        this.worldlineBusinessProcessService = worldlineBusinessProcessService;
    }

    public void setWorldlineAmountUtils(WorldlineAmountUtils worldlineAmountUtils) {
        this.worldlineAmountUtils = worldlineAmountUtils;
    }
}
