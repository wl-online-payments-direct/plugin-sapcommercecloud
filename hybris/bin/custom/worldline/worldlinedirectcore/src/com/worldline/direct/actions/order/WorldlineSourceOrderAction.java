package com.worldline.direct.actions.order;

import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.orderprocessing.model.OrderProcessModel;
import de.hybris.platform.processengine.action.AbstractProceduralAction;
import de.hybris.platform.task.RetryLaterException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

/**
 * Custom SourceOrderAction wrapper with detailed debug logging to diagnose transaction rollback issues.
 * This delegates to the original sourceOrderAction but adds extensive logging.
 */
public class WorldlineSourceOrderAction extends AbstractProceduralAction<OrderProcessModel> {

    private static final Logger LOG = LoggerFactory.getLogger(WorldlineSourceOrderAction.class);

    private AbstractProceduralAction<OrderProcessModel> originalSourceOrderAction;

    @Override
    public void executeAction(final OrderProcessModel process) throws RetryLaterException, Exception {
        LOG.info("[WORLDLINE-DEBUG] ========== Starting WorldlineSourceOrderAction ==========");
        LOG.info("[WORLDLINE-DEBUG] Process code: {}", process.getCode());

        final OrderModel order = process.getOrder();
        if (order != null) {
            LOG.info("[WORLDLINE-DEBUG] Order code: {}", order.getCode());
            LOG.info("[WORLDLINE-DEBUG] Order status: {}", order.getStatus());
            LOG.info("[WORLDLINE-DEBUG] Order paymentStatus: {}", order.getPaymentStatus());
            LOG.info("[WORLDLINE-DEBUG] Order entries count: {}", order.getEntries() != null ? order.getEntries().size() : 0);
            LOG.info("[WORLDLINE-DEBUG] PaymentInfo type: {}", order.getPaymentInfo() != null ? order.getPaymentInfo().getClass().getSimpleName() : "null");
            LOG.info("[WORLDLINE-DEBUG] Store: {}", order.getStore() != null ? order.getStore().getUid() : "null");
            LOG.info("[WORLDLINE-DEBUG] DeliveryMode: {}", order.getDeliveryMode() != null ? order.getDeliveryMode().getCode() : "null");

            // Log B2B-specific info if available
            try {
                java.lang.reflect.Method getUnitMethod = order.getClass().getMethod("getUnit");
                Object unit = getUnitMethod.invoke(order);
                LOG.info("[WORLDLINE-DEBUG] B2B Unit: {}", unit != null ? unit.toString() : "null");
            } catch (NoSuchMethodException e) {
                LOG.info("[WORLDLINE-DEBUG] Not a B2B order (no unit)");
            } catch (Exception e) {
                LOG.info("[WORLDLINE-DEBUG] Could not get B2B unit: {}", e.getMessage());
            }

            // Log consignments before
            LOG.info("[WORLDLINE-DEBUG] Existing consignments before sourcing: {}",
                    order.getConsignments() != null ? order.getConsignments().size() : 0);
        } else {
            LOG.warn("[WORLDLINE-DEBUG] Order is NULL!");
        }

        // Check transaction status BEFORE
        LOG.info("[WORLDLINE-DEBUG] Transaction active BEFORE: {}", TransactionSynchronizationManager.isActualTransactionActive());
        boolean rollbackOnlyBefore = false;
        try {
            rollbackOnlyBefore = TransactionAspectSupport.currentTransactionStatus().isRollbackOnly();
            LOG.info("[WORLDLINE-DEBUG] Transaction rollback-only BEFORE: {}", rollbackOnlyBefore);
        } catch (Exception e) {
            LOG.info("[WORLDLINE-DEBUG] Could not check rollback status before: {}", e.getMessage());
        }

        try {
            LOG.info("[WORLDLINE-DEBUG] Delegating to original sourceOrderAction...");
            originalSourceOrderAction.executeAction(process);
            LOG.info("[WORLDLINE-DEBUG] Original sourceOrderAction completed successfully");

            // Log consignments after
            if (order != null) {
                // Refresh order to get latest data
                getModelService().refresh(order);
                LOG.info("[WORLDLINE-DEBUG] Consignments after sourcing: {}",
                        order.getConsignments() != null ? order.getConsignments().size() : 0);
                if (order.getConsignments() != null) {
                    order.getConsignments().forEach(c ->
                        LOG.info("[WORLDLINE-DEBUG] Consignment: code={}, status={}", c.getCode(), c.getStatus()));
                }
                LOG.info("[WORLDLINE-DEBUG] Order status after: {}", order.getStatus());
            }
        } catch (RetryLaterException e) {
            LOG.error("[WORLDLINE-DEBUG] RetryLaterException in sourceOrderAction: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            LOG.error("[WORLDLINE-DEBUG] Exception in sourceOrderAction: {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);

            // Log the full exception chain
            Throwable cause = e.getCause();
            int depth = 1;
            while (cause != null && depth < 10) {
                LOG.error("[WORLDLINE-DEBUG] Cause {}: {} - {}", depth, cause.getClass().getSimpleName(), cause.getMessage());
                cause = cause.getCause();
                depth++;
            }
            throw e;
        }

        // Check transaction status AFTER
        LOG.info("[WORLDLINE-DEBUG] Transaction active AFTER: {}", TransactionSynchronizationManager.isActualTransactionActive());
        try {
            boolean rollbackOnlyAfter = TransactionAspectSupport.currentTransactionStatus().isRollbackOnly();
            LOG.info("[WORLDLINE-DEBUG] Transaction rollback-only AFTER: {}", rollbackOnlyAfter);
            if (rollbackOnlyAfter && !rollbackOnlyBefore) {
                LOG.error("[WORLDLINE-DEBUG] !!! TRANSACTION WAS MARKED ROLLBACK-ONLY DURING ACTION EXECUTION !!!");
            }
        } catch (Exception e) {
            LOG.info("[WORLDLINE-DEBUG] Could not check rollback status after: {}", e.getMessage());
        }

        LOG.info("[WORLDLINE-DEBUG] ========== WorldlineSourceOrderAction completed ==========");
    }

    @Required
    public void setOriginalSourceOrderAction(final AbstractProceduralAction<OrderProcessModel> originalSourceOrderAction) {
        this.originalSourceOrderAction = originalSourceOrderAction;
    }
}
