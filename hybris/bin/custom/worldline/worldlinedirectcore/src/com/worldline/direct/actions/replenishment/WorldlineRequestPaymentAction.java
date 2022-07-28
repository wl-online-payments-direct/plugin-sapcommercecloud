package com.worldline.direct.actions.replenishment;

import com.onlinepayments.domain.CreatePaymentResponse;
import com.worldline.direct.exception.WorldlineNonAuthorizedPaymentException;
import com.worldline.direct.facade.WorldlineCheckoutFacade;
import com.worldline.direct.service.WorldlineRecurringService;
import de.hybris.platform.b2bacceleratorservices.model.process.ReplenishmentProcessModel;
import de.hybris.platform.commerceservices.impersonation.ImpersonationContext;
import de.hybris.platform.commerceservices.impersonation.ImpersonationService;
import de.hybris.platform.commerceservices.order.CommerceCheckoutService;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.processengine.action.AbstractAction;
import de.hybris.platform.processengine.model.BusinessProcessParameterModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;


/**
 * Action for authorizing payments.
 */
public class WorldlineRequestPaymentAction extends AbstractAction<ReplenishmentProcessModel> {
    private static final Logger LOG = LoggerFactory.getLogger(WorldlineRequestPaymentAction.class);
    private CommerceCheckoutService commerceCheckoutService;
    private ImpersonationService impersonationService;
    private WorldlineCheckoutFacade worldlineCheckoutFacade;
    private WorldlineRecurringService worldlineRecurringService;

    @Override
    public String execute(final ReplenishmentProcessModel process) throws Exception {
        final BusinessProcessParameterModel orderModelParameter = processParameterHelper.getProcessParameterByName(process, "cart");
        final CartModel placedOrder = (CartModel) orderModelParameter.getValue();
        getModelService().refresh(placedOrder);
        final ImpersonationContext context = new ImpersonationContext();
        context.setOrder(placedOrder);
        return getImpersonationService().executeInContext(context,
                (ImpersonationService.Executor<String, ImpersonationService.Nothing>) () -> {
                    if (process.getCartToOrderCronJob().getPaymentInfo() instanceof WorldlinePaymentInfoModel) {
                        Optional<CreatePaymentResponse> recurringPayment = worldlineRecurringService.createRecurringPayment(placedOrder);
                        if (recurringPayment.isPresent()) {
                            try {
                                worldlineCheckoutFacade.handlePaymentResponse(placedOrder, recurringPayment.get().getPayment());
                                return Transition.OK.toString();
                            } catch (WorldlineNonAuthorizedPaymentException e) {
                                LOG.error("error during payment : " + e.getReason());
                                return Transition.NOK.toString();
                            } catch (Exception e) {
                                LOG.error("something went wrong during payment creation", e);
                                return Transition.NOK.toString();
                            }
                        } else {
                            LOG.error("something went wrong during payment creation");
                            return Transition.NOK.toString();
                        }
                    } else {
                        return Transition.SKIP.toString();
                    }
                });
    }

    protected CommerceCheckoutService getCommerceCheckoutService() {
        return commerceCheckoutService;
    }

    @Required
    public void setCommerceCheckoutService(final CommerceCheckoutService commerceCheckoutService) {
        this.commerceCheckoutService = commerceCheckoutService;
    }

    protected ImpersonationService getImpersonationService() {
        return impersonationService;
    }

    @Required
    public void setImpersonationService(final ImpersonationService impersonationService) {
        this.impersonationService = impersonationService;
    }

    public void setWorldlineCheckoutFacade(WorldlineCheckoutFacade worldlineCheckoutFacade) {
        this.worldlineCheckoutFacade = worldlineCheckoutFacade;
    }

    @Override
    public Set<String> getTransitions() {
        return Transition.getStringValues();
    }

    @Required
    public void setWorldlineRecurringService(WorldlineRecurringService worldlineRecurringService) {
        this.worldlineRecurringService = worldlineRecurringService;
    }

    enum Transition {
        OK, NOK, SKIP;

        public static Set<String> getStringValues() {
            Set<String> res = new HashSet<>();
            for (final Transition transitions : Transition.values()) {
                res.add(transitions.toString());
            }
            return res;
        }
    }
}
