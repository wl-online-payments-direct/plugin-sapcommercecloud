package com.worldline.direct.actions.replenishment;

import de.hybris.platform.b2bacceleratorservices.model.process.ReplenishmentProcessModel;
import de.hybris.platform.core.enums.OrderStatus;
import de.hybris.platform.core.enums.PaymentStatus;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.processengine.action.AbstractAction;
import de.hybris.platform.processengine.model.BusinessProcessParameterModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;


/**
 * Action for authorizing payments.
 */
public class WorldlineCheckRecurringPaymentAction extends AbstractAction<ReplenishmentProcessModel> {
    private static final Logger LOG = LoggerFactory.getLogger(WorldlineCheckRecurringPaymentAction.class);

    @Override
    public String execute(final ReplenishmentProcessModel process) throws Exception {
        final BusinessProcessParameterModel orderModelParameter = processParameterHelper.getProcessParameterByName(process, "cart");
        final CartModel cartModel = (CartModel) orderModelParameter.getValue();
        getModelService().refresh(cartModel);
        if (PaymentStatus.WORLDLINE_AUTHORIZED.equals(cartModel.getPaymentStatus())
                || PaymentStatus.WORLDLINE_WAITING_CAPTURE.equals(cartModel.getPaymentStatus())) {
            LOG.debug("[WORLDLINE] Process: {} Order Waiting", process.getCode());
            return Transition.WAIT.toString();
        } else if (PaymentStatus.WORLDLINE_REJECTED.equals(cartModel.getPaymentStatus())) {
            LOG.debug("[WORLDLINE] Process: " + process.getCode() + " Order Not Captured");
            return Transition.NOK.toString();
        } else if (PaymentStatus.WORLDLINE_CAPTURED.equals(cartModel.getPaymentStatus())) {
            LOG.debug("[WORLDLINE] Process: {} Order Captured", process.getCode());
            cartModel.setStatus(OrderStatus.PAYMENT_CAPTURED);
            modelService.save(cartModel);
            return Transition.OK.toString();
        } else {
            LOG.debug("[WORLDLINE] Process: {} Order Waiting Auth", process.getCode());
            cartModel.setPaymentStatus(PaymentStatus.WORLDLINE_WAITING_AUTH);
            modelService.save(cartModel);
            return Transition.WAIT.toString();

        }

    }


    @Override
    public Set<String> getTransitions() {
        return Transition.getStringValues();
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
}
