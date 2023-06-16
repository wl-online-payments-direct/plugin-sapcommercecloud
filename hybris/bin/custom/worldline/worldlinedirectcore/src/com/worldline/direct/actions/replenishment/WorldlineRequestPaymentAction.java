package com.worldline.direct.actions.replenishment;

import com.onlinepayments.domain.AmountOfMoney;
import com.onlinepayments.domain.CalculateSurchargeResponse;
import com.onlinepayments.domain.CreatePaymentResponse;
import com.worldline.direct.enums.WorldlineRecurringPaymentStatus;
import com.worldline.direct.facade.WorldlineCheckoutFacade;
import com.worldline.direct.model.WorldlineConfigurationModel;
import com.worldline.direct.order.data.WorldlinePaymentInfoData;
import com.worldline.direct.service.WorldlinePaymentService;
import com.worldline.direct.service.WorldlineRecurringService;
import com.worldline.direct.service.WorldlineTransactionService;
import de.hybris.platform.b2bacceleratorservices.model.process.ReplenishmentProcessModel;
import de.hybris.platform.commerceservices.impersonation.ImpersonationContext;
import de.hybris.platform.commerceservices.impersonation.ImpersonationService;
import de.hybris.platform.commerceservices.order.CommerceCheckoutService;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.payment.WorldlinePaymentInfoModel;
import de.hybris.platform.processengine.action.AbstractAction;
import de.hybris.platform.processengine.model.BusinessProcessParameterModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;


/**
 * Action for authorizing payments.
 */
public class WorldlineRequestPaymentAction extends AbstractAction<ReplenishmentProcessModel> {
    private final static String ATTEMPTS = "attempts";
    private static final Logger LOG = LoggerFactory.getLogger(WorldlineRequestPaymentAction.class);
    private CommerceCheckoutService commerceCheckoutService;
    private ImpersonationService impersonationService;
    private WorldlineCheckoutFacade worldlineCheckoutFacade;
    private WorldlineRecurringService worldlineRecurringService;
    private WorldlinePaymentService worldlinePaymentService;

    protected WorldlineTransactionService worldlineTransactionService;


    @Override
    public String execute(final ReplenishmentProcessModel process) throws Exception {
        final BusinessProcessParameterModel orderModelParameter = processParameterHelper.getProcessParameterByName(process, "order");
        final OrderModel placedOrder = (OrderModel) orderModelParameter.getValue();
        getModelService().refresh(placedOrder);
        final ImpersonationContext context = new ImpersonationContext();
        context.setOrder(placedOrder);
        return getImpersonationService().executeInContext(context,
                (ImpersonationService.Executor<String, ImpersonationService.Nothing>) () -> {
                    if (process.getCartToOrderCronJob().getPaymentInfo() instanceof WorldlinePaymentInfoModel) {
                        Integer attemptSequence = getAttemptSequence(process, placedOrder.getStore().getWorldlineConfiguration());
                        if (processParameterHelper.getProcessParameterByName(process, ATTEMPTS) == null || attemptSequence > 0) {
                            try {
//                                if (placedOrder.getStore().getWorldlineConfiguration().isApplySurcharge()) {
//                                    CalculateSurchargeResponse surchargeResponse = worldlinePaymentService.calculateSurcharge(((WorldlinePaymentInfoModel)placedOrder.getPaymentInfo()).getHostedTokenizationId(), placedOrder);
//                                    AmountOfMoney surcharge = surchargeResponse.getSurcharges().get(0).getSurchargeAmount();
//                                    worldlineTransactionService.savePaymentCost(placedOrder, surcharge);
//                                }

                                Optional<CreatePaymentResponse> recurringPayment = worldlineRecurringService.createRecurringPayment(placedOrder);
                                if (recurringPayment.isPresent()) {
                                    worldlineCheckoutFacade.handlePaymentResponse(placedOrder, recurringPayment.get().getPayment());
                                    return Transition.OK.toString();

                                } else {
                                    LOG.error("something went wrong during payment creation");
                                    return Transition.NOK.toString();
                                }
                            } catch (Exception e) {
                                updateAttemptSequence(process, placedOrder.getStore().getWorldlineConfiguration());
                                LOG.error("something went wrong during payment creation", e);
                                return Transition.RETRY.toString();
                            }
                        } else {
                            worldlineRecurringService.blockRecurringPayment(placedOrder);
                            return Transition.NOK.toString();
                        }

                    } else {
                        return Transition.OK.toString();
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

    private Integer getAttemptSequence(ReplenishmentProcessModel process, WorldlineConfigurationModel configurationModel) {
        BusinessProcessParameterModel attempts = processParameterHelper.getProcessParameterByName(process, ATTEMPTS);
        if (attempts != null) {
            return (Integer) attempts.getValue();
        } else {
            return configurationModel.getReplenishmentAttempts()!= null ?configurationModel.getReplenishmentAttempts():5;
        }
    }

    private void updateAttemptSequence(ReplenishmentProcessModel process, WorldlineConfigurationModel configurationModel) {
        BusinessProcessParameterModel attempts = processParameterHelper.getProcessParameterByName(process, ATTEMPTS);

        if (attempts != null) {
            attempts.setValue((Integer) attempts.getValue() - 1);
            modelService.save(attempts);
        } else {
            processParameterHelper.setProcessParameter(process, ATTEMPTS,configurationModel.getReplenishmentAttempts()!= null ?configurationModel.getReplenishmentAttempts():5);
        }
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

    public void setWorldlinePaymentService(WorldlinePaymentService worldlinePaymentService) {
        this.worldlinePaymentService = worldlinePaymentService;
    }

    public void setWorldlineTransactionService(WorldlineTransactionService worldlineTransactionService) {
        this.worldlineTransactionService = worldlineTransactionService;
    }

    enum Transition {
        OK, NOK, RETRY;

        public static Set<String> getStringValues() {
            Set<String> res = new HashSet<>();
            for (final Transition transitions : Transition.values()) {
                res.add(transitions.toString());
            }
            return res;
        }
    }
}
