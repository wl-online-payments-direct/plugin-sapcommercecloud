package com.ingenico.ogone.direct.actions.order;

import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_STATUS_CATEGORY_ENUM.REJECTED;
import static com.ingenico.ogone.direct.constants.IngenicoogonedirectcoreConstants.PAYMENT_STATUS_CATEGORY_ENUM.STATUS_UNKNOWN;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import de.hybris.platform.core.enums.OrderStatus;
import de.hybris.platform.core.enums.PaymentStatus;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.payment.IngenicoPaymentInfoModel;
import de.hybris.platform.orderprocessing.model.OrderProcessModel;
import de.hybris.platform.payment.enums.PaymentTransactionType;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.payment.model.PaymentTransactionModel;
import de.hybris.platform.processengine.action.AbstractAction;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingenico.ogone.direct.util.IngenicoAmountUtils;


public class IngenicoCheckCapturedPaymentAction extends AbstractAction<OrderProcessModel> {
    private static final Logger LOG = LoggerFactory.getLogger(IngenicoCheckCapturedPaymentAction.class);

    private IngenicoAmountUtils ingenicoAmountUtils;

    @Override
    public String execute(final OrderProcessModel process) {
        LOG.debug("[INGENICO] Process: " + process.getCode() + " in step " + getClass().getSimpleName());

        final OrderModel order = process.getOrder();


        if (!(order.getPaymentInfo() instanceof IngenicoPaymentInfoModel)) {
            LOG.debug("[INGENICO] Skip capture check!");
            return Transition.OK.toString();
        }

        final List<PaymentTransactionEntryModel> transactionEntries = getCapturePaymentTransactionEntries(order);

        if (CollectionUtils.isEmpty(transactionEntries)) {
            LOG.debug("[INGENICO] Process: {} Order Waiting", process.getCode());
            order.setPaymentStatus(PaymentStatus.INGENICO_WAITING_CAPTURE);
            modelService.save(order);
            return Transition.WAIT.toString();
        }

        final String currencyIsoCode = order.getCurrency().getIsocode();
        BigDecimal remainingAmount = ingenicoAmountUtils.fromAmount(order.getTotalPrice(), currencyIsoCode);

        for (PaymentTransactionEntryModel entry : transactionEntries) {
            if (REJECTED.getValue().equals(entry.getTransactionStatus())) {
                LOG.debug("[INGENICO] Process: " + process.getCode() + " Order Not Captured");
                order.setPaymentStatus(PaymentStatus.INGENICO_REJECTED);
                modelService.save(order);
                return Transition.NOK.toString();
            }
            remainingAmount = remainingAmount.subtract(entry.getAmount());
        }
        LOG.debug("[INGENICO] Remaining amount: {}", remainingAmount);

        final BigDecimal zero = ingenicoAmountUtils.fromAmount(0.0d, currencyIsoCode);
        if (remainingAmount.compareTo(zero) <= 0) {
            LOG.debug("[INGENICO] Process: {} Order Captured", process.getCode());
            order.setStatus(OrderStatus.PAYMENT_CAPTURED);
            order.setPaymentStatus(PaymentStatus.INGENICO_CAPTURED);
            modelService.save(order);
            return Transition.OK.toString();
        }

        //Still remaining non captured amount
        LOG.debug("[INGENICO] Process: {} Order Waiting", process.getCode());
        order.setPaymentStatus(PaymentStatus.INGENICO_WAITING_CAPTURE);
        modelService.save(order);
        return Transition.WAIT.toString();
    }

    private List<PaymentTransactionEntryModel> getCapturePaymentTransactionEntries(final OrderModel order) {
        final PaymentTransactionModel finalPaymentTransaction = order.getPaymentTransactions().get(order.getPaymentTransactions().size() - 1);
        return finalPaymentTransaction.getEntries()
                .stream()
                .filter(entry -> PaymentTransactionType.CAPTURE.equals(entry.getType()))
                .collect(Collectors.toList());
    }

    public void setIngenicoAmountUtils(IngenicoAmountUtils ingenicoAmountUtils) {
        this.ingenicoAmountUtils = ingenicoAmountUtils;
    }


    enum Transition {
        OK, NOK, WAIT;

        public static Set<String> getStringValues() {
            Set<String> res = new HashSet<>();
            for (final Transition transitions : Transition.values()) {
                res.add(transitions.toString());
            }
            return res;
        }
    }

    @Override
    public Set<String> getTransitions() {
        return Transition.getStringValues();
    }
}