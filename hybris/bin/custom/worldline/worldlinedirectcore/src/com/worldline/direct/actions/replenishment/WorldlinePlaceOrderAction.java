package com.worldline.direct.actions.replenishment;

import de.hybris.platform.b2b.model.B2BCustomerModel;
import de.hybris.platform.b2bacceleratorservices.model.process.ReplenishmentProcessModel;
import de.hybris.platform.commerceservices.impersonation.ImpersonationContext;
import de.hybris.platform.commerceservices.impersonation.ImpersonationService;
import de.hybris.platform.commerceservices.order.CommerceCheckoutService;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.processengine.action.AbstractProceduralAction;
import de.hybris.platform.processengine.model.BusinessProcessParameterModel;
import de.hybris.platform.tx.Transaction;
import org.springframework.beans.factory.annotation.Required;


/**
 * Action for placing orders.
 */
public class WorldlinePlaceOrderAction extends AbstractProceduralAction<ReplenishmentProcessModel> {
    private CommerceCheckoutService b2bCommerceCheckoutService;
    private CommerceCheckoutService b2cCommerceCheckoutService;
    private ImpersonationService impersonationService;


    @Override
    public void executeAction(final ReplenishmentProcessModel process) {
        final BusinessProcessParameterModel clonedCartParameter = processParameterHelper.getProcessParameterByName(process, "cart");
        final CartModel cart = (CartModel) clonedCartParameter.getValue();
        this.modelService.refresh(cart);


        final ImpersonationContext context = new ImpersonationContext();
        context.setOrder(cart);
        context.setLanguage(cart.getUser().getSessionLanguage());
        final OrderModel orderModel = getImpersonationService().executeInContext(context,
                (ImpersonationService.Executor<OrderModel, ImpersonationService.Nothing>) () -> {
                    final OrderModel orderModel1;
                    try {
                        Transaction.current().enableDelayedStore(false);
                        orderModel1 = getCommerceCheckoutService(cart).placeOrder(cart);
                    } catch (final InvalidCartException e) {
                        throw new IllegalStateException(e.getMessage(), e);
                    }
                    orderModel1.setSchedulingCronJob(process.getCartToOrderCronJob());
                    return orderModel1;
                });
        getModelService().save(orderModel);
        getProcessParameterHelper().setProcessParameter(process, "order", orderModel);
    }

    private CommerceCheckoutService getCommerceCheckoutService(CartModel cart) {
        if (isB2BContext(cart)) {
            return b2bCommerceCheckoutService;
        } else {
            return b2cCommerceCheckoutService;
        }
    }

    protected boolean isB2BContext(final AbstractOrderModel order) {
        if (order != null && order.getUser() != null) {
            return order.getUser() instanceof B2BCustomerModel;
        } else {
            return false;
        }
    }

    @Required
    public void setB2bCommerceCheckoutService(CommerceCheckoutService b2bCommerceCheckoutService) {
        this.b2bCommerceCheckoutService = b2bCommerceCheckoutService;
    }

    @Required
    public void setB2cCommerceCheckoutService(CommerceCheckoutService b2cCommerceCheckoutService) {
        this.b2cCommerceCheckoutService = b2cCommerceCheckoutService;
    }

    protected ImpersonationService getImpersonationService() {
        return impersonationService;
    }

    @Required
    public void setImpersonationService(final ImpersonationService impersonationService) {
        this.impersonationService = impersonationService;
    }
}
