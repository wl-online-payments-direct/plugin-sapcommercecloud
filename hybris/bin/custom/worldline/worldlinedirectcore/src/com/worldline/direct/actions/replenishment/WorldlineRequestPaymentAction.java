package com.worldline.direct.actions.replenishment;

import com.onlinepayments.domain.CreatePaymentResponse;
import com.onlinepayments.domain.GetMandateResponse;
import com.worldline.direct.constants.WorldlinedirectcoreConstants;
import com.worldline.direct.facade.WorldlineCheckoutFacade;
import com.worldline.direct.service.WorldlinePaymentService;
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
import java.util.Set;

import static com.worldline.direct.constants.WorldlinedirectcoreConstants.PAYMENT_METHOD_SEPA;


/**
 * Action for authorizing payments.
 */
public class WorldlineRequestPaymentAction extends AbstractAction<ReplenishmentProcessModel> {
    private static final Logger LOG = LoggerFactory.getLogger(WorldlineRequestPaymentAction.class);
    private CommerceCheckoutService commerceCheckoutService;
    private ImpersonationService impersonationService;
    private WorldlinePaymentService worldlinePaymentService;
    private WorldlineCheckoutFacade worldlineCheckoutFacade;

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
                        WorldlinePaymentInfoModel worldlinePaymentInfo = (WorldlinePaymentInfoModel) process.getCartToOrderCronJob().getPaymentInfo();

                        switch (worldlinePaymentInfo.getId()) {
                            case PAYMENT_METHOD_SEPA:
                                try {
                                    String mandate = worldlinePaymentInfo.getMandate();

                                    GetMandateResponse mandateResponse = worldlinePaymentService.getMandate(mandate);
                                    if (mandateResponse != null && WorldlinedirectcoreConstants.SEPA_MANDATE_STATUS.valueOf(mandateResponse.getMandate().getStatus()) == WorldlinedirectcoreConstants.SEPA_MANDATE_STATUS.ACTIVE) {
                                        CreatePaymentResponse createPaymentResponse = worldlinePaymentService.createPayment(placedOrder);
                                        worldlineCheckoutFacade.handlePaymentResponse(placedOrder, createPaymentResponse.getPayment());
                                        return Transition.OK.toString();
                                    } else {
                                        return Transition.NOK.toString();
                                    }

                                } catch (Exception e) {
                                    LOG.error("something went wrong during payment creation", e);
                                    return Transition.NOK.toString();
                                }
                            default:
                                break;
                        }
                    } else {
                        return Transition.SKIP.toString();
                    }
                    return Transition.OK.toString();
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

    public void setWorldlinePaymentService(WorldlinePaymentService worldlinePaymentService) {
        this.worldlinePaymentService = worldlinePaymentService;
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
