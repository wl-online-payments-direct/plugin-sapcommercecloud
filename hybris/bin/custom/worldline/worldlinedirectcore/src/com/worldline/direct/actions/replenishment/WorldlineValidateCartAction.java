package com.worldline.direct.actions.replenishment;

import de.hybris.platform.b2bacceleratorservices.model.process.ReplenishmentProcessModel;
import de.hybris.platform.commercefacades.order.CartFacade;
import de.hybris.platform.commercefacades.order.data.CartModificationData;
import de.hybris.platform.commerceservices.impersonation.ImpersonationContext;
import de.hybris.platform.commerceservices.impersonation.ImpersonationService;
import de.hybris.platform.commerceservices.order.CommerceCartModificationException;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.order.CartService;
import de.hybris.platform.processengine.action.AbstractAction;
import de.hybris.platform.processengine.model.BusinessProcessParameterModel;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WorldlineValidateCartAction extends AbstractAction<ReplenishmentProcessModel> {
    public static final String CART_MODIFICATIONS_PARAM="cartModifications";
    private final static Logger LOG = LoggerFactory.getLogger(WorldlineValidateCartAction.class);
    private CartFacade cartFacade;
    private CartService cartService;
    private ImpersonationService impersonationService;

    @Override
    public String execute(ReplenishmentProcessModel process) throws Exception {

        final BusinessProcessParameterModel clonedCartParameter = processParameterHelper.getProcessParameterByName(process, "cart");
        final CartModel cart = (CartModel) clonedCartParameter.getValue();
        this.modelService.refresh(cart);
        final ImpersonationContext context = new ImpersonationContext();
        context.setOrder(cart);
        context.setLanguage(cart.getUser().getSessionLanguage());
        return impersonationService.executeInContext(context,
                (ImpersonationService.Executor<String, ImpersonationService.Nothing>) () -> {
                    cartService.setSessionCart(cart);
                    try {
                        List<CartModificationData> cartModificationData = cartFacade.validateCartData();
                        if (CollectionUtils.isNotEmpty(cartModificationData)) {
                            getProcessParameterHelper().setProcessParameter(process, CART_MODIFICATIONS_PARAM, cartModificationData);
                            return Transition.NOK.toString();
                        } else {
                            return Transition.OK.toString();
                        }
                    } catch (CommerceCartModificationException e) {
                        LOG.error("Failed to validate cart", e);
                        return Transition.ERROR.toString();
                    }
                });
    }


    @Override
    public Set<String> getTransitions() {
        return Transition.getStringValues();
    }

    enum Transition {
        OK, NOK, ERROR;

        public static Set<String> getStringValues() {
            Set<String> res = new HashSet<>();
            for (final Transition transitions : Transition.values()) {
                res.add(transitions.toString());
            }
            return res;
        }
    }

    public void setCartFacade(CartFacade cartFacade) {
        this.cartFacade = cartFacade;
    }

    public void setCartService(CartService cartService) {
        this.cartService = cartService;
    }

    public void setImpersonationService(ImpersonationService impersonationService) {
        this.impersonationService = impersonationService;
    }
}
