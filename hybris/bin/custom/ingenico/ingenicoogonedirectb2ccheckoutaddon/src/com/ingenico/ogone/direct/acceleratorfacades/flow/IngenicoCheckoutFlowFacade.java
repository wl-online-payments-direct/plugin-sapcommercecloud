package com.ingenico.ogone.direct.acceleratorfacades.flow;

import de.hybris.platform.acceleratorfacades.flow.CheckoutFlowFacade;
import de.hybris.platform.acceleratorfacades.order.AcceleratorCheckoutFacade;

public interface IngenicoCheckoutFlowFacade extends AcceleratorCheckoutFacade, CheckoutFlowFacade {

    boolean isNotCheckoutWithTokenization();
}
