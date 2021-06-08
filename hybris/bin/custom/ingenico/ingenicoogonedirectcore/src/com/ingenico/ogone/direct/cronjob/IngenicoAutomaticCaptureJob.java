package com.ingenico.ogone.direct.cronjob;

import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.INGENICO_EVENT_CAPTURE;

import java.util.List;

import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.cronjob.enums.CronJobResult;
import de.hybris.platform.cronjob.enums.CronJobStatus;
import de.hybris.platform.cronjob.model.CronJobModel;
import de.hybris.platform.payment.enums.PaymentTransactionType;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.payment.model.PaymentTransactionModel;
import de.hybris.platform.servicelayer.cronjob.AbstractJobPerformable;
import de.hybris.platform.servicelayer.cronjob.PerformResult;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.store.BaseStoreModel;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingenico.direct.domain.CaptureResponse;
import com.ingenico.ogone.direct.dao.IngenicoOrderDao;
import com.ingenico.ogone.direct.model.IngenicoConfigurationModel;
import com.ingenico.ogone.direct.service.IngenicoBusinessProcessService;
import com.ingenico.ogone.direct.service.IngenicoPaymentService;
import com.ingenico.ogone.direct.service.IngenicoTransactionService;

public class IngenicoAutomaticCaptureJob extends AbstractJobPerformable<CronJobModel> {
    private static final Logger LOGGER = LoggerFactory.getLogger(IngenicoAutomaticCaptureJob.class);

    private IngenicoOrderDao ingenicoOrderDao;
    private IngenicoPaymentService ingenicoPaymentService;
    private IngenicoTransactionService ingenicoTransactionService;
    private IngenicoBusinessProcessService ingenicoBusinessProcessService;

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
                PaymentTransactionEntryModel paymentTransactionEntry = getPaymentTransactionToCapture(orderModel);
                if (paymentTransactionEntry == null) {
                    LOGGER.error("[INGENICO] Order {} has no authorized PaymentTransaction", orderModel.getCode());
                    fail++;
                    continue;
                }
                IngenicoConfigurationModel ingenicoConfiguration = getIngenicoConfiguration(orderModel);

                CaptureResponse captureResponse = ingenicoPaymentService.capturePayment(ingenicoConfiguration, paymentTransactionEntry.getRequestId(), paymentTransactionEntry.getPaymentTransaction().getPlannedAmount(), paymentTransactionEntry.getCurrency().getIsocode());
                ingenicoTransactionService.updatePaymentTransaction(paymentTransactionEntry.getPaymentTransaction(),
                        paymentTransactionEntry.getRequestId(),
                        captureResponse.getStatus(),
                        captureResponse.getStatus(),
                        captureResponse.getCaptureOutput().getAmountOfMoney(),
                        PaymentTransactionType.CAPTURE);

                ingenicoBusinessProcessService.triggerOrderProcessEvent(orderModel, INGENICO_EVENT_CAPTURE);

                LOGGER.info("[INGENICO] Order {} has been processed", orderModel.getCode());


            } catch (Exception e) {
                LOGGER.error("[INGENICO] Order {} ,unexpected error! {}", orderModel.getCode(), e);
                fail++;
            }
        }
        LOGGER.info("[INGENICO] End processing.");
        if (fail > 0) {
            LOGGER.info("[INGENICO] {} error occured!!", fail);
            return new PerformResult(CronJobResult.ERROR, CronJobStatus.FINISHED);
        }
        return new PerformResult(CronJobResult.SUCCESS, CronJobStatus.FINISHED);
    }

    private IngenicoConfigurationModel getIngenicoConfiguration(OrderModel orderModel) {
        final BaseStoreModel store = orderModel.getStore();
        return store != null ? store.getIngenicoConfiguration() : null;
    }


    private PaymentTransactionEntryModel getPaymentTransactionToCapture(final OrderModel order) {
        final PaymentTransactionModel finalPaymentTransaction = order.getPaymentTransactions().get(order.getPaymentTransactions().size() - 1);
        return finalPaymentTransaction.getEntries()
                .stream()
                .filter(entry -> PaymentTransactionType.AUTHORIZATION.equals(entry.getType()))
                .findFirst().orElse(null);
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
}
